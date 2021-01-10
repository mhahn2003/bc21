package coms;

import battlecode.common.Clock;
import battlecode.common.GameActionException;
import battlecode.common.RobotInfo;
import coms.utils.*;

import static coms.Robot.*;
import static coms.RobotPlayer.turnCount;


public class ECComs extends Coms {

    private static int IDcheck = 10000;
    private static boolean allSearched = false;
    private int[] lastFlags = new int[10];
    private int flagIndex = 0;
    // edges, ec id and locations, enemy loc
    private int relevantSize = 0;
    private int relevantInd = 0;
    private int[] relevantFlags = new int[20];
    private HashSet<Integer> robotIDs = new HashSet<>(40);

    public ECComs() {
        super();
        ECLoc.put(rc.getID(), rc.getLocation());
        ECIds[0] = rc.getID();
        relevantSize = 1;
        relevantFlags[0] = getMessage(InformationCategory.EC_ID, rc.getID());
        friendECs[0] = rc.getLocation();
    }

    public void loopBots() throws GameActionException {
        // todo: the bytecode effciency is way way way too low
        // if array too big, prune
        if (robotIDs.size >= 80) {
            HashSet<Integer> tempIDs = new HashSet<>(40);
            for (int i = 0; i < 40; i++) {
                if (robotIDs.table[i].size != 0) tempIDs.add((int) robotIDs.table[i].end.val);
            }
            robotIDs = tempIDs;
        }
        for (int i = 0; i < 40; i++) {
            LinkedList<Integer> list = robotIDs.table[i];
            if (list.size != 0) {
                Node<Integer> temp = list.head;
                while (!temp.equals(list.end)) {
                    int ID = temp.val;
                    if (rc.canGetFlag(ID)) {
                        processFlag(rc.getFlag(ID));
                        temp = temp.next;
                    } else {
                        // this might cause a lot of bugs, will see
                        temp = temp.next;
                        list.remove(temp.prev);
                        robotIDs.size--;
                    }
                }
            }
        }
    }

    // can perform computation through multiple turns, but needs to be called once per turn until it is all done
    // returns whether the looping through flags process has finished
    public boolean loopFlags() throws GameActionException {
        while (Clock.getBytecodesLeft() >= 4000 && IDcheck <= 14096) {
            if (rc.canGetFlag(IDcheck)) {
                System.out.println("Hi I'm here");
                System.out.println(IDcheck);
                int flag = rc.getFlag(IDcheck);
                if (getCat(flag) == InformationCategory.EC_ID) {
                    // found an EC!
                    int ID = getID(flag);
                    boolean knownID = false;
                    for (int i = 0; i < 12; i++) {
                        if (ECIds[i] == ID) {
                            knownID = true;
                            break;
                        }
                    }
                    if (!knownID) {
                        System.out.println("Found a new ID: " + ID);
                        for (int i = 0; i < 12; i++) {
                            if (ECIds[i] == 0) {
                                ECIds[i] = ID;
                                relevantFlags[relevantSize] = ID;
                                relevantSize++;
                                break;
                            }
                        }
                    }
                    signalQueue.add(getMessage(InformationCategory.EC_ID, ID));
                }
            }
            IDcheck++;
        }
        // a brutal way to find all friendEC
        /*
        if (loopingIndex<14096) {
            System.out.println("loopingIndex " + loopingIndex);
            rc.setFlag(rc.getID() ^ 0xaaaa);
            int soft_max=10000 + 512 * rc.getRoundNum();
            while (loopingIndex < soft_max && Clock.getBytecodesLeft() > 1000) {
                if (rc.canGetFlag(loopingIndex)) {
                    if (loopingIndex == (rc.getFlag(loopingIndex) ^ 0xaaaa)) {
                        friendECs.add(loopingIndex);
                    }
                }
                loopingIndex += 1;
            }
            System.out.println("loopingIndex " + loopingIndex);
        }
         */
        if (IDcheck == 14097) {
            allSearched = true;
        }
        return allSearched;
    }

    // todo: a new instance of robot ec is created when a new ec is occupied.
    // get from flags, collect from environment.
    public void getInfo() throws GameActionException {
        loopBots();
        loopECS();
        if (turnCount < 10) {
            lastFlags[9] = getMessage(InformationCategory.EC_ID, rc.getID());
            rc.setFlag(getMessage(InformationCategory.EC_ID, rc.getID()));
            loopFlags();
        } else {
            if (!signalQueue.isEmpty()) {
                // add it to a list of last displayed flags to reduce redundancy between ecs
                int flag = signalQueue.poll();
                lastFlags[flagIndex % 10] = flag;
                flagIndex++;
                rc.setFlag(flag);
            } else {
                rc.setFlag(relevantFlags[relevantInd % relevantSize]);
                relevantInd++;
            }
        }
    }

    public void loopECS() throws GameActionException {
        robots = rc.senseNearbyRobots();
        // process ECs
        for (int i = 0; i < 12; i++) {
            if (ECIds[i] != 0 && ECIds[i] != rc.getID()) {
                if (rc.canGetFlag(ECIds[i])) {
                    processFlag(rc.getFlag(ECIds[i]));
                }
            }
        }
        // add known IDs
        RobotInfo[] adjRobots = rc.senseNearbyRobots(2, team);
        for (RobotInfo r: adjRobots) {
            robotIDs.add(r.getID());
        }
        // process known robot IDs
        // TODO: do this
        loopBots();
    }

    public void processFlag(int flag) {
        boolean processed = false;
        for (int i = 0; i < 10; i++) {
            if (lastFlags[i] == flag) {
                processed = true;
                break;
            }
        }
        if (!processed) {
            signalQueue.add(flag);
        }
        super.processFlag(flag);
    }

}
