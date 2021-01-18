package feeder;

import battlecode.common.*;

public class Muckraker extends Robot {


    private MapLocation wandLoc;

    public Muckraker(RobotController rc) {
        super(rc);
    }

    public void takeTurn() throws GameActionException {
        super.takeTurn();
        Team enemy = rc.getTeam().opponent();
        int actionRadius = rc.getType().actionRadiusSquared;
        int closestPoliticianDist = 100000;
        MapLocation closestPolitician = null;
        int closestSlandererDist = 100000;
        MapLocation closestSlanderer = null;
        int maxSlanderer = -1;
        for (RobotInfo robot : rc.senseNearbyRobots(rc.getType().sensorRadiusSquared, enemy)) {
            if (robot.type.canBeExposed()) {
                int dist = rc.getLocation().distanceSquaredTo(robot.getLocation());
                if (dist <= closestSlandererDist) {
                    closestSlandererDist = dist;
                    closestSlanderer = robot.location;
                }
                if (dist <= RobotType.MUCKRAKER.actionRadiusSquared) {
                    maxSlanderer = Math.max(maxSlanderer, robot.getInfluence());
                }
            }
            if (robot.type == RobotType.POLITICIAN) {
                int dist = rc.getLocation().distanceSquaredTo(robot.getLocation());
                if (dist > 2 && dist <= closestPoliticianDist) {
                    closestPoliticianDist = dist;
                    closestPolitician = robot.location;
                }
            }
        }
        // expose the max slanderer in range
        if (maxSlanderer != -1) {
            for (RobotInfo robot : rc.senseNearbyRobots(actionRadius, enemy)) {
                if (robot.type.canBeExposed() & rc.canExpose(robot.location)) {
                    rc.expose(robot.location);
                }
            }
        }
        // move to the closest slanderer if not
        if (closestSlandererDist != 100000) {
            nav.goTo(closestSlanderer);
        } else {
            // else move to the nearest politician not adjacent
            if (closestPolitician != null) nav.goTo(closestPolitician);
            // otherwise wander
            else wander();
        }
    }

    // wander around
    // TODO: what if you're already at a corner/side and you want to explore more
    public void wander() throws GameActionException {
        wandLoc = new MapLocation(nav.getEnds()[rc.getID() % 8][0], nav.getEnds()[rc.getID() % 8][1]);
        nav.goTo(wandLoc);
    }
}