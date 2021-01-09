package coms;

import battlecode.common.MapLocation;
import battlecode.common.RobotController;

import java.util.LinkedList;
import java.util.Queue;

public class Coms {
    public static RobotController rc;
    private final int senseRadius;
    private final int[] enlightenmentCenterIds = new int[12];
    private final Queue<Integer> signalQueue = new LinkedList<>();

    // number of possible cases for InfoCategory enum class
    private static int numCase = 21;

    public Coms(RobotController r) {
        rc = r;
        senseRadius = rc.getType().sensorRadiusSquared;
    }

    public enum InformationCategory {
        EDGE,
        ENEMY_EC,
        EC,
        NEUTRAL_EC
    }

    public static int getMessage(InformationCategory cat, MapLocation coord) {
        int message = 0;
        switch (cat) {
            case EDGE: message = 1; break;
            case ENEMY_EC: message = 2; break;
            case EC: message = 3; break;
            case NEUTRAL_EC: message = 4; break;
            default: message = 5;
        }
        message = addCoord(message, coord);
        return message;
    }

    public static int addCoord(int message, MapLocation coord) {
        return message*16384+(coord.x % 128)*128+(coord.y % 128);
    }

    public static InformationCategory getCat(int message) {
        switch (message/16384) {
            case 1: return InformationCategory.EDGE;
            case 2: return InformationCategory.ENEMY_EC;
            case 3: return InformationCategory.EC;
            case 4: return InformationCategory.NEUTRAL_EC;
            default: return null;
        }
    }

    public static MapLocation getCoord(int message) {
        MapLocation here = rc.getLocation();
        int remX = here.x % 128;
        int remY = here.y % 128;
        message = message % 16384;
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

}