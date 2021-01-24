package quals;

import battlecode.common.*;
import quals.utils.Debug;

public class Politician extends Robot {

    private static int[] attackRadii = {1, 2, 4, 5, 8, 9};
    private static int effect;


    public Politician(RobotController rc) {
        super(rc);
    }

    public void takeTurn() throws GameActionException {
        super.takeTurn();
        if (rc.getType() == RobotType.SLANDERER) return;
        // self destruct if kinda useless
        if (rc.getConviction() <= 10) {
            if (rc.canEmpower(1)) rc.empower(1);
        }
        if (rc.getConviction() >= 300) attack();
        else {
            if (rc.getRoundNum() <= 400) {
                if (rc.getInfluence() >= 50) attack();
                else defend();
            } else defend();
        }
    }

    // attack on the enemy ec
    static void attack() throws GameActionException {
        int closestECDist = 100000;
        MapLocation closestEC = null;
        int closestNeutralDist = 100000;
        MapLocation closestNeutral = null;
        for (int i = 0; i < 12; i++) {
            if (neutralECs[i] != null) {
                int dist = rc.getLocation().distanceSquaredTo(neutralECs[i]);
                if (dist < closestECDist) {
                    closestECDist = dist;
                    closestEC = neutralECs[i];
                }
                if (dist < closestNeutralDist && neutralInf[i]*70+80 <= rc.getInfluence()) {
                    closestNeutralDist = dist;
                    closestNeutral = neutralECs[i];
                }
            }
        }
        for (int i = 0; i < 12; i++) {
            if (enemyECs[i] != null) {
                int dist = rc.getLocation().distanceSquaredTo(enemyECs[i]);
                if (dist < closestECDist) {
                    closestECDist = dist;
                    closestEC = enemyECs[i];
                }
            }
        }
        int closestBuffMuckDist = 100000;
        RobotInfo closestBuffMuck = null;
        for (RobotInfo r : robots) {
            if (r.getTeam() == team.opponent() && r.getType() == RobotType.MUCKRAKER && r.getConviction() >= 150) {
                int dist = rc.getLocation().distanceSquaredTo(r.getLocation());
                if (dist < closestBuffMuckDist) {
                    closestBuffMuckDist = dist;
                    closestBuffMuck = r;
                }
            }
        }
        if (closestBuffMuck != null) {
            Debug.p("Going to closest buffraker: " + closestBuffMuck);
            if (closestBuffMuckDist <= 9) {
                Debug.p("Within empower distance");
                // check if can kill
                RobotInfo[] empowered = rc.senseNearbyRobots(closestBuffMuckDist);
                int size = empowered.length;
                int effect = ((int) ((rc.getConviction() - 10) * rc.getEmpowerFactor(team, 0)))/size;
                if (closestBuffMuck.getConviction()+1 <= effect) {
                    Debug.p("Can kill, will kill");
                    if (rc.canEmpower(closestBuffMuckDist)) rc.empower(closestBuffMuckDist);
                } else {
                    int closerDist = closestBuffMuckDist;
                    Direction optDir = null;
                    for (int i = 0; i < 8; i++) {
                        int dist = rc.getLocation().add(directions[i]).distanceSquaredTo(closestBuffMuck.getLocation());
                        if (dist < closerDist && rc.canMove(directions[i])) {
                            closerDist = dist;
                            optDir = directions[i];
                        }
                    }
                    if (optDir != null) {
                        Debug.p("The optimal direction to move is: " + optDir);
                        rc.move(optDir);
                    } else {
                        // if can't move, then try to see whether it's good to just blast away
                        if (attackEffect(closestBuffMuckDist)[0] > 20) {
                            Debug.p("Can't kill, kamikaze time");
                            if (rc.canEmpower(closestBuffMuckDist)) rc.empower(closestBuffMuckDist);
                        }
                    }
                }
            } else {
                Debug.p("navbugging");
                nav.bugNavigate(closestBuffMuck.getLocation());
            }
        }
        else if (closestNeutral != null) {
            // should be able to kill if there's no units beside it
            Debug.p("Going to closest neutral EC: " + closestNeutral);
            if (closestNeutralDist <= 9) {
                Debug.p("Within empower distance");
                // check if can kill
                RobotInfo[] empowered = rc.senseNearbyRobots(closestNeutralDist);
                int size = empowered.length;
                int effect = ((int) ((rc.getConviction() - 10) * rc.getEmpowerFactor(team, 0)))/size;;
                RobotInfo neutralEC = rc.senseRobotAtLocation(closestNeutral);
                if (neutralEC.getConviction()+1 <= effect) {
                    Debug.p("Can kill, will kill");
                    if (rc.canEmpower(closestNeutralDist)) rc.empower(closestNeutralDist);
                } else {
                    if (!moveAway) {
                        Debug.p("Signalling attack");
                        rc.setFlag(Coms.getMessage(Coms.IC.ATTACK, closestNeutral));
                    }
                    int closerDist = rc.getLocation().distanceSquaredTo(closestNeutral);
                    Direction optDir = null;
                    for (int i = 0; i < 8; i++) {
                        int dist = rc.getLocation().add(directions[i]).distanceSquaredTo(closestNeutral);
                        if (dist < closerDist && rc.canMove(directions[i])) {
                            closerDist = dist;
                            optDir = directions[i];
                        }
                    }
                    if (optDir != null) {
                        Debug.p("The optimal direction to move is: " + optDir);
                        rc.move(optDir);
                    } else {
                        // if can't move, then try to see whether it's good to just blast away
                        int teamPoli = ((int) ((rc.getConviction() - 10) * rc.getEmpowerFactor(team, 0)))/size;;
                        for (RobotInfo r : robots) {
                            if (r.getTeam() == team && Coms.getTyp(rc.getFlag(r.getID())) == RobotType.POLITICIAN) {
                                teamPoli += ((int) ((r.getConviction() - 10) * rc.getEmpowerFactor(team, 0)))/size;;
                            }
                        }
                        Debug.p("Total team conviction: " + teamPoli);
                        if (attackEffect(closestNeutralDist)[1] > 2) {
                            Debug.p("Can't kill, kamikaze time");
                            if (rc.canEmpower(closestNeutralDist)) rc.empower(closestNeutralDist);
                        } else {
                            if (teamPoli >= 2*neutralEC.getConviction() || rc.getConviction() <= 100) {
                                // just empower
                                if (rc.canEmpower(closestNeutralDist)) rc.empower(closestNeutralDist);
                            }
                        }
                    }
                }
            } else {
                Debug.p("navbugging");
                nav.bugNavigate(closestNeutral);
            }
        }
        else if (closestEC != null) {
            Debug.p("Going to closest EC: " + closestEC);
            if (closestECDist <= 9) {
                Debug.p("Within empower distance");
                // check if can kill
                RobotInfo[] empowered = rc.senseNearbyRobots(closestECDist);
                int size = empowered.length;
                int effect = ((int) ((rc.getConviction() - 10) * rc.getEmpowerFactor(team, 0)))/size;
                RobotInfo enemyEC = rc.senseRobotAtLocation(closestEC);
                if (enemyEC.getConviction()+1 <= effect) {
                    Debug.p("Can kill, will kill");
                    if (rc.canEmpower(closestECDist)) rc.empower(closestECDist);
                } else {
                    // signal that you're attacking, so move out of the way
                    // only signal if you're a fat politician
                    if (rc.getConviction() >= 300 && closestECDist <= 4) {
                        Debug.p("Signalling attack");
                        rc.setFlag(Coms.getMessage(Coms.IC.ATTACK, closestEC));
                    }
                    // check for any empty spots around the EC
                    if (rc.getLocation().isAdjacentTo(closestEC)) {
                        Direction dir = rc.getLocation().directionTo(closestEC);
                        if (rc.getLocation().distanceSquaredTo(closestEC) == 2) {
                            if (rc.canMove(dir.rotateLeft())) rc.move(dir.rotateLeft());
                            if (rc.canMove(dir.rotateRight())) rc.move(dir.rotateRight());
                        }
                    } else {
                        int closestOpenDist = 10000;
                        MapLocation closestOpen = null;
                        for (int i = 0; i < 8; i++) {
                            MapLocation loc = closestEC.add(directions[i]);
                            int dist = loc.distanceSquaredTo(rc.getLocation());
                            if (dist < closestOpenDist && !rc.isLocationOccupied(loc)) {
                                closestOpen = loc;
                                closestOpenDist = dist;
                            }
                        }
                        if (closestOpen != null) nav.bugNavigate(closestOpen);
                    }
                    if (rc.isReady()) {
                        // if can't move, then try to see whether it's good to just blast away
                        int teamPoli = ((int) ((rc.getConviction() - 10) * rc.getEmpowerFactor(team, 0)))/size;;
                        for (RobotInfo r : robots) {
                            if (r.getTeam() == team && r.getType() == RobotType.POLITICIAN) {
                                teamPoli += ((int) ((r.getConviction() - 10) * rc.getEmpowerFactor(team, 0)))/size;;
                            }
                        }
                        if (teamPoli >= Math.min(2*enemyEC.getConviction(), 6000) || rc.getConviction() <= 100) {
                            // just empower
                            if (rc.canEmpower(closestECDist)) rc.empower(closestECDist);
                        }
                    }
                }
            } else {
                Debug.p("navbugging");
                nav.bugNavigate(closestEC);
            }
        } else {
            wander();
            nav.bugNavigate(wandLoc);
        }
    }

    static void defend() throws GameActionException {
        Debug.p("Defending");
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
                    if (r.getType() == RobotType.POLITICIAN && r.getInfluence() < 50) {
                        defended = true;
                        break;
                    }
                }
                if (!defended || (muck != null && rc.getLocation().isWithinDistanceSquared(muck.getLocation(), 16))) {
                    if (muck != null) {
                        // either blow up the muckraker, or go closer to it
                        int locDist = rc.getLocation().distanceSquaredTo(muck.getLocation());
                        attackEffect(locDist);
                        if (locDist <= RobotType.POLITICIAN.actionRadiusSquared) {
                            if (rc.canEmpower(locDist)) rc.empower(locDist);
                        } else nav.bugNavigate(muck.getLocation());
                    }
                    else nav.bugNavigate(enemyMuck);
                }
            }
            if (rc.isReady()) {
                int closestMuckDist = 100000;
                RobotInfo closestMuck = null;
                int closestBuffMuckDist = 100000;
                RobotInfo closestBuffMuck = null;
                for (RobotInfo r : robots) {
                    if (r.getTeam() == team.opponent() && r.getType() == RobotType.MUCKRAKER) {
                        int dist = rc.getLocation().distanceSquaredTo(r.getLocation());
                        if (dist < closestMuckDist) {
                            closestMuckDist = dist;
                            closestMuck = r;
                        }
                        if (r.getConviction() >= 10 && dist < closestBuffMuckDist) {
                            closestBuffMuckDist = dist;
                            closestBuffMuck = r;
                        }
                    }
                }
                if (closestBuffMuck != null) {
                    if (closestBuffMuckDist <= 2 && rc.canEmpower(closestBuffMuckDist)) rc.empower(closestBuffMuckDist);
                    else nav.bugNavigate(closestBuffMuck.getLocation());
                }
                else if (maxEff >= 25) {
                    if (rc.canEmpower(maxEffRadius)) rc.empower(maxEffRadius);
                } else {
                    // otherwise, chase nearby muckrakers and politicians
                    if (closestMuck != null) {
                        boolean defended = false;
                        RobotInfo[] near = rc.senseNearbyRobots(closestMuck.getLocation(), 4, team);
                        for (RobotInfo r : near) {
                            if (Coms.getCat(rc.getFlag(r.getID())) == Coms.IC.MUCKRAKER_ID && Coms.getID(rc.getFlag(r.getID())) == closestMuck.getID()) {
                                defended = true;
                                break;
                            }
                        }
                        if (!defended) {
                            rc.setFlag(Coms.getMessage(Coms.IC.MUCKRAKER_ID, closestMuck.getID()));
                            if (closestMuckDist > 2) nav.bugNavigate(closestMuck.getLocation());
                        }
                    }
                    if (rc.isReady()) {
                        Debug.p("Defending");
                        MapLocation[] nearPolis = new MapLocation[5];
                        int poliInd = 0;
                        RobotInfo[] nearP = rc.senseNearbyRobots(16, team);
                        for (RobotInfo r : nearP) {
                            if (Coms.getTyp(rc.getFlag(r.getID())) == RobotType.POLITICIAN && r.getInfluence() < 50) {
                                if (poliInd < 5) {
                                    nearPolis[poliInd] = r.getLocation();
                                    poliInd++;
                                }
                                if (poliInd == 5) break;
                            }
                        }
                        Debug.p("poliInd: " + poliInd);
                        if (poliInd == 0) {
                            Debug.p("no polies near me");
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
                            if (closestEC != null) nav.bugNavigate(closestEC);
                            else {
                                wander();
                                nav.bugNavigate(wandLoc);
                            }
                        } else {
                            Debug.p("polis near me");
                            int maxH = 0;
                            Direction optDir = null;
                            for (int i = 0; i < 8; i++) {
                                MapLocation loc = rc.getLocation().add(directions[i]);
                                int h = 0;
                                for (int j = 0; j < poliInd; j++) {
                                    Debug.p("near poli: " + j + " " + nearPolis[j]);
                                    h += loc.distanceSquaredTo(nearPolis[j]);
                                }
                                if (h > maxH && rc.canMove(directions[i])) {
                                    maxH = h;
                                    optDir = directions[i];
                                }
                            }
                            if (optDir != null) rc.move(optDir);
                        }
                    }
                }
            }
        }
    }

    static void assist() throws GameActionException {
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
        int closestBuffMuckDist = 100000;
        RobotInfo closestBuffMuck = null;
        for (RobotInfo r : robots) {
            if (r.getTeam() == team.opponent() && r.getType() == RobotType.MUCKRAKER && r.getConviction() >= 50) {
                int dist = rc.getLocation().distanceSquaredTo(r.getLocation());
                if (dist < closestBuffMuckDist) {
                    closestBuffMuckDist = dist;
                    closestBuffMuck = r;
                }
            }
        }
        if (closestBuffMuck != null) {
            Debug.p("Going to closest buffraker: " + closestBuffMuck);
            if (closestBuffMuckDist <= 9) {
                Debug.p("Within empower distance");
                // check if can kill
                RobotInfo[] empowered = rc.senseNearbyRobots(closestBuffMuckDist);
                int size = empowered.length;
                int effect = ((int) ((rc.getConviction() - 10) * rc.getEmpowerFactor(team, 0)))/size;
                if (closestBuffMuck.getConviction()+1 <= effect) {
                    Debug.p("Can kill, will kill");
                    if (rc.canEmpower(closestBuffMuckDist)) rc.empower(closestBuffMuckDist);
                } else {
                    int closerDist = closestBuffMuckDist;
                    Direction optDir = null;
                    for (int i = 0; i < 8; i++) {
                        int dist = rc.getLocation().add(directions[i]).distanceSquaredTo(closestBuffMuck.getLocation());
                        if (dist < closerDist && rc.canMove(directions[i])) {
                            closerDist = dist;
                            optDir = directions[i];
                        }
                    }
                    if (optDir != null) {
                        Debug.p("The optimal direction to move is: " + optDir);
                        rc.move(optDir);
                    } else {
                        // if can't move, then try to see whether it's good to just blast away
                        if (attackEffect(closestBuffMuckDist)[0] >= 20) {
                            Debug.p("Can't kill, kamikaze time");
                            if (rc.canEmpower(closestBuffMuckDist)) rc.empower(closestBuffMuckDist);
                        }
                    }
                }
            } else {
                Debug.p("navbugging");
                nav.bugNavigate(closestBuffMuck.getLocation());
            }
        }
        else if (closestEC != null) {
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
            if (maxKill >= 3) {
                if (rc.canEmpower(maxRadius)) rc.empower(maxRadius);
            } else {
                if (closestECDist <= 9) {
                    Debug.p("Within empower distance");
                    // check if can kill
                    RobotInfo[] empowered = rc.senseNearbyRobots(closestECDist);
                    int size = empowered.length;
                    int effect = ((int) ((rc.getConviction() - 10) * rc.getEmpowerFactor(team, 0)))/size;
                    RobotInfo enemyEC = rc.senseRobotAtLocation(closestEC);
                    if (enemyEC.getConviction()+1 <= effect) {
                        Debug.p("Can kill, will kill");
                        if (rc.canEmpower(closestECDist)) rc.empower(closestECDist);
                    } else {
                        // check for any empty spots around the EC
                        if (rc.getLocation().isAdjacentTo(closestEC)) {
                            Direction dir = rc.getLocation().directionTo(closestEC);
                            if (rc.getLocation().distanceSquaredTo(closestEC) == 2) {
                                if (rc.canMove(dir.rotateLeft())) rc.move(dir.rotateLeft());
                                if (rc.canMove(dir.rotateRight())) rc.move(dir.rotateRight());
                            }
                        } else {
                            int closestOpenDist = 10000;
                            MapLocation closestOpen = null;
                            for (int i = 0; i < 8; i++) {
                                MapLocation loc = closestEC.add(directions[i]);
                                int dist = loc.distanceSquaredTo(rc.getLocation());
                                if (dist < closestOpenDist && !rc.isLocationOccupied(loc)) {
                                    closestOpen = loc;
                                    closestOpenDist = dist;
                                }
                            }
                            if (closestOpen != null) nav.bugNavigate(closestOpen);
                        }
                        if (rc.isReady()) {
                            if (rc.canEmpower(maxEffRadius)) rc.empower(maxEffRadius);
                        }
                    }
                } else {
                    Debug.p("navbugging");
                    nav.bugNavigate(closestEC);
                }
            }
        }
        else defend();
    }

    // calculates the efficiency of the attack
    static int[] attackEffect(int radius) {
        RobotInfo[] empowered = rc.senseNearbyRobots(radius);
        int size = empowered.length;
        if (size == 0 || rc.getConviction() == 0) return new int[] {0, 0};
        effect = ((int) ((rc.getConviction() - 10) * rc.getEmpowerFactor(team, 0)))/size;
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
