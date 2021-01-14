package ducks;

import battlecode.common.*;
import ducks.utils.Debug;

public class Politician extends Robot {

    // whether it's an aggressive or defensive politician
    private static boolean attack = false;
    private static int[] attackRadii = {1, 2, 4, 5, 8, 9};
    private static int effThreshold = 50;
    private static int effect;


    public Politician(RobotController rc) {
        super(rc);
        if (rc.getInfluence() >= 50) attack = true;
    }

    public void takeTurn() throws GameActionException {
        super.takeTurn();
        if (rc.getType() == RobotType.SLANDERER) return;
        if (rc.getConviction() >= 300) attack();
        else {
            if (rc.getRoundNum() <= 200) {
                if (rc.getID() % 4 == 0) search();
                else defend();
            } else if (rc.getRoundNum() <= 400) {
                if (rc.getID() % 4 == 0) attack();
                else defend();
            } else {
                if (rc.getID() % 2 == 0) attack();
                else defend();
            }
        }
    }

    // attack on the enemy ec
    static void attack() throws GameActionException {
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
        for (int i = 0; i < 12; i++) {
            if (neutralECs[i] != null) {
                int dist = rc.getLocation().distanceSquaredTo(neutralECs[i]);
                if (dist < closestECDist) {
                    closestECDist = dist;
                    closestEC = neutralECs[i];
                }
            }
        }
        if (closestEC != null) {
            Debug.p("Going to closest EC: " + closestEC);
            if (closestECDist <= 9) {
                Debug.p("Within empower distance");
                // check if can kill
                RobotInfo[] empowered = rc.senseNearbyRobots(closestECDist);
                int size = empowered.length;
                int effect = ((int) ((double) rc.getConviction() * rc.getEmpowerFactor(team, 0)) - 10)/size;
                RobotInfo enemyEC = rc.senseRobotAtLocation(closestEC);
                if (enemyEC.getConviction()+1 <= effect) {
                    Debug.p("Can kill, will kill");
                    if (rc.canEmpower(closestECDist)) rc.empower(closestECDist);
                } else {
                    // signal that you're attacking, so move out of the way
                    // only signal if you're a fat politician
                    if (rc.getConviction() >= 300) {
                        Debug.p("Signalling attack");
                        rc.setFlag(Coms.getMessage(Coms.IC.ATTACK, closestEC));
                    }
                    // check if we can get closer, or if there's a lot of our own units in the way
                    int closerDist = rc.getLocation().distanceSquaredTo(closestEC);
                    Direction optDir = null;
                    for (int i = 0; i < 8; i++) {
                        int dist = rc.getLocation().add(directions[i]).distanceSquaredTo(closestEC);
                        if (dist < closerDist && rc.canMove(directions[i])) {
                            closerDist = dist;
                            optDir = directions[i];
                        }
                    }
                    if (optDir != null) {
                        Debug.p("The optimal direction to move is: " + optDir);
                        rc.move(optDir);
                    }
                    else {
                        // if can't move, then try to see whether it's good to just blast away
                        int teamCount = 0;
                        for (RobotInfo r : empowered) {
                            if (r.getTeam() == team) teamCount++;
                        }
                        Debug.p("There's teammates around me: " + teamCount);
//                        int eff = attackEffect(closestECDist)[0];
//                        Debug.p("Efficiency: " + eff);
                        if (attackEffect(closestECDist)[1] > 2) {
                            Debug.p("Can't kill, kamikaze time");
                            if (rc.canEmpower(closestECDist)) rc.empower(closestECDist);
                        } else {
                            // wait for others to move
                            // maybe ask for assist?
                        }
                    }
                }
            } else {
                Debug.p("navbugging");
                nav.bugNavigate(closestEC);
            }
        } else {
            // discuss: what if there's no known ECs?
            // do you also try to explore, or do you stay put until?
            // for now, just stay put, just don't take up a spot next to an EC
            wander();
//            RobotInfo[] adjacentRobots = rc.senseNearbyRobots(2);
//            for (RobotInfo r : adjacentRobots) {
//                if (r.getType() == RobotType.ENLIGHTENMENT_CENTER && r.getTeam() == team) {
//                    // move away from it
//                    Direction opp = rc.getLocation().directionTo(r.getLocation()).opposite();
//                    if (rc.canMove(opp)) rc.move(opp);
//                }
//            }
//            if (friendECs[0] != null) patrol(friendECs[0]);
        }
    }

    // search for nearby cheap neutral ECs
    static void search() throws GameActionException {
        int closestECDist = 100000;
        MapLocation closestEC = null;
        for (int i = 0; i < 12; i++) {
            if (neutralECs[i] != null) {
                int dist = rc.getLocation().distanceSquaredTo(neutralECs[i]);
                if (dist < closestECDist) {
                    closestECDist = dist;
                    closestEC = neutralECs[i];
                }
            }
        }
        if (closestEC != null) {
            if (closestECDist <= 9) {
                Debug.p("Within empower distance");
                // check if can kill
                RobotInfo[] empowered = rc.senseNearbyRobots(closestECDist);
                int size = empowered.length;
                int effect = ((int) ((double) rc.getConviction() * rc.getEmpowerFactor(team, 0)) - 10) / size;
                RobotInfo enemyEC = rc.senseRobotAtLocation(closestEC);
                if (enemyEC.getConviction() + 1 <= effect) {
                    Debug.p("Can kill, will kill");
                    if (rc.canEmpower(closestECDist)) rc.empower(closestECDist);
                } else {
                    // signal that you're attacking, so move out of the way
                    Debug.p("Signalling attack");
                    rc.setFlag(Coms.getMessage(Coms.IC.ATTACK, closestEC));
                    // check if we can get closer, or if there's a lot of our own units in the way
                    int closerDist = rc.getLocation().distanceSquaredTo(closestEC);
                    Direction optDir = null;
                    for (int i = 0; i < 8; i++) {
                        int dist = rc.getLocation().add(directions[i]).distanceSquaredTo(closestEC);
                        if (dist < closerDist && rc.canMove(directions[i])) {
                            closerDist = dist;
                            optDir = directions[i];
                        }
                    }
                    if (optDir != null) {
                        Debug.p("The optimal direction to move is: " + optDir);
                        rc.move(optDir);
                    }
                }
            } else {
                Debug.p("navbugging");
                nav.bugNavigate(closestEC);
            }
        } else wander();
    }

    static void defend() throws GameActionException {
        // check if it should just explode
        int maxEff = 0;
        int maxEffRadius = 0;
        int maxKill = 0;
        int maxRadius = 0;
        for (int i = 0; i < 6; i++) {
            int radius = attackRadii[i];
            int[] att = attackEffect(radius);
            int eff = att[0];
            int kill = att[1];
            if (kill > maxKill) {
                maxKill = kill;
                maxRadius = radius;
            }
            if (eff > maxEff) {
                maxEff = eff;
                maxEffRadius = radius;
            }
        }
        if (maxKill >= 2) {
            if (rc.canEmpower(maxRadius)) rc.empower(maxRadius);
        } else {
            // check if any slanderers are in danger
            if (defendSlanderer) {
                // search for muckraker, and also for other politicians near
                boolean defended = false;
                RobotInfo muck = null;
                for (RobotInfo r : robots) {
                    if (r.getType() == RobotType.MUCKRAKER &&
                        r.getTeam() == team.opponent() &&
                        (r.getLocation().isAdjacentTo(enemyMuck) || r.getLocation().equals(enemyMuck))) {
                        muck = r;
                    }
                }
                // search for other friendly politicians near
                RobotInfo[] near = rc.senseNearbyRobots(enemyMuck, 2, team);
                for (RobotInfo r : near) {
                    if (r.getType() == RobotType.POLITICIAN) {
                        defended = true;
                        break;
                    }
                }
                if (!defended) {
                    int locDist = rc.getLocation().distanceSquaredTo(enemyMuck);
                    attackEffect(locDist);
                    // either blow up the muckraker, or go closer to it
                    if (muck != null && effect >= muck.getConviction() + 1) {
                        if (locDist <= RobotType.POLITICIAN.actionRadiusSquared) {
                            if (rc.canEmpower(locDist)) rc.empower(locDist);
                        }
                    } else if (muck != null) nav.bugNavigate(muck.getLocation());
                    else nav.bugNavigate(enemyMuck);
                }
            }
            if (rc.isReady()) {
                // check if empowering right now is pretty efficient
                if (maxEff >= 25) {
                    if (rc.canEmpower(maxEffRadius)) rc.empower(maxEffRadius);
                } else {
                    // otherwise, chase nearby muckrakers and politicians
                    int closestMuckDist = 100000;
                    MapLocation closestMuck = null;
                    int closestPoliticianDist = 100000;
                    MapLocation closestPolitician = null;
                    for (RobotInfo r : robots) {
                        if (r.getTeam() == team.opponent() && r.getType() == RobotType.MUCKRAKER) {
                            int dist = rc.getLocation().distanceSquaredTo(r.getLocation());
                            if (dist < closestMuckDist) {
                                closestMuckDist = dist;
                                closestMuck = r.getLocation();
                            }
                        }
                        if (r.getTeam() == team.opponent() && r.getType() == RobotType.POLITICIAN) {
                            int dist = rc.getLocation().distanceSquaredTo(r.getLocation());
                            if (dist < closestPoliticianDist) {
                                closestPoliticianDist = dist;
                                closestPolitician = r.getLocation();
                            }
                        }
                    }
                    if (closestMuck != null) {
                        boolean defended = false;
                        RobotInfo[] near = rc.senseNearbyRobots(closestMuck, 2, team);
                        for (RobotInfo r : near) {
                            if (Coms.getTyp(rc.getFlag(r.getID())) == RobotType.POLITICIAN) {
                                defended = true;
                                break;
                            }
                        }
                        if (!defended) nav.bugNavigate(closestMuck);
                    }
                    if (rc.isReady()) {
                        // stick to nearby politicians
                        if (closestPolitician != null) {
                            boolean defended = false;
                            RobotInfo[] near = rc.senseNearbyRobots(closestPolitician, 2, team);
                            for (RobotInfo r : near) {
                                if (Coms.getTyp(rc.getFlag(r.getID())) == RobotType.POLITICIAN) {
                                    defended = true;
                                    break;
                                }
                            }
                            if (!defended) nav.bugNavigate(closestPolitician);
                        } else {
                            // if there's nothing, just patrol around HQ
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
                            if (closestEC != null) patrol(closestEC, 25, 45);
                            else wander();
                        }
                    }
                }
            }
        }
    }

    // calculates the efficiency of the attack
    static int[] attackEffect(int radius) {
        RobotInfo[] empowered = rc.senseNearbyRobots(radius);
        int size = empowered.length;
        if (size == 0 || rc.getConviction() == 0) return new int[] {0, 0};
        effect = ((int) ((double) rc.getConviction() * rc.getEmpowerFactor(team, 0)) - 10)/size;
        int eff = 0;
        int killCount = 0;
        for (RobotInfo r : empowered) {
            if (r.getTeam() == team.opponent()) {
                if (r.getConviction()+1 <= effect) {
                    eff += r.getConviction()+1;
                    killCount++;
                }
                else eff += effect;
            }
        }
        return new int[] {(eff*100)/rc.getConviction(), killCount};
    }
}
