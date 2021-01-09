package bug;

import battlecode.common.*;

public class Nav {
    private int patience;
    private static RobotController rc;
    private MapLocation currentDest;
    private int closestDist;

    private int minX = 9999;
    private int maxX = 30001;
    private int minY = 9999;
    private int maxY = 30001;
    private int[][] ends;
    public boolean[] edges = {false, false, false, false};
    private static Direction[] directions = Direction.cardinalDirections();

    // constants
    private double PASS = 0.4;
    private double PASS_DIAG = 0.55;
    private double PASS_PERP = 0.65;
    private double PASS_OPP_DIAG = 0.85;
    private double PASS_OPP = 0.95;
    private int IMPATIENCE = 4;
    private static double THRESHOLD = 0.45;

    public Nav(RobotController rc) {
        this.rc = rc;
        patience = 0;
        currentDest = null;
        closestDist = 1000000;
    }

//    public void goTo(MapLocation dest) throws GameActionException {
//        // reset if new destination
//        if (currentDest != dest) {
//            currentDest = dest;
//            closestDist = 1000000;
//        }
//        System.out.println("Going towards" + dest.toString());
//        rc.setIndicatorDot(dest, 0, 255, 0);
//        closestDist = Math.min(closestDist, rc.getLocation().distanceSquaredTo(dest));
//        if (!rc.isReady()) return;
//        // if adjacent to it just move
//        if (dest.isAdjacentTo(rc.getLocation())) {
//            if (rc.canMove(rc.getLocation().directionTo(dest))) {
//                rc.move(rc.getLocation().directionTo(dest));
//            }
//        }
//        // straight
//        Direction str = rc.getLocation().directionTo(dest);
//        MapLocation opt = rc.getLocation().add(str);
//        if (((rc.onTheMap(opt) && rc.sensePassability(opt) > PASS) || patience > IMPATIENCE) && rc.canMove(str)) {
//            rc.move(str);
//            patience--;
//            if (patience < 0) patience = 0;
//        }
//        // left
//        Direction strLeft = str.rotateLeft();
//        MapLocation optLeft = rc.getLocation().add(strLeft);
//        if (((rc.onTheMap(optLeft) && rc.sensePassability(optLeft) > PASS_DIAG) || patience > IMPATIENCE) && rc.canMove(strLeft)) {
//            rc.move(strLeft);
//            patience--;
//            if (patience < 0) patience = 0;
//        }
//        // right
//        Direction strRight = str.rotateRight();
//        MapLocation optRight = rc.getLocation().add(strRight);
//        if (((rc.onTheMap(optRight) && rc.sensePassability(optRight) > PASS_DIAG) || patience > IMPATIENCE) && rc.canMove(strRight)) {
//            rc.move(strRight);
//            patience--;
//            if (patience < 0) patience = 0;
//        }
//        // leftLeft
//        Direction strLeftLeft = strLeft.rotateLeft();
//        MapLocation optLeftLeft = rc.getLocation().add(strLeftLeft);
//        if (((rc.onTheMap(optLeftLeft) && rc.sensePassability(optLeftLeft) > PASS_PERP) || patience > IMPATIENCE) && rc.canMove(strLeftLeft)) {
//            rc.move(strLeftLeft);
//            if (closestDist <= optLeftLeft.distanceSquaredTo(dest)) {
//                patience++;
//            }
//        }
//        // rightRight
//        Direction strRightRight = strRight.rotateRight();
//        MapLocation optRightRight = rc.getLocation().add(strRightRight);
//        if (((rc.onTheMap(optRightRight) && rc.sensePassability(optRightRight) > PASS_PERP) || patience > IMPATIENCE) && rc.canMove(strRightRight)) {
//            rc.move(strRightRight);
//            if (closestDist <= optRightRight.distanceSquaredTo(dest)) {
//                patience++;
//            }
//        }
//        // leftLeftLeft
//        Direction strLLL = strLeftLeft.rotateLeft();
//        MapLocation optLLL = rc.getLocation().add(strLLL);
//        if (((rc.onTheMap(optLLL) && rc.sensePassability(optLLL) > PASS_OPP_DIAG) || patience > IMPATIENCE) && rc.canMove(strLLL)) {
//            rc.move(strLLL);
//            patience++;
//        }
//        // rightRightRight
//        Direction strRRR = strRightRight.rotateRight();
//        MapLocation optRRR = rc.getLocation().add(strRRR);
//        if (((rc.onTheMap(optRRR) && rc.sensePassability(optRRR) > PASS_OPP_DIAG) || patience > IMPATIENCE) && rc.canMove(strRRR)) {
//            rc.move(strRRR);
//            patience++;
//        }
//        // opp
//        Direction strOpp = str.opposite();
//        MapLocation optOpp = rc.getLocation().add(strOpp);
//        if (((rc.onTheMap(optOpp) && rc.sensePassability(optOpp) > PASS_OPP) || patience > IMPATIENCE) && rc.canMove(strOpp)) {
//            rc.move(strOpp);
//            patience += 2;
//        }
//        // if still not moved, just go straight
//        if (rc.canMove(str)) {
//            rc.move(str);
//            patience += 3;
//        }
//        lookAround();
//    }


    // chase a unit based on their ID
    public void chase(RobotInfo ri) throws GameActionException {

    }

    // relay information about surroundings
    public void lookAround() throws GameActionException {
        // first check for any edges
        for (int i = 0; i < 4; i++) {
            Direction dir = Direction.cardinalDirections()[i];
            System.out.println(dir.toString());
            MapLocation checkLoc = rc.getLocation().add(dir);
            while (checkLoc.isWithinDistanceSquared(rc.getLocation(), rc.getType().sensorRadiusSquared)) {
                if (!rc.onTheMap(checkLoc)) {
                    System.out.println("I see an edge");
                    if (!edges[i]) {
                        // comm this information
                        edges[i] = true;
                        if (i == 0) maxY = checkLoc.y-1;
                        if (i == 1) maxX = checkLoc.x-1;
                        if (i == 2) minY = checkLoc.y+1;
                        if (i == 3) minX = checkLoc.x+1;
                    }
                    break;
                }
                checkLoc = checkLoc.add(dir);
            }
        }
    }

    public int[][] getEnds() {
        int midX, midY;
        if (minX == 9999 && maxX == 30001) midX = 20000;
        else if (minX == 9999) midX = maxX-32;
        else if (maxX == 30001) midX = minX+32;
        else midX = (minX+maxX)/2;

        if (minY == 9999 && maxY == 30001) midY = 20000;
        else if (minY == 9999) midY = maxY-32;
        else if (maxY == 30001) midY = minY+32;
        else midY = (minY+maxY)/2;

        ends = new int[][]{{minX, minY}, {minX, midY}, {minX, maxY}, {midX, maxY}, {maxX, maxY}, {maxX, midY}, {maxX, minY}, {midX, minY}};
        return ends;
    }

    public static boolean isTrapped () throws GameActionException {
        for (Direction dir: directions) {
            if (checkDirMoveable(dir)) {
                return false;
            }
        }
        return true;
    }

    /*
    Tries to move in the target direction
    If we are not a drone, it does not move into flooded tiles
    Returns the Direction that we moved in
    Returns null if did not move
    */
    public static Direction tryMoveInDirection (Direction dir) throws GameActionException {
        if (checkDirMoveable(dir)) {
            rc.move(dir);
            return dir;
        }
        return null;
    }

	/*
	---------------
	BUG PATHFINDING
	---------------
	Uses the bug pathfinding algorithm to navigate around obstacles towards a target MapLocation
	Details here: https://www.cs.cmu.edu/~motionplanning/lecture/Chap2-Bug-Alg_howie.pdf
	Taken/adapted from TheDuck314 Battlecode 2016
	Assumes that we are ready to move
	Returns the Direction we moved in
	Returns null if did not move
	*/

    final public static int MAX_BUG_HISTORY_LENGTH = 100;

    public static MapLocation bugTarget = null;

    public static boolean bugTracing = false;
    public static MapLocation bugLastWall = null;
    public static int bugClosestDistanceToTarget = 1000000000;
    public static int bugTurnsWithoutWall = 0;
    public static boolean bugRotateLeft = true; // whether we are rotating left or right

    public static MapLocation[] bugVisitedLocations = null;
    public static int bugVisitedLocationsIndex;
    public static int bugVisitedLocationsLength;

    public Direction bugNavigate (MapLocation target) throws GameActionException {
        System.out.println("Bug navigating to " + target);

        if (isTrapped()) {
            return null;
        }

        if (!target.equals(bugTarget)) {
            bugTarget = target;
            bugTracing = false;
            bugClosestDistanceToTarget = rc.getLocation().distanceSquaredTo(bugTarget);
        }

        if (rc.getLocation().equals(bugTarget)) {
            return null;
        }

        // bugClosestDistanceToTarget = Math.min(bugClosestDistanceToTarget, here.distanceSquaredTo(bugTarget));

        Direction destDir = rc.getLocation().directionTo(bugTarget);

        System.out.println("BUG_NAVIGATE");
        System.out.println("bugTarget: " + bugTarget);
        System.out.println("bugClosestDistanceToTarget: " + bugClosestDistanceToTarget);
        System.out.println("destDir: " + destDir);
        System.out.println("bugTracing: " + bugTracing);

        if (!bugTracing) { // try to go directly towards the target
            Direction tryMoveResult = tryMoveInDirection(destDir);
            if (tryMoveResult != null) {
                return tryMoveResult;
            } else {
                bugStartTracing();
            }
        } else { // we are on obstacle, trying to get off of it
            if (rc.getLocation().distanceSquaredTo(bugTarget) < bugClosestDistanceToTarget) {
                Direction tryMoveResult = tryMoveInDirection(destDir);
                System.out.println("on obstacle");
                if (tryMoveResult != null) { // we got off of the obstacle
                    System.out.println("We're free!");
                    bugTracing = false;
                    return tryMoveResult;
                }
            }
        }

        Direction moveDir = bugTraceMove(false);

        if (bugTurnsWithoutWall >= 2) {
            bugTracing = false;
        }

        return moveDir;
    }

    /*
    Runs if we just encountered an obstacle
    */
    public static void bugStartTracing() throws GameActionException {
        bugTracing = true;

        bugVisitedLocations = new MapLocation[MAX_BUG_HISTORY_LENGTH];
        bugVisitedLocationsIndex = 0;
        bugVisitedLocationsLength = 0;

        bugTurnsWithoutWall = 0;
        bugClosestDistanceToTarget = rc.getLocation().distanceSquaredTo(bugTarget);

        Direction destDir = rc.getLocation().directionTo(bugTarget);

        Direction leftDir = destDir;
        MapLocation leftDest;
        int leftDist = Integer.MAX_VALUE;
        for (int i = 0; i < 8; ++i) {
            leftDir = leftDir.rotateLeft();
            leftDest = rc.adjacentLocation(leftDir);
            if (checkDirMoveable(leftDir)) {
                leftDist = leftDest.distanceSquaredTo(bugTarget);
                break;
            }
        }

        Direction rightDir = destDir;
        MapLocation rightDest;
        int rightDist = Integer.MAX_VALUE;
        for (int i = 0; i < 8; ++i) {
            rightDir = rightDir.rotateRight();
            rightDest = rc.adjacentLocation(rightDir);
            if (checkDirMoveable(rightDir)) {
                rightDist = rightDest.distanceSquaredTo(bugTarget);
                break;
            }
        }


        if (leftDist < rightDist) { // prefer rotate right if equal
            bugRotateLeft = true;
            bugLastWall = rc.adjacentLocation(leftDir.rotateRight());
        } else {
            bugRotateLeft = false;
            bugLastWall = rc.adjacentLocation(rightDir.rotateLeft());
        }
        System.out.println("START_TRACING");
        System.out.println("bugRotateLeft: " + bugRotateLeft);
        System.out.println("bugLastWall: " + bugLastWall);
    }

    /*
    Returns the Direction that we moved in
    Returns null if we did not move
    */
    public static Direction bugTraceMove(boolean recursed) throws GameActionException {

        Direction curDir = rc.getLocation().directionTo(bugLastWall);

        // adds to array
        bugVisitedLocations[bugVisitedLocationsIndex] = rc.getLocation();
        bugVisitedLocationsIndex = (bugVisitedLocationsIndex + 1) % MAX_BUG_HISTORY_LENGTH;
        bugVisitedLocationsLength = Math.min(bugVisitedLocationsLength + 1, MAX_BUG_HISTORY_LENGTH);

        if (checkDirMoveable(curDir)) {
            bugTurnsWithoutWall += 1;
        } else {
            bugTurnsWithoutWall = 0;
        }

        for (int i = 0; i < 8; ++i) {
            if (bugRotateLeft) {
                curDir = curDir.rotateLeft();
            } else {
                curDir = curDir.rotateRight();
            }
            MapLocation curDest = rc.adjacentLocation(curDir);
            if (!rc.onTheMap(curDest) && !recursed) {
                System.out.println("Hit the edge of map, reverse and recurse");
                // if we hit the edge of the map, reverse direction and recurse
                bugRotateLeft = !bugRotateLeft;
                return bugTraceMove(true);
            }
            if (checkDirMoveable(curDir)) {
                rc.move(curDir);
                for (int x = 0; x < bugVisitedLocationsLength; x++) {
                    if (bugVisitedLocations[x].equals(curDest)) {
                        System.out.println("Resetting bugTracing");
                        bugTracing = false;
                        break;
                    }
                }
                return curDir;
            } else {
                bugLastWall = rc.adjacentLocation(curDir);
            }
        }

        return null;
    }

    public static boolean checkDirMoveable(Direction dir) throws GameActionException {
        return rc.canMove(dir) && rc.sensePassability(rc.getLocation().add(dir)) > THRESHOLD;
    }
}
