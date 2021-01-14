package coms;

import battlecode.common.*;
import coms.utils.Debug;

import java.util.PriorityQueue;

import static coms.Robot.*;

public class Coms {
    protected final PriorityQueue<Integer> signalQueue = new PriorityQueue<>();
    protected int relevantSize = 0;
    protected int relevantInd = 0;
    protected int[] relevantFlags = new int[20];


    // number of possible cases for InfoCategory enum class
    private static int numCase = 8;

    // first 15 bit is message (only 14 is used for transimitting location)
    // next 4 bits are catagory (an extra bit is only added for the 8 (100) since it is 4 bits)
    // next 3 bits are unit type (an extra bit is only added for the 4 (100) since it is 3 bits)
    // discuss: want to cut it down by one?

    // (every trailing f is 4 bit. lead can be f,7,3,1 standing for 4,3,2,1 bits)

    public Coms() {
    }

    // TODO: need to order in terms of priority
    public enum InformationCategory {
        FRIEND_EC,
        EC_ID,
        ENEMY_EC,
        NEUTRAL_EC,
        EDGE_N,
        EDGE_E,
        EDGE_S,
        EDGE_W,
    }

    public static int getMessage(InformationCategory cat, MapLocation coord) {
        int message;
        switch (cat) {
            case FRIEND_EC : message = 1; break;
            case EC_ID     : message = 2; break;
            case ENEMY_EC  : message = 3; break;
            case NEUTRAL_EC: message = 4; break;
            case EDGE_N    : message = 5; break;
            case EDGE_E    : message = 6; break;
            case EDGE_S    : message = 7; break;
            case EDGE_W    : message = 8; break;
            default        : message = 9;
        }
        message = addCoord(message, coord) + typeInt(rc.getType());
        return message;
    }

    public static int getMessage(InformationCategory cat, int ID) {
        int message;
        switch (cat) {
            case FRIEND_EC : message = 1; break;
            case EC_ID     : message = 2; break;
            case ENEMY_EC  : message = 3; break;
            case NEUTRAL_EC: message = 4; break;
            case EDGE_N    : message = 5; break;
            case EDGE_E    : message = 6; break;
            case EDGE_S    : message = 7; break;
            case EDGE_W    : message = 8; break;
            default        : message = 9;
        }
        message = addID(message, ID) + typeInt(rc.getType())<<19;
        return message;
    }

    public static int typeInt(RobotType type) {
        switch (type) {
            case POLITICIAN: return 1;
            case SLANDERER: return 2;
            case MUCKRAKER: return 3;
            case ENLIGHTENMENT_CENTER: return 4;
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
        switch (message>>19) {
            case 1: return RobotType.POLITICIAN;
            case 2: return RobotType.SLANDERER;
            case 3: return RobotType.MUCKRAKER;
            case 4: return RobotType.ENLIGHTENMENT_CENTER;
        }
        return null;
    }


    public static InformationCategory getCat(int message) {
        message = message & 0x7ffff;
        switch (message >> 15) {
            case 1: return InformationCategory.FRIEND_EC;
            case 2: return InformationCategory.EC_ID;
            case 3: return InformationCategory.ENEMY_EC;
            case 4: return InformationCategory.NEUTRAL_EC;
            case 5: return InformationCategory.EDGE_N;
            case 6: return InformationCategory.EDGE_E;
            case 7: return InformationCategory.EDGE_S;
            case 8: return InformationCategory.EDGE_W;
            default: return null;
        }
    }

    public static int getID(int message) {
        message = message & 0x7ffff;
        return message % 32768;
    }

    public static MapLocation getCoord(int message) {
        message = message & 0x7ffff;
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
                        relevantFlags[relevantSize] = getMessage(InformationCategory.EDGE_N, maxY);
                    } else if (i == 1) {
                        maxX = checkLoc.x-1;
                        relevantFlags[relevantSize] = getMessage(InformationCategory.EDGE_E, maxX);
                    } else if (i == 2) {
                        minY = checkLoc.y+1;
                        relevantFlags[relevantSize] = getMessage(InformationCategory.EDGE_S, minY);
                    } else if (i == 3) {
                        minX = checkLoc.x+1;
                        relevantFlags[relevantSize] = getMessage(InformationCategory.EDGE_W, minX);
                    }
                    signalQueue.add(relevantFlags[relevantSize]);
                    relevantSize++;
                    System.out.println("updated "+i+"th edge");
                    break;
                }
                checkLoc = checkLoc.add(dir);
            }
        }
        // check for any ECs
        for (RobotInfo r: robots) {
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
                        signalQueue.add(getMessage(InformationCategory.EC_ID, id));
                        relevantFlags[relevantSize] = getMessage(InformationCategory.EC_ID, id);
                        relevantSize++;
                    }
                    for (int i = 0; i < 12; i++) {
                        if (loc.equals(enemyECs[i])) {
                            enemyECs[i] = null;
                            break;
                        }
                        if (loc.equals(neutralECs[i])) {
                            neutralECs[i] = null;
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
                        signalQueue.add(getMessage(InformationCategory.FRIEND_EC, loc));
                        relevantFlags[relevantSize] = getMessage(InformationCategory.FRIEND_EC, loc);
                        relevantSize++;
                    }
                } else if (r.getTeam() == team.opponent()) {
                    for (int i = 0; i < 12; i++) {
                        if (loc.equals(friendECs[i])) {
                            friendECs[i] = null;
                            break;
                        }
                        if (loc.equals(neutralECs[i])) {
                            neutralECs[i] = null;
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
                        signalQueue.add(getMessage(InformationCategory.ENEMY_EC, loc));
                        relevantFlags[relevantSize] = getMessage(InformationCategory.ENEMY_EC, loc);
                        relevantSize++;
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
                        ECIds[minInd] = r.getID();
                        signalQueue.add(getMessage(InformationCategory.NEUTRAL_EC, loc));
                        Debug.p("NEUTRAL: Adding to signal queue");
                        relevantFlags[relevantSize] = getMessage(InformationCategory.NEUTRAL_EC, loc);
                        relevantSize++;
                    }
                }
            }
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
                processFlag(rc.getFlag(r.getID()));
            }
        }
    }


    // todo: allow ec switching sides
    // process the information gained from flag
    public void processFlag(int flag) {
        InformationCategory cat = getCat(flag);
        if ((flag & 0x7ffff) == 0 || cat == null) return;
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
                    relevantFlags[relevantSize] = getMessage(InformationCategory.EDGE_N, maxY);
                    relevantSize++;
                }
                break;
            case EDGE_E:
                if (!edges[1]) {
                    edges[1] = true;
                    maxX = ID;
                    Debug.p("updated "+1+"st edge");
                    relevantFlags[relevantSize] = getMessage(InformationCategory.EDGE_E, maxX);
                    relevantSize++;
                }
                break;
            case EDGE_S:
                if (!edges[2]) {
                    edges[2] = true;
                    minY = ID;
                    Debug.p("updated "+2+"nd edge");
                    relevantFlags[relevantSize] = getMessage(InformationCategory.EDGE_S, minY);
                    relevantSize++;
                }
                break;
            case EDGE_W:
                if (!edges[3]) {
                    edges[3] = true;
                    minX = ID;
                    Debug.p("updated "+3+"rd edge");
                    relevantFlags[relevantSize] = getMessage(InformationCategory.EDGE_W, minX);
                    relevantSize++;
                }
                break;
            case ENEMY_EC:
                minInd = -1;
                seen = false;
                for (int i = 11; i >= 0; i--) {
                    if (enemyECs[i] == null) {
                        minInd = i;
                    }
                    else if (enemyECs[i].equals(coord)) {
                        seen = true;
                        break;
                    }
                }
                if (minInd != -1 && !seen) {
                    enemyECs[minInd] = coord;
                    relevantFlags[relevantSize] = getMessage(InformationCategory.ENEMY_EC, coord);
                    relevantSize++;
                }
                break;
            case FRIEND_EC:
                minInd = -1;
                seen = false;
                for (int i = 11; i >= 0; i--) {
                    if (friendECs[i] == null) {
                        minInd = i;
                    }
                    else if (friendECs[i].equals(coord)) {
                        seen = true;
                        break;
                    }
                }
                if (minInd != -1 && !seen) {
                    friendECs[minInd] = coord;
                    relevantFlags[relevantSize] = getMessage(InformationCategory.FRIEND_EC, coord);
                    relevantSize++;
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
                    relevantFlags[relevantSize] = getMessage(InformationCategory.NEUTRAL_EC, coord);
                    relevantSize++;
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
                    relevantFlags[relevantSize] = getMessage(InformationCategory.EC_ID, ID);
                    relevantSize++;
                }
                break;
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
}