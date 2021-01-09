package coms;

import battlecode.common.*;

public class EC extends Robot {

    public EC(RobotController rc) {
        super(rc);
    }

    public void takeTurn() throws GameActionException {
        super.takeTurn();
        RobotType toBuild = randomSpawnableRobotType();
        int influence = 50;
        for (Direction dir : directions) {
            if (rc.canBuildRobot(toBuild, dir, influence)) {
                rc.buildRobot(toBuild, dir, influence);
                RobotInfo[] rbs=rc.senseNearbyRobots(2,team);
                for (RobotInfo rb:rbs){
                    if(!eccoms.knownRobotId.contains(rb.ID)){
                        eccoms.appendNewUnit(rb.ID);
                    }
                }
            } else {
                break;
            }
        }
    }

    static RobotType randomSpawnableRobotType() {
        return spawnableRobot[(int) (Math.random() * spawnableRobot.length)];
    }
}
