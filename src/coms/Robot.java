package coms;

import battlecode.common.*;

import java.util.HashMap;
import java.util.HashSet;

public class Robot {
    static RobotController rc;
    static Nav nav;
    static Coms coms;
    static ECComs eccoms;

    static int minX = 9999;
    static int maxX = 30001;
    static int minY = 9999;
    static int maxY = 30001;
    static int[][] ends;
    static boolean[] edgesDetected = {false, false, false, false};
    static int[] edgesValue = {30001, 30001, 9999, 9999};

    static double[][] mapPassibility = new double[128][128];
    static int sqrtSensorRadius;

    static Team team;
    static HashSet<Integer> friendECs = new HashSet<>();
    static HashSet<Integer> neutralECs = new HashSet<>();
    static HashSet<Integer> enemyECs = new HashSet<>();
    static HashMap<Integer, MapLocation> ECLoc = new HashMap<>();

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
            eccoms.collectInfo();
        }else{
            coms.collectInfo();
            coms.displaySignal();
        }
        System.out.println("after taking super.turn" + Clock.getBytecodesLeft());

        MapLocation corner;
        if (edgesDetected[0] && edgesDetected[1]){
            corner = new MapLocation(edgesValue[1],edgesValue[0]);
            System.out.println("q1" + corner.toString());
            rc.setIndicatorLine(rc.getLocation(),corner,255,255,255);
        }
        if (edgesDetected[2] && edgesDetected[1]){
            corner = new MapLocation(edgesValue[1],edgesValue[2]);
            System.out.println("q2" + corner.toString());
            rc.setIndicatorLine(rc.getLocation(),corner,255,255,255);
        }
        if (edgesDetected[0] && edgesDetected[3]){
            corner = new MapLocation(edgesValue[3],edgesValue[0]);
            System.out.println("q3" + corner.toString());
            rc.setIndicatorLine(rc.getLocation(),corner,255,255,255);
        }
        if (edgesDetected[2] && edgesDetected[3]){
            corner = new MapLocation(edgesValue[3],edgesValue[2]);
            System.out.println("q4" + corner.toString());
            rc.setIndicatorLine(rc.getLocation(),corner,255,255,255);
        }
    }
}
