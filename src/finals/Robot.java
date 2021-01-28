package finals;

import battlecode.common.*;
import finals.utils.Debug;

import static finals.RobotPlayer.turnCount;

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
    static MapLocation[] ends;
    static Team team;
    static boolean[] edges = {false, false, false, false};
    static MapLocation wandLoc;


    // ECIds may not necessarily correspond to EC MapLocations
    // discuss: 3 by 12 array?
    static int[] ECIds = new int[12];
    static MapLocation[] friendECs = new MapLocation[12];
    static MapLocation[] neutralECs = new MapLocation[12];
    static int[] neutralInf = new int[12];
    static MapLocation[] enemyECs = new MapLocation[12];
    static int[] enemySurrounded = new int[12];
    static int[] neutralCooldown = new int[12];
    static MapLocation[] possibleECs = new MapLocation[36];
    // 1: vert, 2: horz, 3: diag, 0: found
    static int[] foundECs = new int[36];
    static int ECSize = 0;

    // variables changed by coms
    static boolean moveAway = false;
    static MapLocation attacker = null;
    static int attackDist = 0;
    static boolean defendSlanderer = false;
    static MapLocation enemyMuck = null;
    static boolean runAway = false;
    static MapLocation danger = null;
    static boolean mapGenerated = false;
    static MapLocation[][] mapSpots = new MapLocation[8][8];
    static boolean[][] visited = new boolean[8][8];
    static boolean updateNE = false;
    static boolean updateSE = false;
    static boolean updateSW = false;
    static boolean updateNW = false;
    static int mapType = -1;
    static MapLocation[] slandererLoc = new MapLocation[6];
    static int[] staleness = new int[6];
    static boolean explored = false;
    // the three symmetries
    static boolean vert = true;
    static boolean horz = true;
    static boolean diag = true;

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
        } else {
            coms = new Coms();
            nav = new Nav();
        }
        team = rc.getTeam();
        enemy = rc.getTeam().opponent();
        actionRadius = rc.getType().actionRadiusSquared;
        for (int i = 0; i < 12; i++) {
            neutralInf[i] = -1;
        }
    }


    public void takeTurn() throws GameActionException {
        Coms.resetVariables();
        robots = rc.senseNearbyRobots();
        if (rc.getType() == RobotType.ENLIGHTENMENT_CENTER){
            eccoms.getInfo();
        } else {
//            Debug.p("Before coms: " + Clock.getBytecodeNum());
            coms.getInfo();
//            Debug.p("After getInfo: " + Clock.getBytecodeNum());
            coms.collectInfo();
//            Debug.p("After collectInfo: " + Clock.getBytecodeNum());
            // TODO: fiddle around with this condition
            if (rc.getType() == RobotType.SLANDERER || rc.getRoundNum() < 200 || turnCount > 15) coms.displaySignal();
            else Coms.signalQueue.clear();
//            Debug.p("After displaySignal: " + Clock.getBytecodeNum());
            if (moveAway && rc.getType() != RobotType.POLITICIAN) {
                // move away from the attacker if needed
                if (rc.getLocation().isWithinDistanceSquared(attacker, attackDist+4)) {
                    boolean stay = false;
                    // check if right next to an ec
                    RobotInfo[] near = rc.senseNearbyRobots(2);
                    for (RobotInfo r : near) {
                        if (r.getType() == RobotType.ENLIGHTENMENT_CENTER && r.getTeam() == team.opponent()) stay = true;
                    }
                    int furthestDist = rc.getLocation().distanceSquaredTo(attacker);
                    Direction optDir = null;
                    for (int i = 0; i < 8; i++) {
                        int dist = attacker.distanceSquaredTo(rc.getLocation().add(directions[i]));
                        if (dist > furthestDist && rc.canMove(directions[i])) {
                            furthestDist = dist;
                            optDir = directions[i];
                        }
                    }
                    if (optDir != null && (!stay || rc.getLocation().isWithinDistanceSquared(attacker, attackDist))) rc.move(optDir);
                }
            }
        }
//        if (mapGenerated) {
//            // debug purposes
//            for (int i = 0; i < 8; i++) {
//                for (int j = 0; j < 8; j++) {
//                    if (visited[i][j]) rc.setIndicatorDot(mapSpots[i][j], 0, 0, 255);
//                    else rc.setIndicatorDot(mapSpots[i][j], 255, 0, 0);
//                }
//            }
//        }
        Debug.p("\nmaxY:"+(edges[0]? maxY:0)+"\nmaxX:"+(edges[1]? maxX:0)+"\nminY:"+(edges[2]? minY:0)+"\nminX:"+(edges[3]? minX:0));
//        Debug.p("Robot.takeTurn: " + Clock.getBytecodeNum());
        for (int i = 0; i < 6; i++) staleness[i]--;
        for (int i = 0; i < 12; i++) {
            neutralCooldown[i]--;
            enemySurrounded[i]--;
        }
    }


    // wander around
    // TODO: fix all the wander functions since we changed from void to MapLocation
    public static MapLocation wander() throws GameActionException {
        if (!explored) {
            // first need to find the map dimensions asap
            if (minX == 9999 || maxX == 30065 || minY == 9999 || maxY == 30065) {
                // go to the corner that is not discovered
                if (!mapGenerated) {
                    // do the thing where you separate from others
                    // go to the corners
                    Nav.getEnds();
                    wandLoc = ends[(rc.getID() % 4)];
                    return wandLoc;
                } else {
                    // one corner already found, go find the other!
                    if (rc.getID() % 3 == 0) {
                        // go find the other corner
                        Nav.getEnds();
                        int closestDist = 100000;
                        for (int i = 0; i < 4; i++) {
                            if ((ends[i].x == 9999 || ends[i].x == 30065) && (ends[i].y == 9999 || ends[i].y == 30065)) return ends[i];
                            if (ends[i].x == 9999 || ends[i].x == 30065 || ends[i].y == 9999 || ends[i].y == 30065) {
                                int dist = Math.abs(ends[i].x-rc.getLocation().x)+Math.abs(ends[i].y-rc.getLocation().y);
                                if (dist < closestDist) {
                                    closestDist = dist;
                                    wandLoc = ends[i];
                                }
                            }
                        }
                        return wandLoc;
                    } else {
                        // but some should just keep on exploring
                        return map();
                    }
                }
            } else {
                // third explore, filling out the map
                // other 2 thirds go to supposed ec locations and figure out if they're good or bad
                if (rc.getID() % 3 == 1) return map();
                else {
                    int closestPossDist = 1000000;
                    MapLocation closestPoss = null;
                    for (int i = 0; i < 36; i++) {
                        if (foundECs[i] != 0) {
                            int dist = rc.getLocation().distanceSquaredTo(possibleECs[i]);
                            if (dist < closestPossDist) {
                                closestPossDist = dist;
                                closestPoss = possibleECs[i];
                            }
                        }
                    }
                    if (closestPoss != null) return closestPoss;
                    else return map();
                }
            }
        }
        else {
            // mostly go towards supposed EC locations,
            // but a portion just do the same thing as politicians where you go away from other units
            if (rc.getID() % 5 == 0) {
                // do the jittery dance thingy
                return jitter();
            } else {
                // mostly go towards supposed EC locations
                int closestPossDist = 1000000;
                MapLocation closestPoss = null;
                for (int i = 0; i < 36; i++) {
                    if (foundECs[i] != 0) {
                        int dist = rc.getLocation().distanceSquaredTo(possibleECs[i]);
                        if (dist < closestPossDist) {
                            closestPossDist = dist;
                            closestPoss = possibleECs[i];
                        }
                    }
                }
                if (closestPoss != null) return closestPoss;
                else {
                    return jitter();
                }
            }
        }
//        if (!mapGenerated) {
//            // do the thing where you separate from others
//            // go to the corners
//            Nav.getEnds();
//            wandLoc = ends[(rc.getID() % 4)];
//            nav.bugNavigate(wandLoc);
//        } else {
//            int closestECDist = 100000;
//            MapLocation closestEC = null;
//            for (int i = 0; i < 12; i++) {
//                if (friendECs[i] != null) {
//                    int dist = rc.getLocation().distanceSquaredTo(friendECs[i]);
//                    if (dist < closestECDist) {
//                        closestECDist = dist;
//                        closestEC = friendECs[i];
//                    }
//                }
//            }
//            if (closestEC == null) {
//                Nav.getEnds();
//                wandLoc = ends[(rc.getID() % 4)];
//                return wandLoc;
//            }
//            RobotInfo[] nearMucks = new RobotInfo[3];
//            int nearMuckSize = 0;
//            if (robots.length <= 25) {
//                for (RobotInfo r : robots) {
//                    if (r.getTeam() == team && r.getType() == RobotType.MUCKRAKER &&
//                        r.getLocation().distanceSquaredTo(closestEC) > rc.getLocation().distanceSquaredTo(closestEC)) {
//                        nearMucks[nearMuckSize] = r;
//                        nearMuckSize++;
//                        if (nearMuckSize == 3) break;
//                    }
//                }
//            }
//            int closestOptDist = 100000;
//            MapLocation closestOpt = null;
//            for (int i = 7; i >= 0; i--) {
//                 for (int j = 7; j >= 0; j--) {
//                    int dist = rc.getLocation().distanceSquaredTo(mapSpots[i][j]);
//                    int h = 0;
//                    for (int k = 0; k < nearMuckSize; k++) {
//                        int mDist = Math.max(nearMucks[k].getLocation().distanceSquaredTo(mapSpots[i][j]), 1);
//                        if (mDist < dist) h += 1000;
//                        h += 500/mDist;
//                    }
//                    if (dist <= 4) h = 0;
//                    h -= 300/Math.max(dist, 1);
//                    if (h < closestOptDist && !visited[i][j]) {
//                        closestOptDist = h;
//                        closestOpt = mapSpots[i][j];
//                    }
//                 }
//            }
//            if (closestOpt == null) {
//                Nav.getEnds();
//                wandLoc = ends[(rc.getID() % 4)];
//            } else wandLoc = closestOpt;
//        }
//        Debug.p("going to: " + wandLoc);
//        return wandLoc;
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

    public static MapLocation jitter() {
        int unitInd = 0;
        MapLocation[] nearUnits = new MapLocation[5];
        RobotInfo[] nearU = rc.senseNearbyRobots(16, team);
        for (RobotInfo r : nearU) {
            if (unitInd < 5) {
                nearUnits[unitInd] = r.getLocation();
                unitInd++;
            }
            if (unitInd == 5) break;
        }
        int maxH = 0;
        Direction optDir = null;
        for (int i = 0; i < 8; i++) {
            MapLocation loc = rc.getLocation().add(directions[i]);
            int h = 0;
            for (int j = 0; j < unitInd; j++) {
                Debug.p("near poli: " + j + " " + nearUnits[j]);
                h += loc.distanceSquaredTo(nearUnits[j]);
            }
            if (h > maxH && rc.canMove(directions[i])) {
                maxH = h;
                optDir = directions[i];
            }
        }
        if (unitInd == 0 || optDir == null) {
            // go to corner?
            Nav.getEnds();
            wandLoc = ends[(rc.getID() % 4)];
            return wandLoc;
        } else return rc.getLocation().add(optDir);
    }

    public static MapLocation map() {
        int closestECDist = 100000;
        MapLocation closestEC = null;
        for (int i = 0; i < 12; i++) {
            if (friendECs[i] != null) {
                int dist = rc.getLocation().distanceSquaredTo(friendECs[i]);
                if (dist < closestECDist) {
                    closestECDist = dist;
                    closestEC = friendECs[i];
                }
            }
        }
        if (closestEC == null) {
            Nav.getEnds();
            wandLoc = ends[(rc.getID() % 4)];
            return wandLoc;
        }
        RobotInfo[] nearMucks = new RobotInfo[3];
        int nearMuckSize = 0;
        if (robots.length <= 25) {
            for (RobotInfo r : robots) {
                if (r.getTeam() == team && r.getType() == RobotType.MUCKRAKER &&
                        r.getLocation().distanceSquaredTo(closestEC) > rc.getLocation().distanceSquaredTo(closestEC)) {
                    nearMucks[nearMuckSize] = r;
                    nearMuckSize++;
                    if (nearMuckSize == 3) break;
                }
            }
        }
        int closestOptDist = 100000;
        MapLocation closestOpt = null;
        for (int i = 7; i >= 0; i--) {
            for (int j = 7; j >= 0; j--) {
                int dist = rc.getLocation().distanceSquaredTo(mapSpots[i][j]);
                int h = 0;
                for (int k = 0; k < nearMuckSize; k++) {
                    int mDist = Math.max(nearMucks[k].getLocation().distanceSquaredTo(mapSpots[i][j]), 1);
                    if (mDist < dist) h += 1000;
                    h += 500/mDist;
                }
                if (dist <= 4) h = 0;
                h -= 300/Math.max(dist, 1);
                if (h < closestOptDist && !visited[i][j]) {
                    closestOptDist = h;
                    closestOpt = mapSpots[i][j];
                }
            }
        }
        if (closestOpt == null) {
            Nav.getEnds();
            wandLoc = ends[(rc.getID() % 4)];
        } else wandLoc = closestOpt;
        return wandLoc;
    }
}
