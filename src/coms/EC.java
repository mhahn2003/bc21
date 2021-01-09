package coms;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.RobotController;
import battlecode.common.RobotType;

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
        if (turnCount < 60) {
            if (rc.getInfluence() < 150) {
                build(RobotType.MUCKRAKER, 1);
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
    }

    public void build(RobotType toBuild, int influence) throws GameActionException {
        for (Direction dir : directions) {
            if (rc.canBuildRobot(toBuild, dir, influence)) {
                rc.buildRobot(toBuild, dir, influence);
            } else {
                break;
            }
        }
    }
}
