package coms;

import battlecode.common.*;

import static coms.RobotPlayer.turnCount;

public class EC extends Robot {

    public EC(RobotController rc) {
        super(rc);
    }

    public void takeTurn() throws GameActionException {
        super.takeTurn();

        if (turnCount == 1) {
            // build a slanderer
            build(RobotType.SLANDERER, 150);
        }
//        System.out.println("About to produce muck");
//        build(RobotType.MUCKRAKER, 1);
        if (turnCount < 250) {
            if (rc.getInfluence() < 150) {
                if (turnCount % 15 == 0) build(RobotType.SLANDERER, rc.getInfluence());
                else build(RobotType.MUCKRAKER, 1);
            } else {
                build(RobotType.POLITICIAN, rc.getInfluence());
            }
        } else {
            int rand = (int) (Math.random() * 4);
            if (rand == 0) {
                build(RobotType.POLITICIAN, 15);
            }
            else if (rand == 3) {
                build(RobotType.MUCKRAKER, 1);
            }
            else {
                if (rc.getInfluence() > 150) {
                    build(RobotType.POLITICIAN, rc.getInfluence());
                }
            }
        }
        RobotInfo[] rbs=rc.senseNearbyRobots(2,team);
        for (RobotInfo rb:rbs){
            if(!eccoms.knownRobotId.contains(rb.ID)){
                eccoms.appendNewUnit(rb.ID);
            }
        }
    }

    public void build(RobotType toBuild, int influence) throws GameActionException {
        for (Direction dir : directions) {
            if (rc.canBuildRobot(toBuild, dir, influence)) {
                System.out.println("I can build in " + dir);
                rc.buildRobot(toBuild, dir, influence);
            }
        }
    }
}
