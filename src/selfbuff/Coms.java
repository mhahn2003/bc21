package selfbuff;

import battlecode.common.*;
import selfbuff.utils.Debug;

import java.util.PriorityQueue;

import static selfbuff.Robot.*;
import static selfbuff.utils.Constants.staleCooldown;

public class Coms {
    public static PriorityQueue<Integer> signalQueue = new PriorityQueue<>();
    protected int relevantSize = 0;
    protected int relevantInd = 0;
    protected int[] relevantFlags = new int[30];
    protected RobotInfo processingRobot = null;


    // number of possible cases for InfoCategory enum class
    private static int numCase = 17;

    //[0,16] bits for information (17 bits) (msg)    ( msg&0x00ffff     )
    //[17,21] for IC (5 bits)               (msg<<17)((msg&0x1f0000)>>17)
    //[22,23] for robot type (2 bits)       (msg<<22)((msg&0x600000)>>22)

    private static int bitMaskInfo = 0x01ffff; //0000 0001 1111 1111 1111 1111
    private static int bitMaskIC   = 0x3e0000; //0011 1110 0000 0000 0000 0000
    private static int bitMaskType = 0xc00000; //1100 0000 0000 0000 0000 0000
    // private static int bitShiftInfo = 0 ;
    private static int bitShiftIC   = 17;
    private static int bitShiftType = 22;

    public Coms() {
    }

    // TODO: need to order in terms of priority
    public enum IC {
        MUCKRAKER_HELP,
        FRIEND_EC,
        EC_ID,
        ENEMY_EC,
        NEUTRAL_EC,
        POLITICIAN,
        SLANDERER,
        MAP_CORNER,
        MAP_NW,
        MAP_NE,
        MAP_SW,
        MAP_SE,
        MUCKRAKER,
        EDGE_N,
        EDGE_E,
        EDGE_S,
        EDGE_W,
        ATTACK,
    }

    public static int getMessage(IC cat, MapLocation coord) {
        return typeInt(rc.getType()) + addIC(cat) + addCoord(coord);
    }

    public static int getMessage(IC cat, int ID) {
        return typeInt(rc.getType()) + addIC(cat) + addID(ID);
    }


    public static int typeInt(RobotType type) {
        switch (type) {
            case POLITICIAN          : return 0;
            case SLANDERER           : return 1 << bitShiftType;
            case MUCKRAKER           : return 2 << bitShiftType;
            case ENLIGHTENMENT_CENTER: return 3 << bitShiftType;
        }
        return 0;
    }

    public static int addIC(IC cat){
        switch (cat) {
            case MUCKRAKER_HELP: return 1  << bitShiftIC;
            case FRIEND_EC     : return 2  << bitShiftIC;
            case EC_ID         : return 3  << bitShiftIC;
            case ENEMY_EC      : return 4  << bitShiftIC;
            case NEUTRAL_EC    : return 5  << bitShiftIC;
            case POLITICIAN    : return 6  << bitShiftIC;
            case SLANDERER     : return 7  << bitShiftIC;
            case MAP_CORNER    : return 8  << bitShiftIC;
            case MAP_NW        : return 9  << bitShiftIC;
            case MAP_NE        : return 10 << bitShiftIC;
            case MAP_SW        : return 11 << bitShiftIC;
            case MAP_SE        : return 12 << bitShiftIC;
            case MUCKRAKER     : return 13 << bitShiftIC;
            case EDGE_N        : return 14 << bitShiftIC;
            case EDGE_E        : return 15 << bitShiftIC;
            case EDGE_S        : return 16 << bitShiftIC;
            case EDGE_W        : return 17 << bitShiftIC;
            case ATTACK        : return 18 << bitShiftIC;
            default            : return 19 << bitShiftIC;
        }
    }

    public static int addCoord( MapLocation coord) {
        return ((coord.x % 128) << 7) + (coord.y % 128);
    }

    public static int addID( int ID) {
        return ID;
    }

    public static RobotType getTyp(int message) {
        switch ( (message & bitMaskType) >> bitMaskType) {
            case 0: return RobotType.POLITICIAN          ;
            case 1: return RobotType.SLANDERER           ;
            case 2: return RobotType.MUCKRAKER           ;
            case 3: return RobotType.ENLIGHTENMENT_CENTER;
        }
        return null;
    }

    public static IC getCat(int message) {
        switch ( (message & bitMaskIC) >> bitShiftIC) {
            case 1 : return IC.MUCKRAKER_HELP;
            case 2 : return IC.FRIEND_EC;
            case 3 : return IC.EC_ID;
            case 4 : return IC.ENEMY_EC;
            case 5 : return IC.NEUTRAL_EC;
            case 6 : return IC.POLITICIAN;
            case 7 : return IC.SLANDERER;
            case 8 : return IC.MAP_CORNER;
            case 9 : return IC.MAP_NW;
            case 10: return IC.MAP_NE;
            case 11: return IC.MAP_SW;
            case 12: return IC.MAP_SE;
            case 13: return IC.MUCKRAKER;
            case 14: return IC.EDGE_N;
            case 15: return IC.EDGE_E;
            case 16: return IC.EDGE_S;
            case 17: return IC.EDGE_W;
            case 18: return IC.ATTACK;
            default: return null;
        }
    }

    public static MapLocation getCoord(int message) {
        MapLocation here = rc.getLocation();
        int remX = here.x % 128;
        int remY = here.y % 128;
        message = message & bitMaskInfo;
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

    public static int getID(int message) {
        return message & bitMaskInfo;
    }

    // relay information about surroundings
    public void collectInfo() throws GameActionException {
        // temporary fix for reducing bytecode for slanderers: fix
        // TODO: reduce bytecode usage in general
        if (rc.getType() == RobotType.SLANDERER) {
            RobotInfo[] enemies = rc.senseNearbyRobots(-1, team.opponent());
            for (RobotInfo r : enemies) {
                if (r.getType() == RobotType.MUCKRAKER) {
                    signalQueue.add(getMessage(IC.MUCKRAKER_HELP, r.getLocation()));
                    break;
                }
            }
        } else {
            // first check for any edges
            for (int a = 0; a < 4; a++) {
                if (edges[a]) continue;
                Direction dir = Direction.cardinalDirections()[a];
                MapLocation checkLoc = rc.getLocation().add(dir);
                while (checkLoc.isWithinDistanceSquared(rc.getLocation(), rc.getType().sensorRadiusSquared)) {
                    if (!rc.onTheMap(checkLoc)) {
                        edges[a] = true;
                        if (a == 0) {
                            maxY = checkLoc.y-1;
                            signalQueue.add(getMessage(IC.EDGE_N, maxY));
                            addRelevantFlag(getMessage(IC.EDGE_N, maxY));
                            if (mapGenerated) {
                                // rule out some spots
                                for (int i = 7; i >= 0; i--) {
                                    for (int j = 7; j >= 0; j--) {
                                        if (mapSpots[i][j].y > maxY) visited[i][j] = true;
                                    }
                                }
                            }
                        } else if (a == 1) {
                            maxX = checkLoc.x-1;
                            signalQueue.add(getMessage(IC.EDGE_E, maxX));
                            addRelevantFlag(getMessage(IC.EDGE_E, maxX));
                            if (mapGenerated) {
                                // rule out some spots
                                for (int i = 7; i >= 0; i--) {
                                    for (int j = 7; j >= 0; j--) {
                                        if (mapSpots[i][j].x > maxX) visited[i][j] = true;
                                    }
                                }
                            }
                        } else if (a == 2) {
                            minY = checkLoc.y+1;
                            signalQueue.add(getMessage(IC.EDGE_S, minY));
                            addRelevantFlag(getMessage(IC.EDGE_S, minY));
                            if (mapGenerated) {
                                // rule out some spots
                                for (int i = 7; i >= 0; i--) {
                                    for (int j = 7; j >= 0; j--) {
                                        if (mapSpots[i][j].y < minY) visited[i][j] = true;
                                    }
                                }
                            }
                        } else if (a == 3) {
                            minX = checkLoc.x+1;
                            signalQueue.add(getMessage(IC.EDGE_W, minX));
                            addRelevantFlag(getMessage(IC.EDGE_W, minX));
                            if (mapGenerated) {
                                // rule out some spots
                                for (int i = 7; i >= 0; i--) {
                                    for (int j = 7; j >= 0; j--) {
                                        if (mapSpots[i][j].x < minX) visited[i][j] = true;
                                    }
                                }
                            }
                        }
                        break;
                    }
                    checkLoc = checkLoc.add(dir);
                }
            }
            if (mapGenerated && rc.getType() != RobotType.ENLIGHTENMENT_CENTER) {
                int offsetX = (rc.getLocation().x - mapSpots[0][0].x + 8) % 8;
                int offsetY = (rc.getLocation().y - mapSpots[0][0].y + 8) % 8;
                if ((offsetX == 0 || offsetX == 7) && (offsetY == 0 || offsetY == 7)) {
                    // at one of the middle spots
                    int x = (rc.getLocation().x - mapSpots[0][0].x + 1)/8;
                    int y = (rc.getLocation().y - mapSpots[0][0].y + 1)/8;
                    if (!visited[x][y]) {
                        visited[x][y] = true;
                        if (x < 4 && y < 4) {
                            // SW
                            int msgSum = 0;
                            for (int i = 0; i < 4; i++) {
                                for (int j = 0; j < 4; j++) {
                                    if (visited[i][j]) msgSum += (1 << (i*4+j));
                                }
                            }
                            if (rc.getType() == RobotType.MUCKRAKER) signalQueue.add(getMessage(IC.MAP_SW, msgSum));
                        }
                        if (x >= 4 && y < 4) {
                            // SE
                            int msgSum = 0;
                            for (int i = 4; i < 8; i++) {
                                for (int j = 0; j < 4; j++) {
                                    if (visited[i][j]) msgSum += (1 << ((i-4)*4+j));
                                }
                            }
                            if (rc.getType() == RobotType.MUCKRAKER) signalQueue.add(getMessage(IC.MAP_SE, msgSum));
                        }
                        if (x < 4 && y >= 4) {
                            // NW
                            int msgSum = 0;
                            for (int i = 0; i < 4; i++) {
                                for (int j = 4; j < 8; j++) {
                                    if (visited[i][j]) msgSum += (1 << (i*4+j-4));
                                }
                            }
                            if (rc.getType() == RobotType.MUCKRAKER) signalQueue.add(getMessage(IC.MAP_NW, msgSum));
                        }
                        if (x >= 4 && y >= 4) {
                            // NE
                            int msgSum = 0;
                            for (int i = 4; i < 8; i++) {
                                for (int j = 4; j < 8; j++) {
                                    if (visited[i][j]) msgSum += (1 << ((i-4)*4+j-4));
                                }
                            }
                            if (rc.getType() == RobotType.MUCKRAKER) signalQueue.add(getMessage(IC.MAP_NE, msgSum));
                        }
                    }
                }
            }
//            Nav.getEnds();
//            // check for any corners
//            boolean update = false;
//            for (int i = 0; i < 9; i++) {
//                if (corners[i]) continue;
//                if (rc.canSenseLocation(ends[i])) {
//                    corners[i] = true;
//                    update = true;
//                }
//            }
//            if (update) {
//                int msgSum = 0;
//                for (int i = 0; i < 9; i++) {
//                    if (corners[i]) {
//                        msgSum += (1 << i);
//                    }
//                }
//                signalQueue.add(getMessage(IC.ENDS, msgSum));
//                // need to check relevant flags and replace the previous ends flag if there is any
//                for (int i = 0; i < 20; i++) {
//                    if (getCat(relevantFlags[i]) == IC.ENDS) {
//                        removeRelevantFlag(relevantFlags[i]);
//                        break;
//                    }
//                }
//                addRelevantFlag(getMessage(IC.ENDS, msgSum));
//            }

            // whether you're a guarding a slanderer
            RobotInfo[] closeRobots = rc.senseNearbyRobots(8, team);
            boolean guard = false;
            for (RobotInfo rob : closeRobots) {
                if (rob.getType() == RobotType.SLANDERER) {
                    guard = true;
                    break;
                }
            }
            boolean foundSlanderer = false;
            for (RobotInfo r: robots) {
                // check for any ECs
                if (r.getType() == RobotType.ENLIGHTENMENT_CENTER) {
                        int id = r.getID();
                        MapLocation loc = r.getLocation();
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
                            signalQueue.add(getMessage(IC.NEUTRAL_EC, loc));
                            addRelevantFlag(getMessage(IC.NEUTRAL_EC, loc));
                        }
                    }
                }
                if (rc.getType() != RobotType.ENLIGHTENMENT_CENTER && r.getType() == RobotType.MUCKRAKER && r.getTeam() == team.opponent()) {
                    if (rc.getType() == RobotType.SLANDERER)
                        signalQueue.add(getMessage(IC.MUCKRAKER_HELP, r.getLocation()));
                    else if (guard) {
                        // if you're near a slanderer
                        signalQueue.add(getMessage(IC.MUCKRAKER, r.getLocation()));
                    }
                }
                // if you're a muckraker, check for units
                if (rc.getType() == RobotType.MUCKRAKER && !foundSlanderer) {
                    if (r.getType() == RobotType.SLANDERER && r.getTeam() == team.opponent()) {
                        foundSlanderer = true;
                        signalQueue.add(getMessage(IC.SLANDERER, r.getLocation()));
                    }
                }
            }
        }
    }

    // get information from flags
    public void getInfo() throws GameActionException {
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
        if ( (flag & (bitMaskIC | bitMaskInfo)) == 0 || cat == null) return;
        MapLocation coord = getCoord(flag);
        int ID = getID(flag);
        int minInd;
        boolean seen;
        switch (cat) {
            case EDGE_N:
                if (!edges[0]) {
                    edges[0] = true;
                    maxY = ID;
                    addRelevantFlag(getMessage(IC.EDGE_N, maxY));
                    if (mapGenerated) {
                        // rule out some spots
                        for (int i = 7; i >= 0; i--) {
                            for (int j = 7; j >= 0; j--) {
                                if (mapSpots[i][j].y > maxY) visited[i][j] = true;
                            }
                        }
                    }
                }
                break;
            case EDGE_E:
                if (!edges[1]) {
                    edges[1] = true;
                    maxX = ID;
                    addRelevantFlag(getMessage(IC.EDGE_E, maxX));
                    if (mapGenerated) {
                        // rule out some spots
                        for (int i = 7; i >= 0; i--) {
                            for (int j = 7; j >= 0; j--) {
                                if (mapSpots[i][j].x > maxX) visited[i][j] = true;
                            }
                        }
                    }
                }
                break;
            case EDGE_S:
                if (!edges[2]) {
                    edges[2] = true;
                    minY = ID;
                    addRelevantFlag(getMessage(IC.EDGE_S, minY));
                    if (mapGenerated) {
                        // rule out some spots
                        for (int i = 7; i >= 0; i--) {
                            for (int j = 7; j >= 0; j--) {
                                if (mapSpots[i][j].y < minY) visited[i][j] = true;
                            }
                        }
                    }
                }
                break;
            case EDGE_W:
                if (!edges[3]) {
                    edges[3] = true;
                    minX = ID;
                    addRelevantFlag(getMessage(IC.EDGE_W, minX));
                    if (mapGenerated) {
                        // rule out some spots
                        for (int i = 7; i >= 0; i--) {
                            for (int j = 7; j >= 0; j--) {
                                if (mapSpots[i][j].x > minX) visited[i][j] = true;
                            }
                        }
                    }
                }
                break;
            case MAP_CORNER:
                if (!mapGenerated) {
                    mapType = ID;
                    if (mapType == 0 && edges[0] && edges[1]) {
                        mapGenerated = true;
                        // NE corner
                        int initX = maxX-3;
                        int initY = maxY-3;
                        for (int i = 7; i >= 0; i--) {
                            for (int j = 7; j >= 0; j--) {
                                visited[i][j] = false;
                                mapSpots[i][j] = new MapLocation(initX-8*(7-i), initY-8*(7-j));
                            }
                        }
                        addRelevantFlag(getMessage(IC.MAP_CORNER, 0));
                    }
                    if (mapType == 1 && edges[1] && edges[2]) {
                        mapGenerated = true;
                        // SE corner
                        int initX = maxX-3;
                        int initY = minY+4;
                        for (int i = 7; i >= 0; i--) {
                            for (int j = 7; j >= 0; j--) {
                                visited[i][j] = false;
                                mapSpots[i][j] = new MapLocation(initX-8*(7-i), initY+8*j);
                            }
                        }
                        addRelevantFlag(getMessage(IC.MAP_CORNER, 1));
                    }
                    if (mapType == 2 && edges[2] && edges[3]) {
                        mapGenerated = true;
                        // SW corner
                        int initX = minX+4;
                        int initY = minY+4;
                        for (int i = 7; i >= 0; i--) {
                            for (int j = 7; j >= 0; j--) {
                                visited[i][j] = false;
                                mapSpots[i][j] = new MapLocation(initX+8*i, initY+8*j);
                            }
                        }
                        addRelevantFlag(getMessage(IC.MAP_CORNER, 2));
                    }
                    if (mapType == 3 && edges[3] && edges[0]) {
                        mapGenerated = true;
                        // NW corner
                        int initX = minX+4;
                        int initY = maxY-3;
                        for (int i = 7; i >= 0; i--) {
                            for (int j = 7; j >= 0; j--) {
                                visited[i][j] = false;
                                mapSpots[i][j] = new MapLocation(initX+8*i, initY-8*(7-j));
                            }
                        }
                        addRelevantFlag(getMessage(IC.MAP_CORNER, 3));
                    }
                    if (mapGenerated) {
                        for (int i = 7; i >= 0; i--) {
                            for (int j = 7; j >= 0; j--) {
                                MapLocation loc = mapSpots[i][j];
                                if (loc.x > maxX || loc.y > maxY || loc.x < minX || loc.y < minY) visited[i][j] = true;
                            }
                        }
                    }
                }
                break;
            case MAP_NE:
                for (int i = 4; i < 8; i++) {
                    for (int j = 4; j < 8; j++) {
                        if (!visited[i][j] && (ID & (1 << ((i-4)*4+j-4))) != 0) {
                            visited[i][j] = true;
                            updateNE = true;
                        }
                    }
                }
                break;
            case MAP_NW:
                for (int i = 0; i < 4; i++) {
                    for (int j = 4; j < 8; j++) {
                        if (!visited[i][j] && (ID & (1 << (i*4+j-4))) != 0) {
                            visited[i][j] = true;
                            updateNW = true;
                        }
                    }
                }
                break;
            case MAP_SE:
                for (int i = 4; i < 8; i++) {
                    for (int j = 0; j < 4; j++) {
                        if (!visited[i][j] && (ID & (1 << ((i-4)*4+j))) != 0) {
                            visited[i][j] = true;
                            updateSE = true;
                        }
                    }
                }
                break;
            case MAP_SW:
                for (int i = 0; i < 4; i++) {
                    for (int j = 0; j < 4; j++) {
                        if (!visited[i][j] && (ID & (1 << (i*4+j))) != 0) {
                            visited[i][j] = true;
                            updateSW = true;
                        }
                    }
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
                if (rc.getType() == RobotType.POLITICIAN &&
                    rc.getLocation().distanceSquaredTo(coord) <= RobotType.POLITICIAN.sensorRadiusSquared) {
                    defendSlanderer = true;
                    enemyMuck = coord;
                }
                break;
            case SLANDERER:
                boolean inserted = false;
                for (int i = 0; i < 6; i++) {
                    if (slandererLoc[i] == null || slandererLoc[i].isWithinDistanceSquared(coord, 9)) {
                        slandererLoc[i] = coord;
                        staleness[i] = staleCooldown;
                        inserted = true;
                        break;
                    }
                }
                if (!inserted) {
                    int minStale = 1000;
                    int minStaleInd = -1;
                    for (int i = 0; i < 6; i++) {
                        if (staleness[i] < minStale) {
                            minStale = staleness[i];
                            minStaleInd = i;
                        }
                    }
                    slandererLoc[minStaleInd] = coord;
                    staleness[minStaleInd] = staleCooldown;
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
        updateNE = false;
        updateNW = false;
        updateSE = false;
        updateSW = false;
    }
}