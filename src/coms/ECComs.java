package coms;

import battlecode.common.Clock;
import battlecode.common.GameActionException;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import coms.utils.*;

import static coms.Robot.*;
import static coms.RobotPlayer.turnCount;


public class ECComs extends Coms {

    private static int IDcheck = 10000;
    private static boolean allSearched = false;
    private int[] lastFlags = new int[10];
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
        if (robotIDs.size >= 200) {
            Debug.p("pruning robot ID array");
            HashSet<Integer> tempIDs = new HashSet<>(50);
            for (int i = 0; i < 50; i++) {
                if (robotIDs.table[i].size != 0) tempIDs.add((int) robotIDs.table[i].end.val);
            }
            robotIDs = tempIDs;
            Debug.p("new size: " + robotIDs.size);
        }
//        int counter = 0;
//        Debug.p("Size: " + robotIDs.size);
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
                if (getTyp(flag) == RobotType.ENLIGHTENMENT_CENTER) {
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
        if (turnCount < 7) {
            lastFlags[9] = getMessage(IC.EC_ID, rc.getID()) & 0x7ffff;
            rc.setFlag(getMessage(IC.EC_ID, rc.getID()));
            loopFlags();
        } else {
            if (!signalQueue.isEmpty()) {
                // add it to a list of last displayed flags to reduce redundancy between ecs
                int flag = signalQueue.poll();
                Debug.p("getting from signalQueue");
                lastFlags[flagIndex % 10] = flag & 0x7ffff;
                flagIndex++;
                rc.setFlag(flag);
            } else {
                rc.setFlag(relevantFlags[relevantInd % relevantSize]);
                relevantInd++;
            }
        }
    }

    public void processFlag(int flag) {
        IC cat = getCat(flag);
        if ((flag & 0x7ffff) == 0 || cat == null) return;
        boolean processed = false;
        for (int i = 0; i < 10; i++) {
            if (lastFlags[i] == (flag & 0x7ffff)) {
                processed = true;
                break;
            }
        }
        if (!processed) {
            Debug.p("not processed yet, adding to queue: " + flag);

            signalQueue.add(convertFlag(flag));
        }
        super.processFlag(flag);
    }

    public int convertFlag(int flag) {
        flag = flag & 0x7ffff;
        return flag + typeInt(rc.getType());
    }

}
