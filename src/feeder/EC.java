package feeder;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.RobotController;
import battlecode.common.RobotType;

public class EC extends Robot {

    public EC(RobotController rc) {
        super(rc);
    }

    public void takeTurn() throws GameActionException {
        super.takeTurn();
        nav.lookAround();
        RobotType toBuild = RobotType.SLANDERER;
        if (rc.getInfluence()>100) {
            for (Direction dir : directions) {
                if (rc.canBuildRobot(toBuild, dir, rc.getInfluence())) {
                    rc.buildRobot(toBuild, dir, rc.getInfluence());
                    break;
                }
            }
        }
    }
}
