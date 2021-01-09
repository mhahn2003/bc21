package coms;

import battlecode.common.*;

import java.util.HashMap;

public class Robot {
    static RobotController rc;
    static Nav nav;
    static Coms coms;

    static int minX = 9999;
    static int maxX = 30065;
    static int minY = 9999;
    static int maxY = 30065;
    static int[][] ends;
    static boolean[] edges = {false, false, false, false};
    static Team team;
    // ECIds may not necessarily correspond to EC MapLocations
    static int[] ECIds = new int[12];
    static MapLocation[] ECs = new MapLocation[12];
    static MapLocation[] neutralECs = new MapLocation[12];
    static MapLocation[] enemyECs = new MapLocation[12];
    static HashMap<Integer, MapLocation> ECLoc = new HashMap<>();
    // all robots in sensor radius
    static RobotInfo[] robots;

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
        if (r.getType() == RobotType.ENLIGHTENMENT_CENTER) coms = new ECComs(rc);
        else coms = new Coms(rc);
        team = rc.getTeam();
    }

    public void takeTurn() throws GameActionException {
        coms.getInfo();
        coms.collectInfo();
    }
}
