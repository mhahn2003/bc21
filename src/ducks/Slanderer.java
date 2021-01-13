package ducks;

import battlecode.common.*;

public class Slanderer extends Politician {

    private MapLocation[] dangers;

    public Slanderer(RobotController rc) {
        super(rc);
    }

    public void takeTurn() throws GameActionException {
        super.takeTurn();
        if (rc.getType() == RobotType.POLITICIAN) return;
        dangers = new MapLocation[10];
        int size = 0;
        // whether the danger variable from coms is included
        boolean included = false;
        int closestPoliticianDist = 100000;
        MapLocation closestPolitician = null;
        for (RobotInfo r : robots) {
            if (r.getTeam() == team.opponent() && r.getType() == RobotType.MUCKRAKER) {
                if (runAway && danger.equals(r.getLocation())) {
                    included = true;
                }
                dangers[size] = r.getLocation();
                size++;
                if (size == 9) break;
            }
            if (r.getTeam() == team && r.getType() == RobotType.POLITICIAN) {
                int dist = rc.getLocation().distanceSquaredTo(r.getLocation());
                if (dist < closestPoliticianDist) {
                    closestPoliticianDist = dist;
                    closestPolitician = r.getLocation();
                }
            }
        }
        if (!included) {
            dangers[size] = danger;
            size++;
        }
        if (size != 0) {
            // in danger
            int maxDist = -10000;
            Direction optDir = null;
            for (int i = 0; i < 8; i++) {
                boolean safe = true;
                MapLocation loc = rc.getLocation().add(directions[i]);
                for (int j = 0; j < size; j++) {
                    if (loc.isWithinDistanceSquared(dangers[j], RobotType.MUCKRAKER.actionRadiusSquared)) {
                        safe = false;
                        break;
                    }
                }
                if (safe) {
                    int dangerH = 0;
                    for (int j = 0; j < size; j++) {
                        dangerH += loc.distanceSquaredTo(dangers[j]);
                    }
                    if (closestPolitician != null) {
                        dangerH -= loc.distanceSquaredTo(closestPolitician)/2;
                    }
                    if (dangerH > maxDist && rc.canMove(directions[i])) {
                        maxDist = dangerH;
                        optDir = directions[i];
                    }
                }
            }
            if (optDir != null) rc.move(optDir);
        } else {
            int closestECDist = 1000000;
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
            if (closestEC != null) {
                patrol(closestEC, 12);
            } else {
                // go to an edge maybe?
            }
        }
    }
}