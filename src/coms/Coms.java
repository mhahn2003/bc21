package coms;

import battlecode.common.*;

import java.util.PriorityQueue;

import static coms.Robot.*;

public class Coms {
    public static RobotController rc;
    public static PriorityQueue<Integer> signalQueue = new PriorityQueue<>();

    // number of possible cases for InfoCategory enum class
    private static int numCase = 4;

    public Coms(RobotController r) {
        rc = r;
    }

    // TODO: need to order in terms of priority
    public enum InformationCategory {
        EDGE,
        ENEMY_EC,
        EC,
        NEUTRAL_EC,
        EC_ID,
    }

    public static int getMessage(InformationCategory cat, MapLocation coord) {
        int message = 0;
        switch (cat) {
            case EDGE: message = 1; break;
            case ENEMY_EC: message = 2; break;
            case EC: message = 3; break;
            case NEUTRAL_EC: message = 4; break;
            case EC_ID: message = 5; break;
            default: message = 6;
        }
        message = addCoord(message, coord);
        return message;
    }

    public static int getMessage(InformationCategory cat, int ID) {
        int message = 0;
        switch (cat) {
            case EDGE: message = 1; break;
            case ENEMY_EC: message = 2; break;
            case EC: message = 3; break;
            case NEUTRAL_EC: message = 4; break;
            case EC_ID: message = 5; break;
            default: message = 6;
        }
        message = addID(message, ID);
        return message;
    }

    public static int addCoord(int message, MapLocation coord) {
        return message*32768+(coord.x % 128)*128+(coord.y % 128);
    }

    public static int addID(int message, int ID) {
        return message*32768+ID;
    }

    public static InformationCategory getCat(int message) {
        switch (message/32768) {
            case 1: return InformationCategory.EDGE;
            case 2: return InformationCategory.ENEMY_EC;
            case 3: return InformationCategory.EC;
            case 4: return InformationCategory.NEUTRAL_EC;
            case 5: return InformationCategory.EC_ID;
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
        int x = message/128;
        int y = message % 128;
        if (Math.abs(x-remX) >= 64) {
            if (x > remX) x = here.x-remX-128+x;
            else x = here.x+x+128-remX;
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
            Direction dir = Direction.cardinalDirections()[i];
            System.out.println(dir.toString());
            MapLocation checkLoc = rc.getLocation().add(dir);
            while (checkLoc.isWithinDistanceSquared(rc.getLocation(), rc.getType().sensorRadiusSquared)) {
                if (!rc.onTheMap(checkLoc)) {
                    System.out.println("I see an edge");
                    if (!edges[i]) {
                        // comm this information
                        edges[i] = true;
                        if (i == 0) {
                            maxY = checkLoc.y-1;
                            signalQueue.add(getMessage(InformationCategory.EDGE, new MapLocation(30065, maxY)));
                        }
                        if (i == 1) {
                            maxX = checkLoc.x-1;
                            signalQueue.add(getMessage(InformationCategory.EDGE, new MapLocation(maxX, 30065)));
                        }
                        if (i == 2) {
                            minY = checkLoc.y+1;
                            signalQueue.add(getMessage(InformationCategory.EDGE, new MapLocation(9999, minY)));
                        }
                        if (i == 3) {
                            minX = checkLoc.x+1;
                            signalQueue.add(getMessage(InformationCategory.EDGE, new MapLocation(minX, 9999)));
                        }
                    }
                    break;
                }
                checkLoc = checkLoc.add(dir);
            }
        }
        // check for any ECs

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
                            if (ECs[i] == null) {
                                minInd = i;
                            } else if (ECs[i].equals(r.getLocation())) {
                                seen = true;
                                break;
                            }
                        }
                        if (minInd != -1 && !seen) {
                            ECs[minInd] = r.getLocation();
                            signalQueue.add(getMessage(InformationCategory.EC, loc));
                        }
                    } else if (r.getTeam() == team.opponent()) {
                        for (int i = 0; i < 12; i++) {
                            if (loc.equals(ECs[i])) {
                                ECs[i] = null;
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
        // set flag
        if (!signalQueue.isEmpty()) {
            int flag = signalQueue.poll();
            rc.setFlag(flag);
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

    // process the information gained from flag
    public void processFlag(int flag) {
        if (flag == 0) return;
        MapLocation coord = getCoord(flag);
        int ID = getID(flag);
        int minInd;
        boolean seen;
        switch (getCat(flag)) {
            case EDGE:
                if (coord.x == 9999) {
                    edges[2] = true;
                    minY = coord.y;
                }
                if (coord.x == 30065) {
                    edges[0] = true;
                    maxY = coord.y;
                }
                if (coord.y == 9999) {
                    edges[3] = true;
                    minX = coord.x;
                }
                if (coord.y == 30065) {
                    edges[1] = true;
                    maxX = coord.x;
                }
                break;
            case ENEMY_EC:
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
            case EC:
                minInd = -1;
                seen = false;
                for (int i = 11; i >= 0; i--) {
                    if (ECs[i] == null) {
                        minInd = i;
                    }
                    else if (ECs[i].equals(coord)) {
                        seen = true;
                        break;
                    }
                }
                if (minInd != -1 && !seen) ECs[minInd] = coord;
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

}