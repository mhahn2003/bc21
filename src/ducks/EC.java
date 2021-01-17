package ducks;

import battlecode.common.*;
import ducks.utils.*;

import static ducks.RobotPlayer.turnCount;

public class EC extends Robot {
    int voteCount = -1;
    int bidCount = 1;
    boolean bidded = true;
    boolean initial = true;

    // unit count near EC
    int muckCount = 0;
    int polCount = 0;
    int fMuckCount = 0;
    int fPolCount = 0;

    // total unit count
    int tS = 0;
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
        Debug.p("tP: " + tP);
        Debug.p("tM: " + tM);
        // check what kind of units are outside our base
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
        // if muckraker nearby, signal for help
        if (muckHelp) Coms.signalQueue.add(Coms.getMessage(Coms.IC.MUCKRAKER_HELP, muckHelpLoc));
        // scenario 1: if enemy units nearby
        if (polCount > 0) {
            int random = (int) (Math.random() * 4);
            if (random < 3) {
                build(RobotType.MUCKRAKER, 1);
                tM--;
            }
            else build(RobotType.POLITICIAN, 30);
        }
        else if (muckCount > 0) build(RobotType.POLITICIAN, 25);
        // scenario 2: no enemy units nearby
        if (initial) {
            // at the very first just build a lot of slanderers
            // initially build in a 1:2:1 ratio of p, s, m
            // then build in a 1:1:1 ratio of p, s, m
            // then build in a 2:1:2 ratio of p, s, m
            if (rc.getRoundNum() <= 32) {
                if (2*tM < tS) build(RobotType.MUCKRAKER, 1);
                if (tS < 10) build(RobotType.SLANDERER, Constants.getBestSlanderer(Math.max(rc.getInfluence(), 21)));
                build(RobotType.POLITICIAN, 20);
            }
            if (rc.getRoundNum() <= 100) {
                if (2*tP < tS) {
                    // check available neutral ecs
                    int lowNeutral = 400;
                    for (int i = 0; i < 12; i++) {
                        if (neutralInf[i] != 0) {
                            lowNeutral = Math.min(lowNeutral, neutralInf[i]*70+80);
                        }
                    }
                    if (rc.getInfluence() >= lowNeutral) build(RobotType.POLITICIAN, lowNeutral);
                    else build(RobotType.POLITICIAN, 20);
                }
                else if (2*tM < tS) {
                    // testing
                    if (rc.getInfluence() >= 700) build(RobotType.MUCKRAKER, 500);
                    build(RobotType.MUCKRAKER, 1);
                }
                else build(RobotType.SLANDERER, Constants.getBestSlanderer(Math.max(rc.getInfluence(), 21)));
            }
            else if (rc.getRoundNum() <= 400) {
                if (tP < tS) {
                    int lowNeutral = 400;
                    for (int i = 0; i < 12; i++) {
                        if (neutralInf[i] != 0) {
                            lowNeutral = Math.min(lowNeutral, neutralInf[i]*70+80);
                        }
                    }
                    if (rc.getInfluence() >= lowNeutral) build(RobotType.POLITICIAN, lowNeutral);
                    build(RobotType.POLITICIAN, 25);
                }
                if (tM < tS) {
                    build(RobotType.MUCKRAKER, 1);
                }
                build(RobotType.SLANDERER, Constants.getBestSlanderer(Math.max(rc.getInfluence()-150, 21)));
            }
            else {
                if (tP < 2*tS) {
                    if (rc.getInfluence() >= 800) build(RobotType.POLITICIAN, Math.max(400, rc.getInfluence()/20));
                    build(RobotType.POLITICIAN, 30);
                }
                if (tM < 2*tS) {
                    build(RobotType.MUCKRAKER, 1);
                }
                build(RobotType.SLANDERER, Constants.getBestSlanderer(Math.max(rc.getInfluence()-250, 21)));
            }
        } else {
            // then build in a 1:1:1 ratio of p, s, m
            if (tP < tS) {
                int lowNeutral = 400;
                for (int i = 0; i < 12; i++) {
                    if (neutralInf[i] != 0) {
                        lowNeutral = Math.min(lowNeutral, neutralInf[i]*70+80);
                    }
                }
                if (rc.getInfluence() >= lowNeutral) build(RobotType.POLITICIAN, lowNeutral);
                build(RobotType.POLITICIAN, 25);
            }
            if (tM < tS) {
                build(RobotType.MUCKRAKER, 1);
            }
            build(RobotType.SLANDERER, Constants.getBestSlanderer(Math.max(rc.getInfluence()-100, 21)));
        }

//        if (muckCount > 0) {
//            if (fPolCount <= 3) {
//                build(RobotType.POLITICIAN, 15+muckCount);
//            }
//        }
//        if (polCount > 0) {
//            if (fMuckCount <= 16) {
//                build(RobotType.MUCKRAKER, 1);
//            }
//        }
//        if (polCount == 0 && muckCount == 0) {
//            if (turnCount >= 700) {
//                build(RobotType.SLANDERER, 150);
//                if (rc.getInfluence() > 600) {
//                    if (rc.canBid(rc.getInfluence()-600)) rc.bid(rc.getInfluence()-600);
//                }
//            } else {
//                if (rc.getInfluence() > 150) {
//                    build(RobotType.POLITICIAN, rc.getInfluence());
//                } else {
//                    int rand = (int) (Math.random() * 2);
//                    if (rand == 0) build(RobotType.POLITICIAN,  16);
//                    else build(RobotType.MUCKRAKER, 1);
//                }
//            }
//        }
//
//        if (turnCount < 250) {
//            if (rc.getInfluence() < 150) {
//                if (turnCount % 15 == 0) build(RobotType.SLANDERER, rc.getInfluence());
//                else build(RobotType.MUCKRAKER, 1);
//            } else {
//                build(RobotType.POLITICIAN, rc.getInfluence());
//            }
//        } else {
//            int rand = (int) (Math.random() * 4);
//            if (rand == 0) {
//                build(RobotType.POLITICIAN, 16);
//            }
//            else if (rand == 3) {
//                build(RobotType.MUCKRAKER, 1);
//            }
//            else {
//                if (rc.getInfluence() > 150) {
//                    build(RobotType.POLITICIAN, rc.getInfluence());
//                }
//            }
//        }
    }

    public void build(RobotType toBuild, int influence) throws GameActionException {
        int safetyNet = 0;
        if (muckCount > 0 && toBuild == RobotType.SLANDERER) return;
        if (polCount > 0) safetyNet = 100;
        if (toBuild == RobotType.MUCKRAKER) safetyNet = 0;
        if (influence + safetyNet > rc.getInfluence()) return;
        for (Direction dir : directions) {
            if (rc.canBuildRobot(toBuild, dir, influence)) {
                switch (toBuild) {
                    case POLITICIAN: tP++; break;
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
}
