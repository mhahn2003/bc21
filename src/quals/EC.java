package quals;

import battlecode.common.*;
import quals.utils.Constants;
import quals.utils.Debug;

import static ducks.RobotPlayer.turnCount;

public class EC extends Robot {
    int voteCount = -1;
    int bidCount = 1;
    boolean bidded = true;
    boolean initial;
    boolean mid = false;
    boolean high = false;

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
        if (Math.abs(rc.getRoundNum()-turnCount) <= 5) initial = true;
        else initial = false;
    }

    public void takeTurn() throws GameActionException {
        super.takeTurn();
        if (rc.getRoundNum() >= 500) bid();
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
                if (r.getType() == RobotType.POLITICIAN) {
                    polCount++;
                    if (r.getConviction() > maxConv) maxConv = r.getConviction();
                }
                if (r.getType() == RobotType.MUCKRAKER) {
                    if (r.getLocation().isWithinDistanceSquared(rc.getLocation(), 20)) {
                        muckHelp = true;
                        muckHelpLoc = r.getLocation();
                    }
                    muckCount++;
                }
            }
            if (r.getTeam() == team) {
                if (r.getType() == RobotType.POLITICIAN) fPolCount++;
                if (r.getType() == RobotType.MUCKRAKER) fMuckCount++;
            }
        }
        if (muckHelp) Coms.signalQueue.add(Coms.getMessage(Coms.IC.MUCKRAKER_HELP, muckHelpLoc));
        if (rc.getInfluence() >= 10000) {
            medium();
            return;
        }
        // check what kind of units are outside our base
        // if muckraker nearby, signal for help
        if (muckHelp) Coms.signalQueue.add(Coms.getMessage(Coms.IC.MUCKRAKER_HELP, muckHelpLoc));
        // scenario 1: if enemy units nearby
        double rand = Math.random();
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
            build(RobotType.POLITICIAN, 25);
            tDP--;
        }
        // if can self buff do self buff
        if (rc.getEmpowerFactor(team,12) > 4) {
            if (rc.getInfluence() <= 100) build(RobotType.POLITICIAN, 25, true);
            else build(RobotType.POLITICIAN,rc.getInfluence(),true);
        }
        // scenario 2: no enemy units nearby
        if (initial) {
            // at the very first just build a lot of slanderers
            // initially build in a 1:2:1 ratio of p, s, m
            // then build in a 1:1:1 ratio of p, s, m
            // then build in a 2:1:1 ratio of p, s, m
            if (rc.getRoundNum() <= 32) {
                if (2*tM < tS) build(RobotType.MUCKRAKER, 1);
                if (tS < 10) build(RobotType.SLANDERER, Constants.getBestSlanderer(Math.max(rc.getInfluence(), 21)));
                build(RobotType.POLITICIAN, 20);
            }
            if (rc.getRoundNum() <= 100) {
                if (2*tP < tS) {
                    // check available neutral ecs
                    int lowNeutral = 1000;
                    int lowInd = -1;
                    for (int i = 0; i < 12; i++) {
                        if (neutralInf[i] != 0 && neutralCooldown[i] <= 0) {
                            lowNeutral = Math.min(lowNeutral, neutralInf[i]*70+80);
                            lowInd = i;
                        }
                    }
                    if (lowInd == -1) lowNeutral = 400;
                    else neutralCooldown[lowInd] = 40;
                    if (rc.getInfluence() >= lowNeutral) {
                        build(RobotType.POLITICIAN, lowNeutral);
                    }
                    else {
                        if (lowInd != -1) neutralCooldown[lowInd] = 0;
                        build(RobotType.POLITICIAN, 20);
                    }
                }
                else if (2*tM < tS) {
                    build(RobotType.MUCKRAKER, 1);
                }
                else build(RobotType.SLANDERER, Constants.getBestSlanderer(Math.max(rc.getInfluence(), 21)));
            }
            else if (rc.getRoundNum() <= 400) {
                if (tP < tS) {
                    int lowNeutral = 1000;
                    int lowInd = -1;
                    for (int i = 0; i < 12; i++) {
                        if (neutralInf[i] != 0 && neutralCooldown[i] <= 0) {
                            lowNeutral = Math.min(lowNeutral, neutralInf[i]*70+80);
                            lowInd = i;
                        }
                    }
                    if (lowInd == -1) lowNeutral = 400;
                    else neutralCooldown[lowInd] = 40;
                    if (rc.getInfluence() >= lowNeutral && 2*tAP < tDP) {
                        int random = (int) (rand * 6);
                        if (random == 0) {
                            build(RobotType.MUCKRAKER, lowNeutral);
                            if (lowInd != -1) neutralCooldown[lowInd] = 0;
                        } else build(RobotType.POLITICIAN, lowNeutral);
                    }
                    else {
                        if (lowInd != -1) neutralCooldown[lowInd] = 0;
                        build(RobotType.POLITICIAN, 25);
                    }
                }
                if (tM < tS) {
                    build(RobotType.MUCKRAKER, 1);
                }
                build(RobotType.SLANDERER, Constants.getBestSlanderer(Math.max(rc.getInfluence()-150, 21)));
            }
            else {
                if (tP < 2*tS) {
                    int lowNeutral = 1000;
                    int lowInd = -1;
                    for (int i = 0; i < 12; i++) {
                        if (neutralInf[i] != 0 && neutralCooldown[i] <= 0) {
                            lowNeutral = Math.min(lowNeutral, neutralInf[i]*70+80);
                            lowInd = i;
                        }
                    }
                    if (lowInd == -1) lowNeutral = 400;
                    else neutralCooldown[lowInd] = 40;
                    if (rc.getInfluence() >= lowNeutral && tAP < tDP) {
                        int random = (int) (rand * 6);
                        if (random == 0) {
                            build(RobotType.MUCKRAKER, lowNeutral);
                            if (lowInd != -1) neutralCooldown[lowInd] = 0;
                        } else build(RobotType.POLITICIAN, lowNeutral);
                    }
                    else {
                        if (lowInd != -1) neutralCooldown[lowInd] = 0;
                        build(RobotType.POLITICIAN, 30);
                    }
                }
                if (tM < tS) {
                    build(RobotType.MUCKRAKER, 1);
                }
                build(RobotType.SLANDERER, Constants.getBestSlanderer(Math.max(rc.getInfluence()-250, 21)));
            }
        } else {
            // initially build in a 1:2:1 ratio of p, s, m
            // then build in a 1:1:1 ratio of p, s, m
            if (rc.getRoundNum() <= 150) {
                if (2*tP < tS) {
                    int lowNeutral = 1000;
                    int lowInd = -1;
                    for (int i = 0; i < 12; i++) {
                        if (neutralInf[i] != 0 && neutralCooldown[i] <= 0) {
                            lowNeutral = Math.min(lowNeutral, neutralInf[i]*70+80);
                            lowInd = i;
                        }
                    }
                    if (lowInd == -1) lowNeutral = 400;
                    else neutralCooldown[lowInd] = 40;
                    if (rc.getInfluence() >= lowNeutral) {
                        int random = (int) (rand * 6);
                        if (random == 0) {
                            build(RobotType.MUCKRAKER, lowNeutral);
                            if (lowInd != -1) neutralCooldown[lowInd] = 0;
                        } else build(RobotType.POLITICIAN, lowNeutral);
                    }
                    else {
                        if (lowInd != -1) neutralCooldown[lowInd] = 0;
                        build(RobotType.POLITICIAN, 25);
                    }
                }
                if (2*tM < tS) {
                    build(RobotType.MUCKRAKER, 1);
                }
                build(RobotType.SLANDERER, Constants.getBestSlanderer(Math.max(rc.getInfluence(), 21)));
            } else {
                if (tP < tS) {
                    int lowNeutral = 1000;
                    int lowInd = -1;
                    for (int i = 0; i < 12; i++) {
                        if (neutralInf[i] != 0 && neutralCooldown[i] <= 0) {
                            lowNeutral = Math.min(lowNeutral, neutralInf[i]*70+80);
                            lowInd = i;
                        }
                    }
                    if (lowInd == -1) lowNeutral = 400;
                    else neutralCooldown[lowInd] = 40;
                    if (rc.getInfluence() >= lowNeutral) {
                        int random = (int) (rand * 6);
                        if (random == 0) {
                            build(RobotType.MUCKRAKER, lowNeutral);
                            if (lowInd != -1) neutralCooldown[lowInd] = 0;
                        } else build(RobotType.POLITICIAN, lowNeutral);
                    }
                    else {
                        if (lowInd != -1) neutralCooldown[lowInd] = 0;
                        build(RobotType.POLITICIAN, 25);
                    }
                }
                if (tM < tS) {
                    build(RobotType.MUCKRAKER, 1);
                }
                build(RobotType.SLANDERER, Constants.getBestSlanderer(Math.max(rc.getInfluence() - 150, 21)));
            }
        }
    }

    public void build(RobotType toBuild, int influence) throws GameActionException {
        build(toBuild, influence, false);
    }

    public void build(RobotType toBuild, int influence, boolean onlyCardinal) throws GameActionException {
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

    // stuff to do if you're decently rich, aka over 10000 inf
    // need to focus on making more buff mucks to gain more buff to reach the 100 mil point
    public void medium() throws GameActionException {
        double rand = Math.random();
        if (!mid) {
            mid = true;
            // reset total count
            tS = 0;
            tP = 0;
            tAP = 0;
            tDP = 0;
            tM = 0;
        }
        if (polCount > 0) {
            int random = (int) (Math.random() * 4);
            if (random < 3) {
                build(RobotType.MUCKRAKER, rc.getInfluence()/200);
                tM--;
            }
            else {
                build(RobotType.POLITICIAN, 40);
                tDP--;
            }
        }
        else if (muckCount > 0) {
            build(RobotType.POLITICIAN, 40);
            tDP--;
        }
        // if can self buff do self buff
        if (rc.getEmpowerFactor(team,12) > 4) {
            if (rc.getInfluence() <= 100) build(RobotType.POLITICIAN, 40, true);
            else build(RobotType.POLITICIAN,rc.getInfluence(),true);
        }
        // build in a 3:1:2 ratio I think
        if (tP < 3*tS) {
            int lowNeutral = 1000;
            int lowInd = -1;
            for (int i = 0; i < 12; i++) {
                if (neutralInf[i] != 0 && neutralCooldown[i] <= 0) {
                    lowNeutral = Math.min(lowNeutral, neutralInf[i]*70+80);
                    lowInd = i;
                }
            }
            if (lowInd == -1) lowNeutral = Math.max(400, rc.getInfluence()/50);
            else neutralCooldown[lowInd] = 40;
            if (rc.getInfluence() >= lowNeutral && tAP < tDP) {
                int random = (int) (rand * 6);
                if (random == 0) {
                    build(RobotType.MUCKRAKER, lowNeutral);
                    if (lowInd != -1) neutralCooldown[lowInd] = 0;
                } else build(RobotType.POLITICIAN, lowNeutral);
            }
            else {
                if (lowInd != -1) neutralCooldown[lowInd] = 0;
                build(RobotType.POLITICIAN, 40);
            }
        }
        if (tM < 2*tS) {
            build(RobotType.MUCKRAKER, rc.getInfluence()/200);
        }
        build(RobotType.SLANDERER, Constants.getBestSlanderer(Math.max(rc.getInfluence(), 21)));
    }

    // stuff to do if you're rich, aka one ec has reached 100 mil
    public void rich() throws GameActionException {
        if (!high) {
            high = true;
            // reset total count
            tS = 0;
            tP = 0;
            tAP = 0;
            tDP = 0;
            tM = 0;
        }
        // no slanderers
        if (rc.getInfluence() >= 10000000) {
            if (tP < tM) {
                if (tAP < 2*tDP) {
                    build(RobotType.POLITICIAN, 5000);
                } else build(RobotType.POLITICIAN, 49);
                // TODO: up the influence of defense politician when you have lots of money
            } else build(RobotType.MUCKRAKER, 5000);
        } else {
            if (tP < tM) {
                build(RobotType.POLITICIAN, 49);
            } else build(RobotType.MUCKRAKER, 1);
        }
    }
}
