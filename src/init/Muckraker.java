package init;

import battlecode.common.*;

public class Muckraker extends Robot {

    private int[][] ends = {{10000, 10000}, {10000, 20000}, {10000, 30000}, {20000, 30000}, {30000, 30000}, {30000, 20000}, {30000, 10000}, {20000, 10000}};
    private MapLocation wandLoc;

    public Muckraker(RobotController rc) {
        super(rc);
        wandLoc = new MapLocation(ends[rc.getID() % 8][0], ends[rc.getID() % 8][1]);
    }

    public void takeTurn() throws GameActionException {
        super.takeTurn();
        Team enemy = rc.getTeam().opponent();
        int actionRadius = rc.getType().actionRadiusSquared;
        for (RobotInfo robot : rc.senseNearbyRobots(actionRadius, enemy)) {
            if (robot.type.canBeExposed()) {
                // It's a slanderer... go get them!
                if (rc.canExpose(robot.location)) {
                    System.out.println("e x p o s e d");
                    rc.expose(robot.location);
                    return;
                }
            }
        }
        if (tryMove(randomDirection()))
            System.out.println("I moved!");
    }

    static boolean tryMove(Direction dir) throws GameActionException {
        System.out.println("I am trying to move " + dir + "; " + rc.isReady() + " " + rc.getCooldownTurns() + " " + rc.canMove(dir));
        if (rc.canMove(dir)) {
            rc.move(dir);
            return true;
        } else return false;
    }

    static Direction randomDirection() {
        return directions[(int) (Math.random() * directions.length)];
    }

    // wander around
    public void wander() throws GameActionException {
        nav.goTo(wandLoc);
    }
}