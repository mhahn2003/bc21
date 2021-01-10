package coms;

import battlecode.common.*;

import java.util.PriorityQueue;

import static coms.Robot.*;

public class Coms {
    protected final PriorityQueue<Integer> signalQueue = new PriorityQueue<>();

    // number of possible cases for InfoCategory enum class
    private static int numCase = 8;

    public Coms() {
    }

    // TODO: need to order in terms of priority
    public enum InformationCategory {
        EDGE_N,
        EDGE_E,
        EDGE_S,
        EDGE_W,
        ENEMY_EC,
        FRIEND_EC,
        NEUTRAL_EC,
        EC_ID,
    }

    public static int getMessage(InformationCategory cat, MapLocation coord) {
        System.out.println(cat.toString() + " " + coord.toString());
        int message;
        switch (cat) {
            case EDGE_N    : message = 1; break;
            case EDGE_E    : message = 2; break;
            case EDGE_S    : message = 3; break;
            case EDGE_W    : message = 4; break;
            case ENEMY_EC  : message = 5; break;
            case FRIEND_EC : message = 6; break;
            case NEUTRAL_EC: message = 7; break;
            case EC_ID     : message = 8; break;
            default        : message = 9;
        }
        message = addCoord(message, coord);
        return message;
    }

    public static int getMessage(InformationCategory cat, int ID) {
        int message;
        switch (cat) {
            case EDGE_N    : message = 1; break;
            case EDGE_E    : message = 2; break;
            case EDGE_S    : message = 3; break;
            case EDGE_W    : message = 4; break;
            case ENEMY_EC  : message = 5; break;
            case FRIEND_EC : message = 6; break;
            case NEUTRAL_EC: message = 7; break;
            case EC_ID     : message = 8; break;
            default        : message = 9;
        }
        System.out.println(message);
        message = addID(message, ID);
        System.out.println("converting:\n" +cat.toString() + "+" + ID + "\n=>\n" + message);
        return message;
    }

    public static int addCoord(int message, MapLocation coord) {
        return (message<<15)+((coord.x % 128)<<7)+(coord.y % 128);
    }

    public static int addID(int message, int ID) {
        return (message<<15)+ID;
    }


    public static InformationCategory getCat(int message) {
        switch (message>>15) {
            case 1: return InformationCategory.EDGE_N;
            case 2: return InformationCategory.EDGE_E;
            case 3: return InformationCategory.EDGE_S;
            case 4: return InformationCategory.EDGE_W;
            case 5: return InformationCategory.ENEMY_EC;
            case 6: return InformationCategory.FRIEND_EC;
            case 7: return InformationCategory.NEUTRAL_EC;
            case 8: return InformationCategory.EC_ID;
            default: return null;
        }
    }

    public static int getID(int message) {
        return message % 32768;
    }

    public static MapLocation getCoord(int message) {
        MapLocation here = rc.getLocation();
        int remX = here.x % 128;
        int remY = here.y % 128;
        message = message % 32768;
        int x = message>>7;
        int y = message % 128;
        if (Math.abs(x-remX) >= 64) {
            if (x > remX) x = here.x-remX-128+x;
            else x = here.x-remX+x+128;
        } else x = here.x-remX+x;
        if (Math.abs(y-remY) >= 64) {
            if (y > remY) y = here.y-remY-128+y;
            else y = here.y+y+128-remY;
        } else y = here.y-remY+y;
        return new MapLocation(x, y);
    }

    // relay information about surroundings
    public void collectInfo() throws GameActionException {
        // first check for any edges
        for (int i = 0; i < 4; i++) {
            if (edges[i]){continue;}
            Direction dir = Direction.cardinalDirections()[i];
            MapLocation checkLoc = rc.getLocation().add(dir);
            while (checkLoc.isWithinDistanceSquared(rc.getLocation(), rc.getType().sensorRadiusSquared)) {
                if (!rc.onTheMap(checkLoc)) {
                    System.out.println("I see an edge");
                    edges[i] = true;
                    if       (i == 0) {
                        maxY = checkLoc.y-1;
                        signalQueue.add(getMessage(InformationCategory.EDGE_N, maxY));
                    }else if (i == 1) {
                        maxX = checkLoc.x-1;
                        signalQueue.add(getMessage(InformationCategory.EDGE_E, maxX));
                    }else if (i == 2) {
                        minY = checkLoc.y+1;
                        signalQueue.add(getMessage(InformationCategory.EDGE_S, minY));
                    }else if (i == 3) {
                        minX = checkLoc.x+1;
                        signalQueue.add(getMessage(InformationCategory.EDGE_W, minX));
                    }
                    System.out.println("updated "+i+"th edge");
                    break;
                }
                checkLoc = checkLoc.add(dir);
            }
        }
        // check for any ECs
        RobotInfo[] robots = rc.senseNearbyRobots();
        for (RobotInfo r: robots) {
            if (r.getType() == RobotType.ENLIGHTENMENT_CENTER) {
                int id = r.getID();
                if (!ECLoc.containsKey(r.getID())) {
                    MapLocation loc = r.getLocation();
                    ECLoc.put(id, loc);
                    if (r.getTeam() == team) {
                        int minInd = -1;
                        boolean seen = false;
                        for (int i = 11; i >= 0; i--) {
                            if (ECIds[i] == 0) {
                                minInd = i;
                            }
                            if (ECIds[i] == r.getID()) {
                                seen = true;
                                break;
                            }
                        }
                        if (minInd != -1 && !seen) ECIds[minInd] = r.getID();
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
                            signalQueue.add(getMessage(InformationCategory.FRIEND_EC, loc));
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
                            } else if (enemyECs[i].equals(r.getLocation())) {
                                seen = true;
                                break;
                            }
                        }
                        if (minInd != -1 && !seen) {
                            enemyECs[minInd] = r.getLocation();
                            signalQueue.add(getMessage(InformationCategory.ENEMY_EC, loc));
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
                        }
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


    //todo: allow ec swithcing sides
    // process the information gained from flag
    public void processFlag(int flag) {
        InformationCategory cat = getCat(flag);
        if (cat == null){return;}
        MapLocation coord = getCoord(flag);
        System.out.println("processing signal:" + cat.toString());
        System.out.println("processing signal:" + coord.toString());
        int ID = getID(flag);
        int minInd;
        boolean seen;
        switch (cat) {
            case EDGE_N : if(!edges[0]){edges[0]=true;maxY=ID;System.out.println("updated "+0+"th edge");}break;
            case EDGE_E : if(!edges[1]){edges[1]=true;maxX=ID;System.out.println("updated "+1+"st edge");}break;
            case EDGE_S : if(!edges[2]){edges[2]=true;minY=ID;System.out.println("updated "+2+"nd edge");}break;
            case EDGE_W : if(!edges[3]){edges[3]=true;minX=ID;System.out.println("updated "+3+"rd edge");}break;
            case ENEMY_EC:
                // add the coordinate into the list at the first empty slot
                if (!ECLoc.containsValue(coord)) {
                    for (int i = 0; i < 12; i++) {
                        if (enemyECs[i] == null) {
                            enemyECs[i] = coord;
                            break;
                        }
                    }
                }
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
                if (minInd != -1 && !seen) enemyECs[minInd] = coord;
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
                if (minInd != -1 && !seen) friendECs[minInd] = coord;
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
                if (minInd != -1 && !seen) neutralECs[minInd] = coord;
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
                if (minInd != -1 && !seen) ECIds[minInd] = ID;
                break;
        }
    }


    public void displaySignal() throws GameActionException {
        if (!signalQueue.isEmpty()) {
            int flag = signalQueue.poll();
            System.out.println("showing:" + flag);
            rc.setFlag(flag);
        }
    }
}