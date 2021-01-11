package coms;

import battlecode.common.*;

public class Politician extends Robot {

    // whether it's an aggressive or defensive politician
    private static boolean attack = false;
    private static int[] attackRadii = {1, 2, 4, 5, 8, 9};
    private static int effThreshold = 50;


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
            if (closestECDist <= 9) {
                // check if can kill
                RobotInfo[] empowered = rc.senseNearbyRobots(closestECDist);
                int size = empowered.length;
                int effect = ((int) ((double) rc.getConviction() * rc.getEmpowerFactor(team, 0)) - 10)/size;
                RobotInfo enemyEC = rc.senseRobotAtLocation(closestEC);
                if (enemyEC.getConviction()+1 <= effect) {
                    if (rc.canEmpower(closestECDist)) rc.empower(closestECDist);
                } else {
                    // signal that you're attacking, so move out of the way
                    rc.setFlag(Coms.getMessage(Coms.InformationCategory.ATTACK, closestEC));
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
                    if (optDir != null) rc.move(optDir);
                    else {
                        // if can't move, then try to see whether it's good to just blast away
                        int teamCount = 0;
                        for (RobotInfo r : empowered) {
                            if (r.getTeam() == team) teamCount++;
                        }
                        if (teamCount <= 2 && attackEffect(closestECDist) > effThreshold) {
                            if (rc.canEmpower(closestECDist)) rc.empower(closestECDist);
                        } else {
                            // wait for others to move
                            // maybe ask for assist?
                        }
                    }
                }
            } else {
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
        }
    }

    // assist with the attack by killing any muckrakers/politicians around the EC
    static void assist() {

    }

    static void defend() {

    }

    // calculates the efficiency of the attack
    static int attackEffect(int radius) {
        RobotInfo[] empowered = rc.senseNearbyRobots(radius);
        int size = empowered.length;
        int effect = ((int) ((double) rc.getConviction() * rc.getEmpowerFactor(team, 0)) - 10)/size;
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
