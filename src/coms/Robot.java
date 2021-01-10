package coms;

import battlecode.common.*;

import java.util.HashMap;

public class Robot {
    static RobotController rc;
    static Nav nav;
    static Coms coms;
    static ECComs eccoms;

    static int minX = 9999;
    static int maxX = 30065;
    static int minY = 9999;
    static int maxY = 30065;
    static int[][] ends;

    static Team team;

    static boolean[] edges = {false, false, false, false};

    static int sqrtSensorRadius;


    // ECIds may not necessarily correspond to EC MapLocations
    // discuss: 3 by 12 array?
    static int[] ECIds = new int[12];
    static MapLocation[] friendECs = new MapLocation[12];
    static MapLocation[] neutralECs = new MapLocation[12];
    static MapLocation[] enemyECs = new MapLocation[12];
    static HashMap<Integer, MapLocation> ECLoc = new HashMap<>();

    // all robots in sensor radius
    static RobotInfo[] robots;

    protected Team enemy;
    protected int actionRadius;

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

    static int getSqrtSensorRadius(RobotType rt){
        switch (rt){
            case ENLIGHTENMENT_CENTER:
            case            MUCKRAKER:
                return 6;
            case           POLITICIAN:
            case            SLANDERER:
                return 4;
        };
        return 0;
    };

    static int directionToInt(Direction dir) {
        switch(dir) {
            case NORTH    : return 0;
            case NORTHEAST: return 1;
            case EAST     : return 2;
            case SOUTHEAST: return 3;
            case SOUTH    : return 4;
            case SOUTHWEST: return 5;
            case WEST     : return 6;
            case NORTHWEST: return 7;
        }
        assert (false);
        return -1;
    }

    public Robot(RobotController r) {
        System.out.println("bytecode before initialization " + Clock.getBytecodesLeft());
        rc = r;
        if (rc.getType() == RobotType.ENLIGHTENMENT_CENTER){
            eccoms = new ECComs();
        }else{
            coms = new Coms();
            nav = new Nav();
        }
        team = rc.getTeam();
        enemy = rc.getTeam().opponent();
        actionRadius = rc.getType().actionRadiusSquared;
        sqrtSensorRadius = getSqrtSensorRadius(rc.getType());
        System.out.println("bytecode after initialization" + Clock.getBytecodesLeft());
    }


    public void takeTurn() throws GameActionException {

        System.out.println("before taking super.turn" + Clock.getBytecodesLeft());
        if (rc.getType() == RobotType.ENLIGHTENMENT_CENTER){
            eccoms.getInfo();
            eccoms.displaySignal();
        }else{
            coms.getInfo();
            coms.collectInfo();
            coms.displaySignal();
        }

        System.out.println("after taking super.turn" + Clock.getBytecodesLeft());
        System.out.println("\nmaxY:"+(edges[0]? maxY:0)+"\nmaxX:"+(edges[1]? maxX:0)+"\nminY:"+(edges[2]? minY:0)+"\nminX:"+(edges[3]? minX:0));
        rc.setIndicatorLine(rc.getLocation(),new MapLocation(maxX, maxY), 255, 255, 255);
        rc.setIndicatorLine(rc.getLocation(),new MapLocation(minX, minY), 255, 255, 255);
    }
}
