package coms;

import battlecode.common.Clock;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;

import static coms.Robot.*;
import static coms.RobotPlayer.turnCount;

public class ECComs extends Coms {

    private static int IDcheck = 10000;
    private static boolean allSearched = false;
    private int[] lastFlags = new int[10];
    private int flagIndex = 0;

    public ECComs(RobotController r) {
        super(r);
        ECLoc.put(rc.getID(), rc.getLocation());
        ECIds[0] = rc.getID();
        ECs[0] = rc.getLocation();
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
                                break;
                            }
                        }
                    }
                    signalQueue.add(getMessage(InformationCategory.EC_ID, ID));
                }
            }
            IDcheck++;
        }
        if (IDcheck == 14097) {
            allSearched = true;
        }
        return allSearched;
    }

    public void collectInfo() throws GameActionException {
        if (turnCount < 10) {
            lastFlags[9] = getMessage(InformationCategory.EC_ID, rc.getID());
            rc.setFlag(getMessage(InformationCategory.EC_ID, rc.getID()));
            loopFlags();
        }
        else {
            if (!signalQueue.isEmpty()) {
                // add it to a list of last displayed flags to reduce redundancy between ecs
                int flag = signalQueue.poll();
                lastFlags[flagIndex % 10] = flag;
                flagIndex++;
                rc.setFlag(flag);
            }
        }
    }


    public void getInfo() throws GameActionException {
        // process ECs
        for (int i = 0; i < 12; i++) {
            if (ECIds[i] != 0 && ECIds[i] != rc.getID()) {
                if (rc.canGetFlag(ECIds[i])) {
                    processFlag(rc.getFlag(ECIds[i]));
                }
            }
        }
    }

    public void processFlag(int flag) {
        boolean processed = false;
        for (int i = 0; i < 10; i++) {
            if (lastFlags[i] == flag) {
                processed = true;
                break;
            }
        }
        if (processed) {
            super.processFlag(flag);
        } else {
            if (flag == 0 || getCat(flag) == null) return;
            MapLocation coord = getCoord(flag);
            int ID = getID(flag);
            int minInd;
            boolean seen;
            switch (getCat(flag)) {
                case EDGE:
                    if (coord.x == 9999) {
                        edges[2] = true;
                        minY = coord.y;
                    }
                    if (coord.x == 30065) {
                        edges[0] = true;
                        maxY = coord.y;
                    }
                    if (coord.y == 9999) {
                        edges[3] = true;
                        minX = coord.x;
                    }
                    if (coord.y == 30065) {
                        edges[1] = true;
                        maxX = coord.x;
                    }
                    signalQueue.add(flag);
                    break;
                case ENEMY_EC:
                    minInd = -1;
                    seen = false;
                    for (int i = 11; i >= 0; i--) {
                        if (enemyECs[i] == null) {
                            minInd = i;
                        }
                        else if (enemyECs[i].equals(coord)) {
                            seen = true;
                            break;
                        }
                    }
                    if (minInd != -1 && !seen) {
                        enemyECs[minInd] = coord;
                        signalQueue.add(flag);
                    }
                    break;
                case EC:
                    minInd = -1;
                    seen = false;
                    for (int i = 11; i >= 0; i--) {
                        if (ECs[i] == null) {
                            minInd = i;
                        }
                        else if (ECs[i].equals(coord)) {
                            seen = true;
                            break;
                        }
                    }
                    if (minInd != -1 && !seen) {
                        ECs[minInd] = coord;
                        signalQueue.add(flag);
                    }
                    break;
                case NEUTRAL_EC:
                    minInd = -1;
                    seen = false;
                    for (int i = 11; i >= 0; i--) {
                        if (neutralECs[i] == null) {
                            minInd = i;
                        }
                        else if (neutralECs[i].equals(coord)) {
                            seen = true;
                            break;
                        }
                    }
                    if (minInd != -1 && !seen) {
                        neutralECs[minInd] = coord;
                        signalQueue.add(flag);
                    }
                    break;
                case EC_ID:
                    minInd = -1;
                    seen = false;
                    for (int i = 11; i >= 0; i--) {
                        if (ECIds[i] == 0) {
                            minInd = i;
                        }
                        if (ECIds[i] == ID) {
                            seen = true;
                            break;
                        }
                    }
                    if (minInd != -1 && !seen) {
                        ECIds[minInd] = ID;
                        signalQueue.add(flag);
                    }
                    break;
            }
        }
    }
}
