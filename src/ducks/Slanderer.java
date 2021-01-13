package ducks;

import battlecode.common.*;

public class Slanderer extends Robot {

    private MapLocation[] dangers;

    public Slanderer(RobotController rc) {
        super(rc);
    }

    public void takeTurn() throws GameActionException {
        super.takeTurn();
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
                        // going closer to a politician is more important, which is why the 2 is there
                        // can modify constant later
                        dangerH -= 2 * loc.distanceSquaredTo(closestPolitician);
                    }
                    if (dangerH > maxDist && rc.canMove(directions[i])) {
                        maxDist = dangerH;
                        optDir = directions[i];
                    }
                }
            }
            if (optDir != null) rc.move(optDir);
        } else {
            if (friendECs[0] != null) {
                patrol(friendECs[0]);
            }
        }
    }
}