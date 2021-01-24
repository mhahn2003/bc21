package quals;

import battlecode.common.*;

public class Muckraker extends Robot {


    public Muckraker(RobotController rc) {
        super(rc);
    }

    public void takeTurn() throws GameActionException {
        super.takeTurn();
        int closestPoliticianDist = 100000;
        RobotInfo closestPolitician = null;
        RobotInfo[] nearPoliticians = new RobotInfo[5];
        int nearPolSize = 0;
        int closestSlandererDist = 100000;
        MapLocation closestSlanderer = null;
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
                if (dist <= closestPoliticianDist) {
                    closestPoliticianDist = dist;
                    closestPolitician = robot;
                }
                if (dist <= 13 && nearPolSize < 5) {
                    nearPoliticians[nearPolSize] = robot;
                    nearPolSize++;
                }
            }
        }
        if (rc.isReady()) {
            // expose the max slanderer in range
            if (maxSlanderer != -1) {
                if (rc.canExpose(maxSlandererLocation)) rc.expose(maxSlandererLocation);
            }
            int closestEnemyECDist = 100000;
            MapLocation closestEnemyEC = null;
            for (int i = 0; i < 12; i++) {
                if (enemyECs[i] != null) {
                    int dist = rc.getLocation().distanceSquaredTo(enemyECs[i]);
                    if (dist < closestEnemyECDist) {
                        closestEnemyECDist = dist;
                        closestEnemyEC = enemyECs[i];
                    }
                }
            }
            int closestECDist = 100000;
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
            // move to the closest slanderer
            if (closestSlanderer != null) {
                nav.bugNavigate(closestSlanderer);
            }
            // are we defending or attacking?
            if (closestEC != null && rc.getLocation().isWithinDistanceSquared(closestEC, 50)) {
                // defending
                if (closestPolitician == null) {
                    // just go outside and move to attack mode
                    MapLocation loc;
                    if (mapGenerated || closestEnemyEC == null) {
                        loc = wander();
                    } else loc = closestEnemyEC;
                    nav.bugNavigate(loc);
                } else {
                    if (nearPolSize == 0) {
                        // check if it's already surrounded by muckrakers tagging that thing
                        boolean tagged = false;
                        RobotInfo[] near = rc.senseNearbyRobots(closestPolitician.getLocation(), 9, team);
                        for (RobotInfo r : near) {
                            if (r.getType() == RobotType.MUCKRAKER &&
                                Coms.getCat(rc.getFlag(r.getID())) == Coms.IC.POLITICIAN &&
                                closestPolitician.getID() == Coms.getID(rc.getFlag(r.getID()))) {
                                tagged = true;
                                break;
                            }
                        }
                        if (!tagged) {
                            nav.bugNavigate(closestPolitician.getLocation());
                        } else {
                            MapLocation loc;
                            if (mapGenerated || closestEnemyEC == null) {
                                loc = wander();
                            } else loc = closestEnemyEC;
                            nav.bugNavigate(loc);
                        }
                    } else {
                        // check if it's already surrounded by muckrakers tagging that thing
                        boolean needed = false;
                        for (int i = 0; i < nearPolSize; i++) {
                            int tagCount = 0;
                            MapLocation polLoc = nearPoliticians[i].getLocation();
                            RobotInfo[] near = rc.senseNearbyRobots(polLoc, 9, team);
                            for (RobotInfo r : near) {
                                if (r.getType() == RobotType.MUCKRAKER &&
                                    Coms.getCat(rc.getFlag(r.getID())) == Coms.IC.POLITICIAN &&
                                    nearPoliticians[i].getID() == Coms.getID(rc.getFlag(r.getID()))) {
                                    tagCount++;
                                }
                            }
                            if (tagCount < 2) {
                                // move towards that politician
                                rc.setFlag(Coms.getMessage(Coms.IC.POLITICIAN, nearPoliticians[i].getID()));
                                if (!rc.getLocation().isWithinDistanceSquared(polLoc, 1)) {
                                    // TODO: optimize movement?
                                    nav.bugNavigate(polLoc);
                                }
                                needed = true;
                                break;
                            }
                        }
                        if (!needed) {
                            // if not needed, just do your own thing
                            // just go outside and move to attack mode
                            MapLocation loc;
                            if (mapGenerated || closestEnemyEC == null) {
                                loc = wander();
                            } else loc = closestEnemyEC;
                            nav.bugNavigate(loc);
                        }
                    }
                }
            }
            else {
                // attacking, searching for slanderers
                MapLocation suspectSlanderer = null;
                int maxStale = -50;
                for (int i = 0; i < 6; i++) {
                    if (slandererLoc[i] != null) {
                        // if too close, update staleness and move on
                        if (slandererLoc[i].isWithinDistanceSquared(rc.getLocation(), 4)) staleness[i] = -50;
                        else if (staleness[i] > maxStale) {
                            maxStale = staleness[i];
                            suspectSlanderer = slandererLoc[i];
                        }
                    }
                }
                MapLocation loc;
                if (suspectSlanderer != null) loc = suspectSlanderer;
                else {
                    // there's no suspect location, go to nearby ecs? or wander
                    if (mapGenerated || closestEnemyEC == null) {
                        loc = wander();
                    } else loc = closestEnemyEC;
                }
                // loc is our destination
                if (nearPolSize != 0) {
                    // TODO: not really sure if this piece of code works, maybe fix later?
                    // politician is near
                    int[] optDirH = new int[8];
                    for (int i = 0; i < nearPolSize; i++) {
                        MapLocation polLoc = nearPoliticians[i].getLocation();
                        int teamSize = rc.senseNearbyRobots(polLoc, 9, team).length;
                        if (teamSize > 0) {
                            for (int j = 0; j < 8; j++) {
                                if (rc.getLocation().add(directions[i]).isWithinDistanceSquared(polLoc, 9)) {
                                    optDirH[j] = Math.max(optDirH[j], teamSize*100);
                                }
                            }
                        }
                    }
                    Direction optDir = null;
                    int minH = 10000;
                    for (int i = 0; i < 8; i++) {
                        MapLocation adj = rc.getLocation().add(directions[i]);
                        optDirH[i] += adj.distanceSquaredTo(loc);
                        if (rc.canSenseLocation(adj)) optDirH[i] += 2*((int) (1.0/rc.sensePassability(adj)));
                        if (optDirH[i] < minH && rc.canMove(directions[i])) {
                            minH = optDirH[i];
                            optDir = directions[i];
                        }
                    }
                    if (optDir != null) rc.move(optDir);
                } else {
                    nav.bugNavigate(loc);
                }
            }
        }
    }
}