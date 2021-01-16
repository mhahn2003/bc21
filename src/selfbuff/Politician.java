package selfbuff.coms;

import battlecode.common.*;
import selfbuff.coms.utils.Debug;

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
        if (attack) attack();
        else defend();
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
                    else {
                        // if can't move, then try to see whether it's good to just blast away
                        int teamCount = 0;
                        for (RobotInfo r : empowered) {
                            if (r.getTeam() == team) teamCount++;
                        }
                        Debug.p("There's teammates around me: " + teamCount);
                        int eff = attackEffect(closestECDist);
                        Debug.p("Efficiency: " + eff);
                        if (teamCount <= 2 && eff > effThreshold) {
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
            // discuss: what if there's no known enemy ECs?
            // do you also try to explore, or do you stay put until?
            // for now, just stay put, just don't take up a spot next to an EC
            RobotInfo[] adjacentRobots = rc.senseNearbyRobots(2);
            for (RobotInfo r : adjacentRobots) {
                if (r.getType() == RobotType.ENLIGHTENMENT_CENTER && r.getTeam() == team) {
                    // move away from it
                    Direction opp = rc.getLocation().directionTo(r.getLocation()).opposite();
                    if (rc.canMove(opp)) rc.move(opp);
                }
            }
            if (friendECs[0] != null) patrol(friendECs[0]);
        }
    }

    // assist with the attack by killing any muckrakers/politicians around the EC
    static void assist() {

    }

    static void defend() throws GameActionException {
        // check if it should just explode
        int maxEff = 0;
        int maxRadius = 0;
        for (int i = 0; i < 6; i++) {
            int radius = attackRadii[i];
            int eff = attackEffect(radius);
            if (eff > maxEff) {
                maxEff = eff;
                maxRadius = radius;
            }
        }
        if (maxEff >= 25) {
            if (rc.canEmpower(maxRadius)) rc.empower(maxRadius);
        } else {
            // check if any slanderers are in danger
            if (defendSlanderer) {
                // search for muckraker
                RobotInfo muck = null;
                for (RobotInfo r : robots) {
                    if (r.getType() == RobotType.MUCKRAKER &&
                        r.getTeam() == team.opponent() &&
                        (r.getLocation().isAdjacentTo(enemyMuck) || r.getLocation().equals(enemyMuck))) {
                        muck = r;
                        break;
                    }
                }
                int locDist = rc.getLocation().distanceSquaredTo(enemyMuck);
                attackEffect(locDist);
                // either blow up the muckraker, or go closer to it
                if (muck != null && effect >= muck.getConviction()+1) {
                    if (locDist <= RobotType.POLITICIAN.actionRadiusSquared) {
                        if (rc.canEmpower(locDist)) rc.empower(locDist);
                    }
                }
                else if (muck != null) nav.bugNavigate(muck.getLocation());
                else nav.bugNavigate(enemyMuck);
            } else {
                // otherwise, chase nearby muckrakers
                int closestMuckDist = 100000;
                MapLocation closestMuck = null;
                for (RobotInfo r : robots) {
                    if (r.getTeam() == team.opponent() && r.getType() == RobotType.MUCKRAKER) {
                        int dist = rc.getLocation().distanceSquaredTo(r.getLocation());
                        if (dist < closestMuckDist) {
                            closestMuckDist = dist;
                            closestMuck = r.getLocation();
                        }
                    }
                }
                if (closestMuck != null) nav.bugNavigate(closestMuck);
                else {
                    // if there's nothing, just patrol around slanderers
                    int closestSlandererDist = 100000;
                    MapLocation closestSlanderer = null;
                    for (RobotInfo r : robots) {
                        if (r.getTeam() == team) {
                            if (Coms.getTyp(rc.getFlag(r.getID())) == RobotType.SLANDERER) {
                                // slanderer is near
                                int dist = rc.getLocation().distanceSquaredTo(r.getLocation());
                                if (dist < closestSlandererDist) {
                                    closestSlandererDist = dist;
                                    closestSlanderer = r.getLocation();
                                }
                            }
                        }
                    }
                    if (closestSlanderer != null) nav.bugNavigate(closestSlanderer);
                    else wander();
                }
            }
        }
    }

    // calculates the efficiency of the attack
    static int attackEffect(int radius) {
        RobotInfo[] empowered = rc.senseNearbyRobots(radius);
        int size = empowered.length;
        if (size == 0 || rc.getConviction() == 0) return 0;
        effect = ((int) ((double) rc.getConviction() * rc.getEmpowerFactor(team, 0)) - 10)/size;
        int eff = 0;
        for (RobotInfo r : empowered) {
            if (r.getTeam() == team.opponent()) {
                if (r.getConviction()+1 <= effect) eff += r.getConviction()+1;
                else eff += effect;
            }
        }
        return (eff*100)/rc.getConviction();
    }
}
