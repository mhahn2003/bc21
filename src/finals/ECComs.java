package finals;

import battlecode.common.*;
import finals.utils.Debug;
import finals.utils.HashSet;
import finals.utils.LinkedList;
import finals.utils.Node;

import static finals.Robot.*;
import static finals.RobotPlayer.turnCount;


public class ECComs extends Coms {

    private static int IDcheck = 10000;
    private static boolean allSearched = false;
    private int[] lastFlags = new int[20];
    private int flagIndex = 0;
    // edges, ec id and locations, enemy loc

    private HashSet<Integer> robotIDs = new HashSet<>(50);

    public ECComs() {
        super();
        ECIds[0] = rc.getID();
        relevantSize = 1;
        relevantFlags[0] = getMessage(IC.EC_ID, rc.getID());
        friendECs[0] = rc.getLocation();
    }

    public void loopBots() throws GameActionException {
        // add known IDs
        RobotInfo[] adjRobots = rc.senseNearbyRobots(2, team);
        for (RobotInfo r: adjRobots) {
            robotIDs.add(r.getID());
        }
        // process known robot IDs
        // if array too big, prune
        if (robotIDs.size >= 150) {
            Debug.p("pruning robot ID array");
            HashSet<Integer> tempIDs = new HashSet<>(50);
            for (int i = 0; i < 50; i++) {
                if (robotIDs.table[i].size != 0) tempIDs.add((int) robotIDs.table[i].end.val);
            }
            robotIDs = tempIDs;
            Debug.p("new size: " + robotIDs.size);
        }
//        int counter = 0;
        Debug.p("Size: " + robotIDs.size);
        for (int i = 0; i < 50; i++) {
            LinkedList<Integer> list = robotIDs.table[i];
            if (list.size != 0) {
                Node<Integer> temp = list.head;
                while (temp != null) {
                    int ID = temp.val;
//                    counter++;
                    if (rc.canGetFlag(ID)) {
                        processFlag(rc.getFlag(ID));
                        temp = temp.next;
                    } else {
                        // this might cause a lot of bugs, will see
                        Node<Integer> temp2 = temp;
                        temp = temp.next;
                        list.remove(temp2);
                        robotIDs.size--;
                    }
                }
            }
        }
//        Debug.p("Counter: " + counter);

    }

    public void loopECS() throws GameActionException {
        // process ECs
        for (int i = 0; i < 12; i++) {
            if (ECIds[i] != 0 && ECIds[i] != rc.getID()) {
                Debug.p("checking EC: " + ECIds[i]);
                if (rc.canGetFlag(ECIds[i])) {
                    Debug.p("got flag from EC!");
                    processFlag(rc.getFlag(ECIds[i]));
                }
            }
        }
    }

    // can perform computation through multiple turns, but needs to be called once per turn until it is all done
    // returns whether the looping through flags process has finished
    public boolean loopFlags() throws GameActionException {
        while (Clock.getBytecodesLeft() >= 4000 && IDcheck <= 14096) {
            if (rc.canGetFlag(IDcheck)) {
                int flag = rc.getFlag(IDcheck);
                if (getTyp(flag) == RobotType.ENLIGHTENMENT_CENTER && getCat(flag) != null) {
                    // found an EC!
                    boolean knownID = false;
                    for (int i = 0; i < 12; i++) {
                        if (ECIds[i] == IDcheck) {
                            knownID = true;
                            break;
                        }
                    }
                    if (!knownID) {
                        Debug.p("Found a new ID: " + IDcheck);
                        for (int i = 0; i < 12; i++) {
                            if (ECIds[i] == 0) {
                                ECIds[i] = IDcheck;
                                relevantFlags[relevantSize] = getMessage(IC.EC_ID, IDcheck);
                                relevantSize++;
                                break;
                            }
                        }
                        signalQueue.add(getMessage(IC.EC_ID, IDcheck));
                    }
                }
            }
            IDcheck++;
        }
        if (IDcheck == 14097) {
            allSearched = true;
        }
        return allSearched;
    }

    // todo: a new instance of robot ec is created when a new ec is occupied.
    // get from flags, collect from environment.
    // also displays flag
    public void getInfo() throws GameActionException {
        loopBots();
        loopECS();
        updateMap();
        if (turnCount < 7) {
            lastFlags[9] = getMessage(IC.EC_ID, rc.getID()) % (1 << 22);
            rc.setFlag(getMessage(IC.EC_ID, rc.getID()));
            loopFlags();
        } else {
            // remove redundant flags
            while (!signalQueue.isEmpty()) {
                boolean redundant = false;
                int flag = signalQueue.peek();
                for (int i = 0; i < 20; i++) {
                    if (lastFlags[i] == flag % (1 << 22)) {
                        redundant = true;
                        break;
                    }
                }
                for (int i = 0; i < relevantSize; i++) {
                    if (relevantFlags[relevantInd % relevantSize] % (1 << 22) == flag % (1 << 22)) {
                        redundant = true;
                        break;
                    }
                }
                if (redundant) {
                    Debug.p("Redundant flag, removing");
                    Debug.p("Type: " + getCat(flag));
                    signalQueue.poll();
                }
                else break;
            }
            if (!signalQueue.isEmpty()) {
                // add it to a list of last displayed flags to reduce redundancy between ecs
                int flag = signalQueue.poll();
                Debug.p("getting from signalQueue");
                lastFlags[flagIndex % 20] = flag % (1 << 22);
                flagIndex++;
                Debug.p("Type: " + getCat(flag));
                rc.setFlag(flag);
            } else {
                Debug.p("Getting from relevant flags");
                Debug.p("index: " + relevantInd);
                Debug.p("size: " + relevantSize);
                Debug.p("Type: " + getCat(relevantFlags[relevantInd % relevantSize]));
                Debug.p("coord: " + getCoord(relevantFlags[relevantInd % relevantSize]));
                rc.setFlag(relevantFlags[relevantInd % relevantSize]);
                relevantInd++;
            }
        }
    }

    public void processFlag(int flag) {
        IC cat = getCat(flag);
        if (flag % (1 << 22) == 0 || cat == null) return;
        boolean processed = false;
        for (int i = 0; i < 20; i++) {
            if (lastFlags[i] == flag % (1 << 22)) {
                processed = true;
                break;
            }
        }
        for (int i = 0; i < relevantSize; i++) {
            if (relevantFlags[relevantInd % relevantSize] % (1 << 22) == flag % (1 << 22)) {
                processed = true;
                break;
            }
        }
        if (!processed) {
            if (!sanityCheck(flag)) return;
            if (cat != IC.MUCKRAKER && cat != IC.MUCKRAKER_HELP && cat != IC.MUCKRAKER_ID && cat != IC.POLITICIAN
            && cat != IC.MAP_NE && cat != IC.MAP_NW && cat != IC.MAP_SE
            && cat != IC.MAP_SW && cat != IC.ATTACK) {
                Debug.p("not processed yet, adding to queue: " + flag);
                lastFlags[flagIndex % 20] = flag % (1 << 22);
                flagIndex++;
                signalQueue.add(convertFlag(flag));
            }
            if (cat != IC.MUCKRAKER_HELP && cat != IC.MUCKRAKER && cat != IC.ATTACK && cat != IC.MUCKRAKER_ID && cat != IC.POLITICIAN) super.processFlag(flag);
        }
    }

    public int convertFlag(int flag) {
        flag = flag % (1 << 22);
        return flag + typeInt(rc.getType());
    }

    public void updateMap() {
        if (explored) return;
        Debug.p("updating map");
        if (!mapGenerated && Math.abs(rc.getRoundNum()-turnCount) <= 10) {
            // check if need to generate map
            if (edges[0] && edges[1]) {
                Debug.p("Generating map on NE corner");
                // NE corner
                mapGenerated = true;
                int initX = maxX-3;
                int initY = maxY-3;
                for (int i = 7; i >= 0; i--) {
                    for (int j = 7; j >= 0; j--) {
                        visited[i][j] = false;
                        mapSpots[i][j] = new MapLocation(initX-8*(7-i), initY-8*(7-j));
                    }
                }
                signalQueue.add(getMessage(IC.MAP_CORNER, 0));
                addRelevantFlag(getMessage(IC.MAP_CORNER, 0));
            }
            else if (edges[1] && edges[2]) {
                Debug.p("Generating map on SE corner");
                // SE corner
                mapGenerated = true;
                int initX = maxX-3;
                int initY = minY+4;
                for (int i = 7; i >= 0; i--) {
                    for (int j = 7; j >= 0; j--) {
                        visited[i][j] = false;
                        mapSpots[i][j] = new MapLocation(initX-8*(7-i), initY+8*j);
                    }
                }
                signalQueue.add(getMessage(IC.MAP_CORNER, 1));
                addRelevantFlag(getMessage(IC.MAP_CORNER, 1));
            }
            else if (edges[2] && edges[3]) {
                Debug.p("Generating map on SW corner");
                // SW corner
                mapGenerated = true;
                int initX = minX+4;
                int initY = minY+4;
                for (int i = 7; i >= 0; i--) {
                    for (int j = 7; j >= 0; j--) {
                        visited[i][j] = false;
                        mapSpots[i][j] = new MapLocation(initX+8*i, initY+8*j);
                    }
                }
                signalQueue.add(getMessage(IC.MAP_CORNER, 2));
                addRelevantFlag(getMessage(IC.MAP_CORNER, 2));
            }
            else if (edges[3] && edges[0]) {
                Debug.p("Generating map on NW corner");
                // NW corner
                mapGenerated = true;
                int initX = minX+4;
                int initY = maxY-3;
                for (int i = 7; i >= 0; i--) {
                    for (int j = 7; j >= 0; j--) {
                        visited[i][j] = false;
                        mapSpots[i][j] = new MapLocation(initX+8*i, initY-8*(7-j));
                    }
                }
                signalQueue.add(getMessage(IC.MAP_CORNER, 3));
                addRelevantFlag(getMessage(IC.MAP_CORNER, 3));
            }
        }
        if (updateNE) {
            Debug.p("Updating NE Map");
            int msgSum = 0;
            for (int i = 4; i < 8; i++) {
                for (int j = 4; j < 8; j++) {
                    if (visited[i][j]) msgSum += (1 << ((i-4)*4+j-4));
                }
            }
            // need to check relevant flags and replace the previous ends flag if there is any
            for (int i = 0; i < 20; i++) {
                if (getCat(relevantFlags[i]) == IC.MAP_NE) {
                    removeRelevantFlag(relevantFlags[i]);
                    break;
                }
            }
            addRelevantFlag(getMessage(IC.MAP_NE, msgSum));
        }
        if (updateNW) {
            Debug.p("Updating NW Map");
            int msgSum = 0;
            for (int i = 0; i < 4; i++) {
                for (int j = 4; j < 8; j++) {
                    if (visited[i][j]) msgSum += (1 << (i*4+j-4));
                }
            }
            // need to check relevant flags and replace the previous ends flag if there is any
            for (int i = 0; i < 20; i++) {
                if (getCat(relevantFlags[i]) == IC.MAP_NW) {
                    removeRelevantFlag(relevantFlags[i]);
                    break;
                }
            }
            addRelevantFlag(getMessage(IC.MAP_NW, msgSum));
        }
        if (updateSE) {
            Debug.p("Updating SE Map");
            int msgSum = 0;
            for (int i = 4; i < 8; i++) {
                for (int j = 0; j < 4; j++) {
                    if (visited[i][j]) msgSum += (1 << ((i-4)*4+j));
                }
            }
            // need to check relevant flags and replace the previous ends flag if there is any
            for (int i = 0; i < 20; i++) {
                if (getCat(relevantFlags[i]) == IC.MAP_SE) {
                    removeRelevantFlag(relevantFlags[i]);
                    break;
                }
            }
            addRelevantFlag(getMessage(IC.MAP_SE, msgSum));
        }
        if (updateSW) {
            Debug.p("Updating SW Map");
            int msgSum = 0;
            for (int i = 0; i < 4; i++) {
                for (int j = 0; j < 4; j++) {
                    if (visited[i][j]) msgSum += (1 << (i*4+j));
                }
            }
            // need to check relevant flags and replace the previous ends flag if there is any
            for (int i = 0; i < 20; i++) {
                if (getCat(relevantFlags[i]) == IC.MAP_SW) {
                    removeRelevantFlag(relevantFlags[i]);
                    break;
                }
            }
            addRelevantFlag(getMessage(IC.MAP_SW, msgSum));
        }
        boolean allVisited = true;
        for (int i = 7; i >= 0; i--) {
            for (int j = 7; j >= 0; j--) {
                if (!visited[i][j]) allVisited = false;
            }
        }
        if (allVisited) {
            // reset map
            signalQueue.add(getMessage(IC.RESET, 0));
            addRelevantFlag(getMessage(IC.RESET, 0));
            explored = true;
        }
    }

    public boolean sanityCheck(int flag) {
        if (getCat(flag) == IC.NEUTRAL_EC || getCat(flag) == IC.ENEMY_EC) {
            return !getCoord(flag).equals(rc.getLocation());
        }
        return true;
    }
}
