package ducks;

import battlecode.common.*;
import ducks.utils.Debug;

import java.util.PriorityQueue;

import static ducks.Robot.*;

public class Coms {
    protected final PriorityQueue<Integer> signalQueue = new PriorityQueue<>();
    protected int relevantSize = 0;
    protected int relevantInd = 0;
    protected int[] relevantFlags = new int[20];
    protected RobotInfo processingRobot = null;


    // number of possible cases for InfoCategory enum class
    private static int numCase = 9;

    public Coms() {
    }

    // TODO: need to order in terms of priority
    public enum IC {
        MUCKRAKER_HELP,
        FRIEND_EC,
        EC_ID,
        ENEMY_EC,
        NEUTRAL_EC,
        MUCKRAKER,
        SLANDERER,
        EDGE_N,
        EDGE_E,
        EDGE_S,
        EDGE_W,
        ATTACK,
    }

    public static int getMessage(IC cat, MapLocation coord) {
        int message;
        switch (cat) {
            case MUCKRAKER_HELP: message = 1; break;
            case FRIEND_EC     : message = 2; break;
            case EC_ID         : message = 3; break;
            case ENEMY_EC      : message = 4; break;
            case NEUTRAL_EC    : message = 5; break;
            case MUCKRAKER     : message = 6; break;
            case SLANDERER     : message = 7; break;
            case EDGE_N        : message = 8; break;
            case EDGE_E        : message = 9; break;
            case EDGE_S        : message = 10; break;
            case EDGE_W        : message = 11; break;
            case ATTACK        : message = 12; break;
            default            : message = 13;
        }
        message = addCoord(message, coord) + typeInt(rc.getType());
        return message;
    }

    public static int getMessage(IC cat, int ID) {
        int message;
        switch (cat) {
            case MUCKRAKER_HELP: message = 1; break;
            case FRIEND_EC     : message = 2; break;
            case EC_ID         : message = 3; break;
            case ENEMY_EC      : message = 4; break;
            case NEUTRAL_EC    : message = 5; break;
            case MUCKRAKER     : message = 6; break;
            case SLANDERER     : message = 7; break;
            case EDGE_N        : message = 8; break;
            case EDGE_E        : message = 9; break;
            case EDGE_S        : message = 10; break;
            case EDGE_W        : message = 11; break;
            case ATTACK        : message = 12; break;
            default            : message = 13;
        }
        message = addID(message, ID) + typeInt(rc.getType());
        return message;
    }

    public static int typeInt(RobotType type) {
        switch (type) {
            case POLITICIAN: return 1000000;
            case SLANDERER: return 2000000;
            case MUCKRAKER: return 3000000;
            case ENLIGHTENMENT_CENTER: return 4000000;
        }
        return 0;
    }
    public static int addCoord(int message, MapLocation coord) {
        return (message << 15) + ((coord.x % 128) << 7) + (coord.y % 128);
    }

    public static int addID(int message, int ID) {
        return (message << 15)+ID;
    }

    public static RobotType getTyp(int message) {
        switch (message/1000000) {
            case 1: return RobotType.POLITICIAN;
            case 2: return RobotType.SLANDERER;
            case 3: return RobotType.MUCKRAKER;
            case 4: return RobotType.ENLIGHTENMENT_CENTER;
        }
        return null;
    }


    public static IC getCat(int message) {
        message = message % 1000000;
        switch (message >> 15) {
            case 1: return IC.MUCKRAKER_HELP;
            case 2: return IC.FRIEND_EC;
            case 3: return IC.EC_ID;
            case 4: return IC.ENEMY_EC;
            case 5: return IC.NEUTRAL_EC;
            case 6: return IC.MUCKRAKER;
            case 7: return IC.SLANDERER;
            case 8: return IC.EDGE_N;
            case 9: return IC.EDGE_E;
            case 10: return IC.EDGE_S;
            case 11: return IC.EDGE_W;
            case 12: return IC.ATTACK;
            default: return null;
        }
    }

    public static int getID(int message) {
        message = message % 1000000;
        return message % 32768;
    }

    public static MapLocation getCoord(int message) {
        message = message % 1000000;
        MapLocation here = rc.getLocation();
        int remX = here.x % 128;
        int remY = here.y % 128;
        message = message % 32768;
        int x = message >> 7;
        int y = message % 128;
        if (Math.abs(x - remX) >= 64) {
            if (x > remX) x = here.x - remX - 128 + x;
            else x = here.x - remX + x + 128;
        } else x = here.x - remX + x;
        if (Math.abs(y - remY) >= 64) {
            if (y > remY) y = here.y - remY - 128 + y;
            else y = here.y + y + 128 - remY;
        } else y = here.y - remY + y;
        return new MapLocation(x, y);
    }

    // relay information about surroundings
    public void collectInfo() throws GameActionException {
        // first check for any edges
        for (int i = 0; i < 4; i++) {
            if (edges[i]) continue;
            Direction dir = Direction.cardinalDirections()[i];
            MapLocation checkLoc = rc.getLocation().add(dir);
            while (checkLoc.isWithinDistanceSquared(rc.getLocation(), rc.getType().sensorRadiusSquared)) {
                if (!rc.onTheMap(checkLoc)) {
                    System.out.println("I see an edge");
                    edges[i] = true;
                    if (i == 0) {
                        maxY = checkLoc.y-1;
                        signalQueue.add(getMessage(IC.EDGE_N, maxY));
                        addRelevantFlag(getMessage(IC.EDGE_N, maxY));
                    } else if (i == 1) {
                        maxX = checkLoc.x-1;
                        signalQueue.add(getMessage(IC.EDGE_E, maxX));
                        addRelevantFlag(getMessage(IC.EDGE_E, maxX));
                    } else if (i == 2) {
                        minY = checkLoc.y+1;
                        signalQueue.add(getMessage(IC.EDGE_S, minY));
                        addRelevantFlag(getMessage(IC.EDGE_S, minY));
                    } else if (i == 3) {
                        minX = checkLoc.x+1;
                        signalQueue.add(getMessage(IC.EDGE_W, minX));
                        addRelevantFlag(getMessage(IC.EDGE_W, minX));
                    }
                    System.out.println("updated "+i+"th edge");
                    break;
                }
                checkLoc = checkLoc.add(dir);
            }
        }
        // whether you're a muckraker guarding a slanderer
//        RobotInfo[] closeRobots = rc.senseNearbyRobots(8, team);
//        boolean guard = false;
//        for (RobotInfo rob : closeRobots) {
//            if (rob.getType() == RobotType.SLANDERER) {
//                guard = true;
//                break;
//            }
//        }
        for (RobotInfo r: robots) {
            // check for any ECs
            if (r.getType() == RobotType.ENLIGHTENMENT_CENTER) {
                Debug.p("Found an EC!");
                int id = r.getID();
                Debug.p("ID is: " + id);
                MapLocation loc = r.getLocation();
                Debug.p("Location is: " + loc);
                if (r.getTeam() == team) {
                    int minInd = -1;
                    boolean seen = false;
                    for (int i = 11; i >= 0; i--) {
                        if (ECIds[i] == 0) {
                            minInd = i;
                        }
                        if (ECIds[i] == id) {
                            seen = true;
                            break;
                        }
                    }
                    if (minInd != -1 && !seen) {
                        ECIds[minInd] = id;
                        Debug.p("ID: Adding to signal queue");
                        signalQueue.add(getMessage(IC.EC_ID, id));
                        addRelevantFlag(getMessage(IC.EC_ID, id));
                    }
                    for (int i = 0; i < 12; i++) {
                        if (loc.equals(enemyECs[i])) {
                            enemyECs[i] = null;
                            removeRelevantFlag(getMessage(IC.ENEMY_EC, loc));
                            break;
                        }
                        if (loc.equals(neutralECs[i])) {
                            neutralECs[i] = null;
                            removeRelevantFlag(getMessage(IC.NEUTRAL_EC, loc));
                            break;
                        }
                    }
                    minInd = -1;
                    seen = false;
                    for (int i = 11; i >= 0; i--) {
                        if (friendECs[i] == null) {
                            minInd = i;
                        } else if (friendECs[i].equals(r.getLocation())) {
                            seen = true;
                            break;
                        }
                    }
                    if (minInd != -1 && !seen) {
                        friendECs[minInd] = r.getLocation();
                        Debug.p("FRIENDLY: Adding to signal queue");
                        signalQueue.add(getMessage(IC.FRIEND_EC, loc));
                        addRelevantFlag(getMessage(IC.FRIEND_EC, loc));
                    }
                } else if (r.getTeam() == team.opponent()) {
                    for (int i = 0; i < 12; i++) {
                        if (loc.equals(friendECs[i])) {
                            friendECs[i] = null;
                            removeRelevantFlag(getMessage(IC.FRIEND_EC, loc));
                            break;
                        }
                        if (loc.equals(neutralECs[i])) {
                            neutralECs[i] = null;
                            removeRelevantFlag(getMessage(IC.NEUTRAL_EC, loc));
                            break;
                        }
                    }
                    int minInd = -1;
                    boolean seen = false;
                    for (int i = 11; i >= 0; i--) {
                        if (enemyECs[i] == null) {
                            minInd = i;
                        } else if (enemyECs[i].equals(loc)) {
                            seen = true;
                            break;
                        }
                    }
                    if (minInd != -1 && !seen) {
                        enemyECs[minInd] = r.getLocation();
                        Debug.p("ENEMY: Adding to signal queue");
                        signalQueue.add(getMessage(IC.ENEMY_EC, loc));
                        addRelevantFlag(getMessage(IC.ENEMY_EC, loc));
                    }
                } else {
                    int minInd = -1;
                    boolean seen = false;
                    for (int i = 11; i >= 0; i--) {
                        if (neutralECs[i] == null) {
                            minInd = i;
                        } else if (neutralECs[i].equals(r.getLocation())) {
                            seen = true;
                            break;
                        }
                    }
                    if (minInd != -1 && !seen) {
                        Debug.p("NEUTRAL: Adding to signal queue");
                        signalQueue.add(getMessage(IC.NEUTRAL_EC, loc));
                        addRelevantFlag(getMessage(IC.NEUTRAL_EC, loc));
                    }
                }
            }
            if (rc.getType() != RobotType.ENLIGHTENMENT_CENTER && r.getType() == RobotType.MUCKRAKER && r.getTeam() == team.opponent()) {
                if (rc.getType() == RobotType.SLANDERER) signalQueue.add(getMessage(IC.MUCKRAKER_HELP, r.getLocation()));
                else signalQueue.add(getMessage(IC.MUCKRAKER, r.getLocation()));
            }
//            // if you're a muckraker, check for units
//            if (rc.getType() == RobotType.MUCKRAKER) {
//                if (guard) {
//                    // then relay info about any muckrakers you see
//                    if (r.getType() == RobotType.MUCKRAKER && r.getTeam() == team.opponent()) {
//                        signalQueue.add(getMessage(IC.MUCKRAKER, r.getLocation()));
//                        guard = false;
//                    }
//                }
//                // discuss: do we even need to signal slanderers?
////                if (r.getType() == RobotType.SLANDERER && r.getTeam() == team.opponent()) {
////                    signalQueue.add(getMessage(IC.SLANDERER, r.getLocation()));
////                }
//            }
        }
    }

    // get information from flags
    public void getInfo() throws GameActionException {
        robots = rc.senseNearbyRobots();
        // first get the flags from ECs
        for (int i = 0; i < 12; i++) {
            if (ECIds[i] != 0) {
                if (rc.canGetFlag(ECIds[i])) {
                    processFlag(rc.getFlag(ECIds[i]));
                }
            }
        }
        // get the flags from other nearby units
        for (RobotInfo r : robots) {
            if (r.getTeam() == team && rc.canGetFlag(r.getID())) {
                processingRobot = r;
                processFlag(rc.getFlag(r.getID()));
            }
        }
    }


    // todo: allow ec switching sides
    // process the information gained from flag
    public void processFlag(int flag) {
        IC cat = getCat(flag);
        if (flag % 1000000 == 0 || cat == null) return;
        MapLocation coord = getCoord(flag);
        Debug.p("Signal type: " + cat.toString());
        Debug.p("Signal Coords: " + coord.toString());
        int ID = getID(flag);
        Debug.p("Signal ID: " + ID);
        int minInd;
        boolean seen;
        switch (cat) {
            case EDGE_N:
                if (!edges[0]) {
                    edges[0] = true;
                    maxY = ID;
                    Debug.p("updated "+0+"th edge");
                    addRelevantFlag(getMessage(IC.EDGE_N, maxY));
                }
                break;
            case EDGE_E:
                if (!edges[1]) {
                    edges[1] = true;
                    maxX = ID;
                    Debug.p("updated "+1+"st edge");
                    addRelevantFlag(getMessage(IC.EDGE_E, maxX));
                }
                break;
            case EDGE_S:
                if (!edges[2]) {
                    edges[2] = true;
                    minY = ID;
                    Debug.p("updated "+2+"nd edge");
                    addRelevantFlag(getMessage(IC.EDGE_S, minY));
                }
                break;
            case EDGE_W:
                if (!edges[3]) {
                    edges[3] = true;
                    minX = ID;
                    Debug.p("updated "+3+"rd edge");
                    addRelevantFlag(getMessage(IC.EDGE_W, minX));
                }
                break;
            case ENEMY_EC:
                for (int i = 0; i < 12; i++) {
                    if (coord.equals(friendECs[i])) {
                        friendECs[i] = null;
                        removeRelevantFlag(getMessage(IC.FRIEND_EC, coord));
                        break;
                    }
                    if (coord.equals(neutralECs[i])) {
                        neutralECs[i] = null;
                        removeRelevantFlag(getMessage(IC.NEUTRAL_EC, coord));
                        break;
                    }
                }
                minInd = -1;
                seen = false;
                for (int i = 11; i >= 0; i--) {
                    if (enemyECs[i] == null) {
                        minInd = i;
                    } else if (enemyECs[i].equals(coord)) {
                        seen = true;
                        break;
                    }
                }
                if (minInd != -1 && !seen) {
                    enemyECs[minInd] = coord;
                    addRelevantFlag(getMessage(IC.ENEMY_EC, coord));
                }

                break;
            case FRIEND_EC:
                for (int i = 0; i < 12; i++) {
                    if (coord.equals(enemyECs[i])) {
                        enemyECs[i] = null;
                        removeRelevantFlag(getMessage(IC.ENEMY_EC, coord));
                        break;
                    }
                    if (coord.equals(neutralECs[i])) {
                        neutralECs[i] = null;
                        removeRelevantFlag(getMessage(IC.NEUTRAL_EC, coord));
                        break;
                    }
                }
                minInd = -1;
                seen = false;
                for (int i = 11; i >= 0; i--) {
                    if (friendECs[i] == null) {
                        minInd = i;
                    } else if (friendECs[i].equals(coord)) {
                        seen = true;
                        break;
                    }
                }
                if (minInd != -1 && !seen) {
                    friendECs[minInd] = coord;
                    addRelevantFlag(getMessage(IC.FRIEND_EC, coord));
                }
                break;
            case NEUTRAL_EC:
                minInd = -1;
                seen = false;
                for (int i = 11; i >= 0; i--) {
                    if (neutralECs[i] == null) {
                        minInd = i;
                    }
                    else if (neutralECs[i].equals(coord)) {
                        seen = true;
                        break;
                    }
                }
                if (minInd != -1 && !seen) {
                    neutralECs[minInd] = coord;
                    addRelevantFlag(getMessage(IC.NEUTRAL_EC, coord));
                }
                break;
            case EC_ID:
                minInd = -1;
                seen = false;
                for (int i = 11; i >= 0; i--) {
                    if (ECIds[i] == 0) {
                        minInd = i;
                    }
                    if (ECIds[i] == ID) {
                        seen = true;
                        break;
                    }
                }
                if (minInd != -1 && !seen) {
                    ECIds[minInd] = ID;
                    addRelevantFlag(getMessage(IC.EC_ID, ID));
                }
                break;
            case ATTACK:
                if (rc.getType() == RobotType.ENLIGHTENMENT_CENTER) break;
                moveAway = true;
                attacker = processingRobot.getLocation();
                attackDist = attacker.distanceSquaredTo(coord);
                break;
            case MUCKRAKER:
                if (rc.getType() == RobotType.SLANDERER) {
                    runAway = true;
                    danger = coord;
                }
                break;
            case MUCKRAKER_HELP:
                if (rc.getType() == RobotType.POLITICIAN) {
                    defendSlanderer = true;
                    enemyMuck = coord;
                }
        }
    }


    public void displaySignal() throws GameActionException {
        if (!signalQueue.isEmpty()) {
            int flag = signalQueue.poll();
            rc.setFlag(flag);
        } else {
            rc.setFlag(typeInt(rc.getType()));
        }
    }

    public void addRelevantFlag(int flag) {
        for (int i = 0; i < 20; i++) {
            if (relevantFlags[i] == 0) {
                relevantFlags[i] = flag;
                relevantSize++;
                break;
            }
        }
    }

    public void removeRelevantFlag(int flag) {
        for (int i = 0; i < 20; i++) {
            if (relevantFlags[i] == flag) {
                relevantFlags[i] = 0;
                relevantSize--;
                break;
            }
        }
    }

    // reset all the variables
    public static void resetVariables() {
        moveAway = false;
        defendSlanderer = false;
        runAway = false;
    }
}