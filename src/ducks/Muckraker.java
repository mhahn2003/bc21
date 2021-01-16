package ducks;

import battlecode.common.*;

public class Muckraker extends Robot {


    public Muckraker(RobotController rc) {
        super(rc);
    }

    public void takeTurn() throws GameActionException {
        super.takeTurn();
        int closestPoliticianDist = 100000;
        MapLocation closestPolitician = null;
        int closestSlandererDist = 100000;
        MapLocation closestSlanderer = null;
        // friendly slanderer
        int closestProtectorDist = 100000;
        MapLocation closestProtector = null;
        int maxSlanderer = -1;
        MapLocation maxSlandererLocation = null;
        for (RobotInfo robot : robots) {
            if (robot.getTeam() == enemy && robot.type.canBeExposed()) {
                int dist = rc.getLocation().distanceSquaredTo(robot.getLocation());
                if (dist <= closestSlandererDist) {
                    closestSlandererDist = dist;
                    closestSlanderer = robot.location;
                }
                if (dist <= RobotType.MUCKRAKER.actionRadiusSquared) {
                    maxSlanderer = Math.max(maxSlanderer, robot.getInfluence());
                    maxSlandererLocation = robot.location;
                }
            }
            if (robot.getTeam() == enemy && robot.type == RobotType.POLITICIAN) {
                int dist = rc.getLocation().distanceSquaredTo(robot.getLocation());
                if (dist > 2 && dist <= closestPoliticianDist) {
                    closestPoliticianDist = dist;
                    closestPolitician = robot.location;
                }
            }
            if (robot.getTeam() == team && robot.type == RobotType.SLANDERER) {
                int dist = rc.getLocation().distanceSquaredTo(robot.getLocation());
                if (dist <= closestProtectorDist) {
                    closestProtectorDist = dist;
                    closestProtector = robot.location;
                }
            }
        }
        if (rc.isReady()) {
            // expose the max slanderer in range
            if (maxSlanderer != -1) {
                if (rc.senseNearbyRobots(maxSlandererLocation, 1, enemy).length > 0 & rc.canExpose(maxSlandererLocation)) {
                    rc.expose(maxSlandererLocation);
                }
            }
            int closestECDist = 100000;
            MapLocation closestEC = null;
            for (int i = 0; i < 12; i++) {
                if (enemyECs[i] != null) {
                    int dist = rc.getLocation().distanceSquaredTo(enemyECs[i]);
                    if (dist < closestECDist) {
                        closestECDist = dist;
                        closestEC = enemyECs[i];
                    }
                }
            }
            // move to the closest slanderer
            if (closestSlanderer != null) {
                nav.bugNavigate(closestSlanderer);
            }
            else if (closestPolitician != null) {
                int muckCount = 0;
                RobotInfo[] near = rc.senseNearbyRobots(closestPolitician, 2, team);
                for (RobotInfo r : near) {
                    if (r.getType() == RobotType.MUCKRAKER) {
                        muckCount++;
                    }
                }
                if (muckCount <= 2) nav.bugNavigate(closestPolitician);
            }
            else wander();
            // else move to protect our slanderer
//            if (closestProtectorDist <= 8) {
//                boolean protect = true;
//                // first check if anybody else is on the slanderer
//                RobotInfo[] surroundSlanderer = rc.senseNearbyRobots(closestProtector, 2, team);
//                for (RobotInfo r : surroundSlanderer) {
//                    if (r.getType() == RobotType.MUCKRAKER && r.getTeam() == team) {
//                        protect = false;
//                        break;
//                    }
//                }
//                if (protect) {
//                    int closestDist = rc.getLocation().distanceSquaredTo(closestProtector);
//                    Direction optDir = null;
//                    for (int i = 0; i < 8; i++) {
//                        int dist = rc.getLocation().add(directions[i]).distanceSquaredTo(closestProtector);
//                        if (dist < closestDist && rc.canMove(directions[i])) {
//                            closestDist = dist;
//                            optDir = directions[i];
//                        }
//                    }
//                    if (optDir != null) rc.move(optDir);
//                }
//            }
//            // else move to the closest enemy HQ if known
//            if (closestEC != null) {
//                if (!rc.getLocation().isAdjacentTo(closestEC)) {
//                    if (rc.getLocation().isWithinDistanceSquared(closestEC, 13)) {
//                        // move to an adjacent spot
//                        int closestSpotDist = 100000;
//                        MapLocation closestSpot = null;
//                        for (Direction dir : directions) {
//                            MapLocation loc = closestEC.add(dir);
//                            if (!rc.isLocationOccupied(loc)) {
//                                int dist = rc.getLocation().distanceSquaredTo(loc);
//                                if (dist < closestSpotDist) {
//                                    closestSpotDist = dist;
//                                    closestSpot = loc;
//                                }
//                            }
//                        }
//                        if (closestSpot != null) {
//                            nav.bugNavigate(closestSpot);
//                        } else {
//                            // TODO: call for attack
//                        }
//                    } else nav.bugNavigate(closestEC);
//                }
//            } else {
            // else move to the nearest politician not adjacent

        }
    }
}