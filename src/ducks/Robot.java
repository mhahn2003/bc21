package ducks;

import battlecode.common.*;
import ducks.utils.Debug;

public class Robot {
    static RobotController rc;
    static Nav nav;
    static Coms coms;
    static ECComs eccoms;

    // debug variable
    public static boolean debugOn = false;

    static int minX = 9999;
    static int maxX = 30065;
    static int minY = 9999;
    static int maxY = 30065;
    static int[][] ends;
    static Team team;
    static boolean[] edges = {false, false, false, false};
    static MapLocation wandLoc;
    static int offset = 0;


    // ECIds may not necessarily correspond to EC MapLocations
    // discuss: 3 by 12 array?
    static int[] ECIds = new int[12];
    static MapLocation[] friendECs = new MapLocation[12];
    static MapLocation[] neutralECs = new MapLocation[12];
    static MapLocation[] enemyECs = new MapLocation[12];

    // variables changed by coms
    static boolean moveAway = false;
    static MapLocation attacker = null;
    static int attackDist = 0;
    static boolean defendSlanderer = false;
    static MapLocation enemyMuck = null;
    static boolean runAway = false;
    static MapLocation danger = null;

    // all robots in sensor radius
    static RobotInfo[] robots;

    protected Team enemy;
    protected int actionRadius;

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
        if (rc.getType() == RobotType.ENLIGHTENMENT_CENTER){
            eccoms = new ECComs();
        }else{
            coms = new Coms();
            nav = new Nav();
        }
        team = rc.getTeam();
        enemy = rc.getTeam().opponent();
        actionRadius = rc.getType().actionRadiusSquared;
    }


    public void takeTurn() throws GameActionException {
        Coms.resetVariables();
        robots = rc.senseNearbyRobots();
        if (rc.getType() == RobotType.ENLIGHTENMENT_CENTER){
            eccoms.getInfo();
        } else {
            Debug.p("Before coms: " + Clock.getBytecodeNum());
            coms.getInfo();
            Debug.p("After getInfo: " + Clock.getBytecodeNum());
            coms.collectInfo();
            Debug.p("After collectInfo: " + Clock.getBytecodeNum());
            coms.displaySignal();
            Debug.p("After displaySignal: " + Clock.getBytecodeNum());
            if (moveAway) {
                // move away from the attacker if needed
                if (rc.getLocation().isWithinDistanceSquared(attacker, attackDist+4)) {
                    int furthestDist = rc.getLocation().distanceSquaredTo(attacker);
                    Direction optDir = null;
                    for (int i = 0; i < 8; i++) {
                        int dist = attacker.distanceSquaredTo(rc.getLocation().add(directions[i]));
                        if (dist > furthestDist && rc.canMove(directions[i])) {
                            furthestDist = dist;
                            optDir = directions[i];
                        }
                    }
                    if (optDir != null) rc.move(optDir);
                }
            }
        }
        Debug.p("\nmaxY:"+(edges[0]? maxY:0)+"\nmaxX:"+(edges[1]? maxX:0)+"\nminY:"+(edges[2]? minY:0)+"\nminX:"+(edges[3]? minX:0));
        Debug.p("Robot.takeTurn: " + Clock.getBytecodeNum());
//        rc.setIndicatorLine(rc.getLocation(),new MapLocation(maxX, maxY), 255, 255, 255);
//        rc.setIndicatorLine(rc.getLocation(),new MapLocation(minX, minY), 255, 255, 255);
    }


    // wander around
    // TODO: what if you're already at a corner/side and you want to explore more (+3 to the end to explore?)
    public static void wander() throws GameActionException {
        // first check if you're too close to anyone, and separate from each other
        boolean separate = false;
        MapLocation nearMuck = null;
        RobotInfo[] near = rc.senseNearbyRobots(8, team);
        for (RobotInfo r : near) {
            if (r.getType() == RobotType.MUCKRAKER) {
                separate = true;
                nearMuck = r.getLocation();
                break;
            }
        }
        if (separate) {
            // try to separate from the muckraker
            Direction opp = rc.getLocation().directionTo(nearMuck).opposite();
            nav.bugNavigate(rc.getLocation().add(opp));
        } else {
            wandLoc = new MapLocation(nav.getEnds()[(rc.getID() + offset) % 8][0], nav.getEnds()[(rc.getID() + offset) % 8][1]);
            if (rc.getLocation().isWithinDistanceSquared(wandLoc, 8)) {
                offset++;
                wander();
                return;
            }
            nav.bugNavigate(wandLoc);
        }
    }

    // patrol around center
    public static void patrol(MapLocation center, int minRadius, int maxRadius) throws GameActionException {
        boolean left = (rc.getID() % 2) == 0;
        Direction rotateDir = rc.getLocation().directionTo(center);
        int distHQ = rc.getLocation().distanceSquaredTo(center);
        if (distHQ < minRadius) {
            rotateDir = rotateDir.opposite();
        } else if (distHQ <= maxRadius) {
            if (left) {
                rotateDir = rotateDir.rotateLeft();
                rotateDir = rotateDir.rotateLeft();
            } else {
                rotateDir = rotateDir.rotateRight();
                rotateDir = rotateDir.rotateRight();
            }
        }
        for (int i = 0; i < 8; i++) {
            if (rc.canMove(rotateDir)) rc.move(rotateDir);
            else {
                if (left) rotateDir = rotateDir.rotateRight();
                else rotateDir = rotateDir.rotateLeft();
            }
        }
        if (rc.canMove(rotateDir)) rc.move(rotateDir);
    }
}
