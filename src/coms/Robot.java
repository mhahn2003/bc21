package coms;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.RobotController;
import battlecode.common.RobotType;

public class Robot {
    static RobotController rc;
    static Nav nav;
    static Coms coms;

    static int minX = 9999;
    static int maxX = 30001;
    static int minY = 9999;
    static int maxY = 30001;
    static int[][] ends;
    static boolean[] edges = {false, false, false, false};
    static float[][] mapPassibility = new float[128][128];

    static final RobotType[] spawnableRobot = {
            RobotType.POLITICIAN,
            RobotType.SLANDERER,
            RobotType.MUCKRAKER,
    };

    static final Direction[] directions = {
            Direction.NORTH,
            Direction.NORTHEAST,
            Direction.EAST,
            Direction.SOUTHEAST,
            Direction.SOUTH,
            Direction.SOUTHWEST,
            Direction.WEST,
            Direction.NORTHWEST,
    };

    public Robot(RobotController r) {
        rc = r;
        nav = new Nav(rc);
        coms = new Coms(rc);
    }

    public void takeTurn() throws GameActionException {
        coms.collectInfo();
    }
}
