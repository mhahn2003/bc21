package coms;

import battlecode.common.*;

import java.util.PriorityQueue;

import static coms.Robot.*;

public class Coms {
    private final int senseRadius;
    private final int[] enlightenmentCenterIds = new int[12];
    private final PriorityQueue<Integer> signalQueue = new PriorityQueue<>();

    // number of possible cases for InfoCategory enum class
    private static int numCase = 4;

    public Coms() {
        senseRadius = rc.getType().sensorRadiusSquared;
    }

    // TODO: need to order in terms of priority
    public enum InformationCategory {
        EDGE,
        ENEMY_EC,
        FRIEND_EC,
        NEUTRAL_EC
    }

    public static int getMessage(InformationCategory cat, MapLocation coord) {
        System.out.println(cat.toString() + " " + coord.toString());
        int message;
        switch (cat) {
            case EDGE      : message = 1; break;
            case ENEMY_EC  : message = 2; break;
            case FRIEND_EC : message = 3; break;
            case NEUTRAL_EC: message = 4; break;
            default        : message = 5;
        }
        message = addCoord(message, coord);
        return message;
    }

    public static int addCoord(int message, MapLocation coord) {
        return (message<<14)+((coord.x % 128)<<7)+(coord.y % 128);
    }

    public static InformationCategory getCat(int message) {
        switch (message>>14) {
            case 1: return InformationCategory.EDGE;
            case 2: return InformationCategory.ENEMY_EC;
            case 3: return InformationCategory.FRIEND_EC;
            case 4: return InformationCategory.NEUTRAL_EC;
            default: return null;
        }
    }

    public static MapLocation getCoord(int message) {
        MapLocation here = rc.getLocation();
        int remX = here.x % 128;
        int remY = here.y % 128;
        message = message % 16384;
        int x = message >> 7;
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
            if (edgesDetected[i]){continue;}
            Direction dir = Direction.cardinalDirections()[i];
            MapLocation checkLoc = rc.getLocation().add(dir);
            while (checkLoc.isWithinDistanceSquared(rc.getLocation(), rc.getType().sensorRadiusSquared)) {
                if (!rc.onTheMap(checkLoc)) {
                    System.out.println("I see an edge");
                    edgesDetected[i] = true;
                    if (i == 0) {
                        maxY = checkLoc.y-1;
                        edgesValue[i]=maxY;
                        signalQueue.add(getMessage(InformationCategory.EDGE, new MapLocation(30001, maxY)));
                    }
                    if (i == 1) {
                        maxX = checkLoc.x-1;
                        edgesValue[i]=maxX;
                        signalQueue.add(getMessage(InformationCategory.EDGE, new MapLocation(maxX, 30001)));
                    }
                    if (i == 2) {
                        minY = checkLoc.y+1;
                        edgesValue[i]=minY;
                        signalQueue.add(getMessage(InformationCategory.EDGE, new MapLocation(9999, minY)));
                    }
                    if (i == 3) {
                        minX = checkLoc.x+1;
                        edgesValue[i]=minX;
                        signalQueue.add(getMessage(InformationCategory.EDGE, new MapLocation(minX, 9999)));
                    }
                    break;
                }
                checkLoc = checkLoc.add(dir);
            }
        }
        // check for any ECs
        // todo: allow for ecs to switch teams (maybe when trying to read the flag of all ecs?)
        RobotInfo[] robots = rc.senseNearbyRobots();
        for (RobotInfo r: robots) {
            if (r.getType() == RobotType.ENLIGHTENMENT_CENTER) {
                int id = r.getID();
                if (!ECLoc.containsKey(r.getID())) {
                    // discuss: are you sure you want to pass the location instead of id?
                    MapLocation loc = r.getLocation();
                    ECLoc.put(id, loc);
                    if (r.getTeam() == team) {
                        friendECs.add(id);
                        signalQueue.add(getMessage(InformationCategory.FRIEND_EC, loc));
                    }
                    else if (r.getTeam() == team.opponent()) {
                        enemyECs.add(id);
                        signalQueue.add(getMessage(InformationCategory.ENEMY_EC, loc));
                    }
                    else {
                        neutralECs.add(id);
                        signalQueue.add(getMessage(InformationCategory.NEUTRAL_EC, loc));
                    }
                }
            }
        }
        System.out.println(signalQueue.toString());
    }

    // get information from flags
    public void getInfo(int flag) throws GameActionException {
        InformationCategory cat = getCat(flag);
        if (cat==null){return;}
        MapLocation loc =getCoord(flag);
        switch (cat){
            case EDGE:
                if      (loc.x==30001 & !edgesDetected[0]){edgesValue[0]=loc.y;}
                else if (loc.y==30001 & !edgesDetected[1]){edgesValue[1]=loc.x;}
                else if (loc.x== 9999 & !edgesDetected[2]){edgesValue[2]=loc.y;}
                else if (loc.y== 9999 & !edgesDetected[3]){edgesValue[3]=loc.x;}
            /*
            case   ENEMY_EC:   enemyECs.add(id); break;
            case  FRIEND_EC:  friendECs.add(id); break;
            case NEUTRAL_EC: neutralECs.add(id); break;
             */
            default: break;

        }
    }

    public void displaySignal() throws GameActionException {
        if (signalQueue.size()==0){
            //DISCUSS: is this a good idea to clear stuff in the next turn?
            rc.setFlag(0);
            return;
        }
        int signal=signalQueue.remove();
        if(rc.canSetFlag(signal)){
            rc.setFlag(signal);
        }
    }

}