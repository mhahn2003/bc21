package dijkstra;

import battlecode.common.*;

public class Nav {
    private int patience;
    private RobotController rc;
    private MapLocation currentDest;
    private int closestDist;

    private int minX = 9999;
    private int maxX = 30001;
    private int minY = 9999;
    private int maxY = 30001;
    private int[][] ends;
    public boolean[] edges = {false, false, false, false};
    private int[] pq = new int[5000];
    private int curInd;
    private int[] dirs = {-4, -3, -2, -1, 1, 2, 3, 4};

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
        System.out.println("Going towards" + dest.toString());
        rc.setIndicatorDot(dest, 0, 255, 0);
        closestDist = Math.min(closestDist, rc.getLocation().distanceSquaredTo(dest));
        if (!rc.isReady()) return;
        System.out.println("starting dijkstra");
        System.out.println(Clock.getBytecodeNum());
        // dijkstra
        int[] distance = new int[9];
        int[] source = new int[9];
        int[] pass = new int[9];
        MapLocation[] blocked = rc.detectNearbyRobots(2);
        // make passability big if there's robot
        for (int i = 0; i < blocked.length; i++) {
            int ind = (blocked[i].x-rc.getLocation().x)*5+blocked[i].y-rc.getLocation().y+12;
            pass[ind] = 1000;
        }
        for (int i = -1; i <= 1; i++) {
            for (int j = -1; j <= 1; j++) {
                int ind = i*3+j+4;
                MapLocation trans = rc.getLocation().translate(i, j);
                if (pass[ind] != 1000 && rc.onTheMap(trans)) pass[ind] = (int) (1/rc.sensePassability(trans));
            }
        }
        curInd = 0;
        distance[4] = 0;
        for (int i = 8; i >= 0; i--) {
            if (i != 4) distance[i] = 10000000;
        }
        System.out.println("dijkstra setup done");
        System.out.println(Clock.getBytecodeNum());
        pqAdd(4);
        while (curInd != 0) {
            int minPQ = removeMin();
            int w = minPQ/100;
            int u = minPQ % 100;
            for (int i = 7; i >= 0; i--) {
                int v = u + dirs[i];
                if (v >= 0 && v < 9 && distance[v] > distance[u] + pass[u]) {
                    source[v] = u;
                    distance[v] = distance[u] + pass[u];
                    pqAdd(100*distance[v]+v);
                }
            }
        }
        System.out.println("dijkstra loop done");
        System.out.println(Clock.getBytecodeNum());
        // process the distance
        int minDist = 1000000000;
        int minLoc = 0;
        for (int i = -1; i <= 1; i++) {
            for (int j = -1; j <= 1; j++) {
                int ind = i*3+j+4;
                // TODO: tweak constant
                // there's a constant here for a heuristic!
                distance[ind] += 3*Math.max(Math.abs(dest.x-i), Math.abs(dest.y-j));
                if (distance[ind] < minDist) {
                    minDist = distance[ind];
                    minLoc = ind;
                }
            }
        }
        if (minLoc == 4) {
            // best is not to move? then just move
            Direction optDir = rc.getLocation().directionTo(dest);
            if (rc.canMove(optDir)) rc.move(optDir);
        } else {
            while (source[minLoc] != 4) {
                minLoc = source[minLoc];
            }
            Direction optDir = base3Dir(minLoc-4);
            if (rc.canMove(optDir)) rc.move(optDir);
        }
        System.out.println("dijkstra complete");
        System.out.println(Clock.getBytecodeNum());
        lookAround();
    }

    private Direction base3Dir(int a) {
        switch (a) {
            case -4: return Direction.SOUTHWEST;
            case -3: return Direction.WEST;
            case -2: return Direction.NORTHWEST;
            case -1: return Direction.SOUTH;
            case 1: return Direction.NORTH;
            case 2: return Direction.SOUTHEAST;
            case 3: return Direction.EAST;
            case 4: return Direction.NORTHEAST;
        }
        // should not get here
        return Direction.CENTER;
    }

    // min heap code

    private void pqAdd(int a) {
        curInd++;
        pq[curInd] = a;
        heapifyUp(curInd);
    }

    private void heapifyUp(int a) {
        if (a > 1) {
            if (pq[a] < pq[a/2]) {
                int swap = pq[a];
                pq[a] = pq[a/2];
                pq[a/2] = swap;
                heapifyUp(a/2);
            }
        }
    }

    private int removeMin() {
        int minVal = pq[1];
        pq[1] = pq[curInd];
        curInd--;
        heapifyDown(1);
        return minVal;
    }

    private void heapifyDown(int a) {
        if (2*a <= curInd) {
            int minInd;
            if (2*a == curInd) minInd = 2*a;
            else {
                if (pq[2*a] < pq[2*a+1]) minInd = 2*a;
                else minInd = 2*a+1;
            }
            if (pq[a] > pq[minInd]) {
                int swap = pq[a];
                pq[a] = pq[minInd];
                pq[minInd] = swap;
                heapifyDown(minInd);
            }
        }
    }


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
}
