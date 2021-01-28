package finals;

import battlecode.common.*;
import finals.utils.Constants;
import finals.utils.Debug;

public class EC extends Robot {
    int voteCount = -1;
    int bidCount = 1;
    boolean bidded = true;
    boolean mid = false;
    boolean high = false;
    int income;

    // unit count near EC
    int muckCount = 0;
    int polCount = 0;
    int fMuckCount = 0;
    int fPolCount = 0;

    // total unit count
    int tS = 0;
    int tDP = 0;
    int tAP = 0;
    int tP = 0;
    int tM = 0;

    public EC(RobotController rc) {
        super(rc);
    }

    public void takeTurn() throws GameActionException {
        super.takeTurn();
        if (rc.getRoundNum() >= 500) bid();
        income = rc.getInfluence() - income;
        Debug.p("income: " + income);
        Debug.p("tS: " + tS);
        Debug.p("tDP: " + tDP);
        Debug.p("tAP: " + tAP);
        Debug.p("tM: " + tM);
        muckCount = 0;
        polCount = 0;
        fMuckCount = 0;
        fPolCount = 0;
        int maxConv = 0;
        boolean muckHelp = false;
        MapLocation muckHelpLoc = null;
        for (RobotInfo r : robots) {
            if (r.getTeam() == team.opponent()) {
                if (r.getType() == RobotType.POLITICIAN && r.getLocation().isWithinDistanceSquared(rc.getLocation(), 20)) {
                    polCount++;
                    if (r.getConviction() > maxConv) maxConv = r.getConviction();
                }
                if (r.getType() == RobotType.MUCKRAKER) {
                    if (r.getLocation().isWithinDistanceSquared(rc.getLocation(), 20)) {
                        muckHelp = true;
                        muckHelpLoc = r.getLocation();
                        muckCount++;
                    }
                }
            }
            if (r.getTeam() == team) {
                if (r.getType() == RobotType.POLITICIAN) fPolCount++;
                if (r.getType() == RobotType.MUCKRAKER) fMuckCount++;
            }
        }
        if (muckHelp) Coms.signalQueue.add(Coms.getMessage(Coms.IC.MUCKRAKER_HELP, muckHelpLoc));
        // check what kind of units are outside our base
        // if muckraker nearby, signal for help
        if (muckHelp) Coms.signalQueue.add(Coms.getMessage(Coms.IC.MUCKRAKER_HELP, muckHelpLoc));
        // scenario 1: if enemy units nearby
        if (!rc.isReady()) {
            income = rc.getInfluence();
            return;
        }
        double rand = Math.random();
        // TODO: fix our defense
        if (polCount > 0) {
            int random = (int) (rand * 4);
            if (random < 3) {
                build(RobotType.MUCKRAKER, 1);
                tM--;
            }
            else {
                build(RobotType.POLITICIAN, 30);
                tDP--;
            }
        }
        else if (muckCount > 0) {
            if (fPolCount < muckCount+1) build(RobotType.POLITICIAN, 25);
            else build(RobotType.MUCKRAKER, 1);
        }
        // scenario 2: no enemy units nearby
            // at the very first just build a lot of slanderers
            // build in a 1:1:eps ratio of p, s, m
        if (rc.getRoundNum() <= 50) {
            if (tM < 3*tS) build(RobotType.MUCKRAKER, 1);
            if (tP < tS) build(RobotType.POLITICIAN, 20);
            build(RobotType.SLANDERER, Constants.getBestSlanderer(rc.getInfluence()));
        }
        else if (rc.getRoundNum() <= 150) {
            if (tP < tS) {
                // check available neutral ecs
                int lowNeutral = 1000;
                int lowInd = -1;
                for (int i = 0; i < 12; i++) {
                    // debug
                    Debug.p("neutralInf: " + i + ": " + neutralInf[i]);
                    if (neutralInf[i] != -1 && neutralCooldown[i] <= 0) {
                        lowNeutral = Math.min(lowNeutral, neutralInf[i]*70+80);
                        lowInd = i;
                    }
                }
                Debug.p("lowNeutral: " + lowNeutral);
                if (lowInd == -1) lowNeutral = 400;
                else neutralCooldown[lowInd] = 40;
                if (rc.getInfluence() >= lowNeutral) {
                    build(RobotType.POLITICIAN, lowNeutral);
                }
                else {
                    if (lowInd != -1) {
                        neutralCooldown[lowInd] = 0;
                        if (rc.getInfluence() + income*5 > lowNeutral) build(RobotType.MUCKRAKER, 1);
                    }
                    build(RobotType.POLITICIAN, 20);
                }
            }
            else if (2*tM < tS) {
                build(RobotType.MUCKRAKER, 1);
            }
            else {
                if (rc.getInfluence() >= Math.min(income*6, 949)) build(RobotType.SLANDERER, Constants.getBestSlanderer(rc.getInfluence()));
                build(RobotType.MUCKRAKER, 1);
            }
        }
        else if (rc.getRoundNum() <= 350) {
            if (rc.getRoundNum() == 151) resetCount();
            // have sustainable eco
            if (income >= 160) {
                if (tP < 3*tM) {
                    int random = (int) (rand * 4);
                    int lowNeutral = 1000;
                    int lowInd = -1;
                    for (int i = 0; i < 12; i++) {
                        if (neutralInf[i] != -1 && neutralCooldown[i] <= 0) {
                            lowNeutral = Math.min(lowNeutral, neutralInf[i] * 70 + 80);
                            lowInd = i;
                        }
                    }
                    if (lowInd == -1) lowNeutral = 400;
                    else neutralCooldown[lowInd] = 40;
                    if (rc.getInfluence() >= lowNeutral && tAP < 2*tDP) {
                        if (lowInd == -1) {
                            if (random == 0) {
                                build(RobotType.MUCKRAKER, lowNeutral);
                                tM--;
                                tP++;
                            }
                        }
                        build(RobotType.POLITICIAN, lowNeutral);
                    } else {
                        if (lowInd != -1) {
                            neutralCooldown[lowInd] = 0;
                            if (rc.getInfluence() + income * 10 > lowNeutral) {
                                tM--;
                                tP++;
                                build(RobotType.MUCKRAKER, 1);
                            }
                        }
                        if (random == 0) {
                            build(RobotType.MUCKRAKER, 100);
                            tM--;
                            tP++;
                        }
                        build(RobotType.POLITICIAN, 20);
                    }
                }
                else {
                    if (rc.getInfluence() >= 1500) build(RobotType.MUCKRAKER, 1500);
                    else if (rc.getInfluence() >= 949) {
                        build(RobotType.SLANDERER, 949);
                        tM++;
                    }
                    else if (rc.getInfluence() + 4*income >= 949) {
                        build(RobotType.MUCKRAKER, 10);
                        tM--;
                    }
                    else build(RobotType.MUCKRAKER, 10);
                }
            } else {
                if (income < 100) {
                    int random = (int) (rand * 4);
                    // if poor, just maintain a 1:1:1 ratio
                    if (tP < tS) {
                        // check available neutral ecs
                        int lowNeutral = 1000;
                        int lowInd = -1;
                        for (int i = 0; i < 12; i++) {
                            if (neutralInf[i] != -1 && neutralCooldown[i] <= 0) {
                                lowNeutral = Math.min(lowNeutral, neutralInf[i]*70+80);
                                lowInd = i;
                            }
                        }
                        if (lowInd == -1) lowNeutral = 400;
                        else neutralCooldown[lowInd] = 40;
                        if (rc.getInfluence() >= lowNeutral) {
                            if (lowInd == -1) {
                                if (random == 0) {
                                    build(RobotType.MUCKRAKER, lowNeutral);
                                    tM--;
                                    tP++;
                                }
                            }
                            build(RobotType.POLITICIAN, lowNeutral);
                        }
                        else {
                            if (lowInd != -1) {
                                neutralCooldown[lowInd] = 0;
                                if (rc.getInfluence() + income * 5 > lowNeutral) build(RobotType.MUCKRAKER, 1);
                            }
                            build(RobotType.POLITICIAN, 20);
                        }
                    }
                    else if (tM < tS) {
                        build(RobotType.MUCKRAKER, 1);
                    }
                    else {
                        if (income <= 30 || rc.getInfluence() >= income*6) build(RobotType.SLANDERER, Constants.getBestSlanderer(rc.getInfluence()));
                        else build(RobotType.MUCKRAKER, 1);
                    }
                } else {
                    if (rc.getInfluence() >= 606) build(RobotType.SLANDERER, Constants.getBestSlanderer(rc.getInfluence()));
                    if (tP < tS) build(RobotType.POLITICIAN, 20);
                    build(RobotType.MUCKRAKER, 1);
                }
            }
        }
        else {
            if (rc.getRoundNum() == 351) resetCount();
            if (income >= 300) {
                if (tP < 4*tM) {
                    int random = (int) (rand * 4);
                    if (rc.getInfluence() >= 750 && tAP < 3*tDP) {
                        if (random == 0) {
                            build(RobotType.MUCKRAKER, 750);
                            tM--;
                            tP++;
                        }
                        build(RobotType.POLITICIAN, 750);
                    }
                    else build(RobotType.POLITICIAN, 30);
                }
                else {
                    if (rc.getInfluence() >= 2000) build(RobotType.MUCKRAKER, 1500);
                    else if (rc.getInfluence() >= 949) {
                        build(RobotType.MUCKRAKER, 949);
                        tM++;
                    }
                    else build(RobotType.MUCKRAKER, 1);
                }
            }
            else {
                if (income < 100) {
                    int random = (int) (rand * 4);
                    // if poor, just maintain a 1:1:1 ratio
                    if (tP < tS) {
                        // check available neutral ecs
                        int lowNeutral = 1000;
                        int lowInd = -1;
                        for (int i = 0; i < 12; i++) {
                            if (neutralInf[i] != -1 && neutralCooldown[i] <= 0) {
                                lowNeutral = Math.min(lowNeutral, neutralInf[i]*70+80);
                                lowInd = i;
                            }
                        }
                        if (lowInd == -1) lowNeutral = 400;
                        else neutralCooldown[lowInd] = 40;
                        if (rc.getInfluence() >= lowNeutral) {
                            if (lowInd == -1) {
                                if (random == 0) {
                                    build(RobotType.MUCKRAKER, lowNeutral);
                                    tM--;
                                    tP++;
                                }
                            }
                            build(RobotType.POLITICIAN, lowNeutral);
                        }
                        else {
                            if (lowInd != -1) {
                                neutralCooldown[lowInd] = 0;
                                if (rc.getInfluence() + income * 5 > lowNeutral) build(RobotType.MUCKRAKER, 1);
                            }
                            build(RobotType.POLITICIAN, 20);
                        }
                    }
                    else if (tM < tS) {
                        build(RobotType.MUCKRAKER, 1);
                    }
                    else {
                        if (income <= 30 || rc.getInfluence() >= income*6) build(RobotType.SLANDERER, Constants.getBestSlanderer(rc.getInfluence()));
                        else build(RobotType.MUCKRAKER, 1);
                    }
                } else if (income < 200) {
                    if (tP < 2*tS) {
                        int random = (int) (rand * 4);
                        int lowNeutral = 1000;
                        int lowInd = -1;
                        for (int i = 0; i < 12; i++) {
                            if (neutralInf[i] != -1 && neutralCooldown[i] <= 0) {
                                lowNeutral = Math.min(lowNeutral, neutralInf[i] * 70 + 80);
                                lowInd = i;
                            }
                        }
                        if (lowInd == -1) lowNeutral = 400;
                        else neutralCooldown[lowInd] = 40;
                        if (rc.getInfluence() >= lowNeutral && tAP < 2*tDP) {
                            if (lowInd == -1) {
                                if (random == 0) {
                                    build(RobotType.MUCKRAKER, lowNeutral);
                                    tM--;
                                    tP++;
                                }
                            }
                            build(RobotType.POLITICIAN, lowNeutral);
                        } else {
                            if (lowInd != -1) {
                                neutralCooldown[lowInd] = 0;
                                if (rc.getInfluence() + income * 10 > lowNeutral) {
                                    tM--;
                                    tP++;
                                    build(RobotType.MUCKRAKER, 1);
                                }
                            }
                            if (random == 0) {
                                build(RobotType.MUCKRAKER, 100);
                                tM--;
                                tP++;
                            }
                            build(RobotType.POLITICIAN, 20);
                        }
                    }
                    else if (tM < tS) {
                        build(RobotType.MUCKRAKER, 1);
                    }
                    else {
                        if (rc.getInfluence() >= 949) build(RobotType.SLANDERER, 949);
                        else {
                            build(RobotType.MUCKRAKER, 1);
                            tM--;
                        }
                    }
                } else {
                    if (rc.getInfluence() >= 949) build(RobotType.SLANDERER, Constants.getBestSlanderer(rc.getInfluence()));
                    if (tP < tS) build(RobotType.POLITICIAN, 20);
                    build(RobotType.MUCKRAKER, 1);
                }
            }
        }
        income = rc.getInfluence();
    }

    public void build(RobotType toBuild, int influence) throws GameActionException {
        build(toBuild, influence, false);
    }

    public void build(RobotType toBuild, int influence, boolean onlyCardinal) throws GameActionException {
        if (!rc.isReady()) return;
        int safetyNet = 0;
        if (muckCount > 0 && toBuild == RobotType.SLANDERER) return;
        if (polCount > 0) safetyNet = 100;
        if (influence == 1) safetyNet = 0;
        if (influence + safetyNet > rc.getInfluence()) return;
        for (Direction dir : onlyCardinal? Direction.cardinalDirections(): directions) {
            if (rc.canBuildRobot(toBuild, dir, influence)) {
                switch (toBuild) {
                    case POLITICIAN:
                        if (influence >= 50) tAP++;
                        else tDP++;
                        tP++;
                        break;
                    case SLANDERER: tS++; break;
                    case MUCKRAKER: tM++; break;
                }
                rc.buildRobot(toBuild, dir, influence);
                return;
            }
        }
    }

    public void bid() throws GameActionException {
        int curVote = rc.getTeamVotes();
        if (curVote >= 751) return;
        if (curVote == voteCount && bidded) bidCount *= 2;
        if (rc.canBid(bidCount)) {
            rc.bid(bidCount);
            bidded = true;
        } else bidded = false;
        bidCount -= bidCount/16;
        voteCount = curVote;
    }

    public void resetCount() {
        tS = 0;
        tDP = 0;
        tAP = 0;
        tP = 0;
        tM = 0;
    }
}
