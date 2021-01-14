package ducks;

import battlecode.common.*;
import ducks.utils.*;

public class EC extends Robot {
    int voteCount = -1;
    int bidCount = 1;

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
    }

    public void takeTurn() throws GameActionException {
        super.takeTurn();
        if (rc.getRoundNum() >= 500) bid();
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
        if (muckCount > 0) {
            if (polCount > 0) {
                build(RobotType.POLITICIAN, 40);
            }
            if (muckHelp) {
                build(RobotType.POLITICIAN, 25);
            }
        }
        else if (polCount > 0) {
            build(RobotType.POLITICIAN, 30);
        }
        // scenario 2: no enemy units nearby
        // initially build in a 1:2:1 ratio of p, s, m
        // then build in a 2:2:1 ratio of p, s, m
        // then build in a 4:2:1 ratio of p, s, m
        if (rc.getRoundNum() <= 100) {
            if (tP < 2*tS) {
                if (rc.getInfluence() >= 600) build(RobotType.POLITICIAN, 400);
                build(RobotType.POLITICIAN, 25);
            }
            if (tM < 2*tS) {
                build(RobotType.MUCKRAKER, 1);
            }
            build(RobotType.SLANDERER, Constants.getBestSlanderer(Math.max(rc.getInfluence()-40, 21)));
        }
        else if (rc.getRoundNum() <= 400) {
            if (tP < tS) {
                if (rc.getInfluence() >= 800) build(RobotType.POLITICIAN, 400);
                build(RobotType.POLITICIAN, 40);
            }
            if (tM < 2*tS) {
                build(RobotType.MUCKRAKER, 1);
            }
            build(RobotType.SLANDERER, Constants.getBestSlanderer(Math.max(rc.getInfluence()-40, 21)));
        }
        else {
            if (2*tP < 2*tS) {
                if (rc.getInfluence() >= 800) build(RobotType.POLITICIAN, Math.max(400, rc.getInfluence()/20));
                build(RobotType.POLITICIAN, 80);
            }
            if (tM < 2*tS) {
                build(RobotType.MUCKRAKER, 1);
            }
            build(RobotType.SLANDERER, Constants.getBestSlanderer(Math.max(rc.getInfluence()-200, 21)));
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
                    case POLITICIAN: tP++;
                    case SLANDERER: tS++;
                    case MUCKRAKER: tM++;
                }
                rc.buildRobot(toBuild, dir, influence);
            }
        }
    }

    public void bid() throws GameActionException {
        int curVote = rc.getTeamVotes();
        if (curVote >= 751) return;
        if (curVote == voteCount) bidCount *= 2;
        if (bidCount + 500 <= rc.getInfluence() && rc.canBid(bidCount)) rc.bid(bidCount);
        bidCount -= bidCount/16;
        voteCount = curVote;
    }
}
