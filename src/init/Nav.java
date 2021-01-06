package init;

import battlecode.common.*;

public class Nav {
    private int patience;
    private RobotController rc;
    private MapLocation currentDest;
    private int closestDist;


    // constants
    private double PASS = 0.4;
    private double PASS_DIAG = 0.55;
    private double PASS_PERP = 0.65;
    private double PASS_OPP_DIAG = 0.85;
    private double PASS_OPP = 0.95;
    private int IMPATIENCE = 4;

    public Nav(RobotController rc) {
        this.rc = rc;
        patience = 0;
        currentDest = null;
        closestDist = 1000000;
    }

    public void goTo(MapLocation dest) throws GameActionException {
        // reset if new destination
        if (currentDest != dest) {
            currentDest = dest;
            closestDist = 1000000;
        }
        rc.setIndicatorDot(dest, 0, 255, 0);
        closestDist = Math.min(closestDist, rc.getLocation().distanceSquaredTo(dest));
        if (!rc.isReady()) return;
        // if adjacent to it just move
        if (dest.isAdjacentTo(rc.getLocation())) {
            if (rc.canMove(rc.getLocation().directionTo(dest))) {
                rc.move(rc.getLocation().directionTo(dest));
            }
        }
        // straight
        Direction str = rc.getLocation().directionTo(dest);
        MapLocation opt = rc.getLocation().add(str);
        if (((rc.onTheMap(opt) && rc.sensePassability(opt) > PASS) || patience > IMPATIENCE) && rc.canMove(str)) {
            rc.move(str);
            patience--;
            if (patience < 0) patience = 0;
        }
        // left
        Direction strLeft = str.rotateLeft();
        MapLocation optLeft = rc.getLocation().add(strLeft);
        if (((rc.onTheMap(optLeft) && rc.sensePassability(optLeft) > PASS_DIAG) || patience > IMPATIENCE) && rc.canMove(strLeft)) {
            rc.move(strLeft);
            patience--;
            if (patience < 0) patience = 0;
        }
        // right
        Direction strRight = str.rotateRight();
        MapLocation optRight = rc.getLocation().add(strRight);
        if (((rc.onTheMap(optRight) && rc.sensePassability(optRight) > PASS_DIAG) || patience > IMPATIENCE) && rc.canMove(strRight)) {
            rc.move(strRight);
            patience--;
            if (patience < 0) patience = 0;
        }
        // leftLeft
        Direction strLeftLeft = strLeft.rotateLeft();
        MapLocation optLeftLeft = rc.getLocation().add(strLeftLeft);
        if (((rc.onTheMap(optLeftLeft) && rc.sensePassability(optLeftLeft) > PASS_PERP) || patience > IMPATIENCE) && rc.canMove(strLeftLeft)) {
            rc.move(strLeftLeft);
            if (closestDist <= optLeftLeft.distanceSquaredTo(dest)) {
                patience++;
            }
        }
        // rightRight
        Direction strRightRight = strRight.rotateRight();
        MapLocation optRightRight = rc.getLocation().add(strRightRight);
        if (((rc.onTheMap(optRightRight) && rc.sensePassability(optRightRight) > PASS_PERP) || patience > IMPATIENCE) && rc.canMove(strRightRight)) {
            rc.move(strRightRight);
            if (closestDist <= optRightRight.distanceSquaredTo(dest)) {
                patience++;
            }
        }
        // leftLeftLeft
        Direction strLLL = strLeftLeft.rotateLeft();
        MapLocation optLLL = rc.getLocation().add(strLLL);
        if (((rc.onTheMap(optLLL) && rc.sensePassability(optLLL) > PASS_OPP_DIAG) || patience > IMPATIENCE) && rc.canMove(strLLL)) {
            rc.move(strLLL);
            patience++;
        }
        // rightRightRight
        Direction strRRR = strRightRight.rotateRight();
        MapLocation optRRR = rc.getLocation().add(strRRR);
        if (((rc.onTheMap(optRRR) && rc.sensePassability(optRRR) > PASS_OPP_DIAG) || patience > IMPATIENCE) && rc.canMove(strRRR)) {
            rc.move(strRRR);
            patience++;
        }
        // opp
        Direction strOpp = str.opposite();
        MapLocation optOpp = rc.getLocation().add(strOpp);
        if (((rc.onTheMap(optOpp) && rc.sensePassability(optOpp) > PASS_OPP) || patience > IMPATIENCE) && rc.canMove(strOpp)) {
            rc.move(strOpp);
            patience += 2;
        }
        // if still not moved, just go straight
        if (rc.canMove(str)) {
            rc.move(str);
            patience += 3;
        }
    }


    // chase a unit based on their ID
    public void chase(RobotInfo ri) throws GameActionException {

    }

}
