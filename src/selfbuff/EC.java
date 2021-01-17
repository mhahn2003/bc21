package selfbuff;

import battlecode.common.*;

import static selfbuff.RobotPlayer.turnCount;

public class EC extends Robot {
    int muckCount = 0;
    int polCount = 0;
    int fMuckCount = 0;
    int fPolCount = 0;

    public EC(RobotController rc) {
        super(rc);
    }

    public void takeTurn() throws GameActionException {
        super.takeTurn();

        if (turnCount == 1) {
            // build a slanderer
            build(RobotType.SLANDERER, 150);
        }
        // check what kind of units are outside our base
        muckCount = 0;
        polCount = 0;
        fMuckCount = 0;
        fPolCount = 0;
        for (RobotInfo r : robots) {
            if (r.getTeam() == team.opponent()) {
                if (r.getType() == RobotType.POLITICIAN) polCount++;
                if (r.getType() == RobotType.MUCKRAKER) muckCount++;
            }
            if (r.getTeam() == team) {
                if (r.getType() == RobotType.POLITICIAN) fPolCount++;
                if (r.getType() == RobotType.MUCKRAKER) fMuckCount++;
            }
        }
        if (muckCount > 0) {
            if (fPolCount <= 3) {
                build(RobotType.POLITICIAN, 15+muckCount);
            }
        }
        if (polCount > 0) {
            if (fMuckCount <= 16) {
                build(RobotType.MUCKRAKER, 1);
            }
        }
        if (polCount == 0 && muckCount == 0) {
            if (turnCount >= 700) {
                build(RobotType.SLANDERER, 150);
                if (rc.getInfluence() > 600) {
                    if (rc.canBid(rc.getInfluence()-600)) rc.bid(rc.getInfluence()-600);
                }
            } else {
                if (rc.getInfluence() > 150) {
                    build(RobotType.POLITICIAN, rc.getInfluence());
                } else {
                    int rand = (int) (Math.random() * 2);
                    if (rand == 0) build(RobotType.POLITICIAN,  16);
                    else build(RobotType.MUCKRAKER, 1);
                }
            }
        }
        if (rc.getEmpowerFactor(team,10)>1.5){
            build(RobotType.POLITICIAN,rc.getInfluence());
        }
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
                System.out.println("I can build in " + dir);
                rc.buildRobot(toBuild, dir, influence);
            }
        }
    }

    public void build(RobotType toBuild, int influence, boolean onlyCardial) throws GameActionException {
        if (onlyCardial){
            int safetyNet = 0;
            if (muckCount > 0 && toBuild == RobotType.SLANDERER) return;
            if (polCount > 0) safetyNet = 100;
            if (toBuild == RobotType.MUCKRAKER) safetyNet = 0;
            if (influence + safetyNet > rc.getInfluence()) return;
            for (Direction dir : Direction.cardinalDirections()) {
                if (rc.canBuildRobot(toBuild, dir, influence)) {
                    System.out.println("I can build in " + dir);
                    rc.buildRobot(toBuild, dir, influence);
                }
            }
        }else{
            build( toBuild, influence);
        }
    }
}
