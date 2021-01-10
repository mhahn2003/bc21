package coms;

import battlecode.common.Clock;
import battlecode.common.GameActionException;
import battlecode.common.RobotInfo;
import coms.utils.HashSet;

import java.util.ArrayList;

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
    private HashSet<Integer> robotIDs = new HashSet<>(16);

    public ArrayList<Integer> knownRobotId = new ArrayList<Integer>();

    public ECComs() {
        super();
        ECLoc.put(rc.getID(), rc.getLocation());
        ECIds[0] = rc.getID();
        relevantSize = 1;
        relevantFlags[0] = getMessage(InformationCategory.EC_ID, rc.getID());
        friendECs[0] = rc.getLocation();
    }

    public void appendNewUnit(int unitid){
        knownRobotId.add(unitid);
    }

    public void loopBots() throws GameActionException {
        //todo: the bytecode effciency is way way way too low
        if (knownRobotId.size()>0) {
            System.out.println(knownRobotId.toString());
            for (int unitid_dex=0 ; unitid_dex<knownRobotId.size();unitid_dex++) {
                int unitid = knownRobotId.get(unitid_dex);
                if (rc.canGetFlag(unitid)) {
                    System.out.println("processing: " + unitid);
                    processFlag(rc.getFlag(unitid));
                } else {
                    knownRobotId.remove(knownRobotId.indexOf(unitid));
                    unitid_dex--;
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
        for (RobotInfo r: robots) {
            if (r.getTeam() == team) {
                robotIDs.add(r.getID());
            }
        }
        // process known robot IDs
        // TODO: do this
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

        /*
        if (processed) {
            super.processFlag(flag);
        } else {
            if (flag == 0 || getCat(flag) == null) return;
            MapLocation coord = getCoord(flag);
            int ID = getID(flag);
            int minInd;
            boolean seen;
            switch (getCat(flag)) {
                case EDGE_N : if(!edges[0]){edges[0]=true;maxY=ID;System.out.println("updated "+0+"th edge");signalQueue.add(flag);}break;
                case EDGE_E : if(!edges[1]){edges[1]=true;maxX=ID;System.out.println("updated "+1+"st edge");signalQueue.add(flag);}break;
                case EDGE_S : if(!edges[2]){edges[2]=true;minY=ID;System.out.println("updated "+2+"nd edge");signalQueue.add(flag);}break;
                case EDGE_W : if(!edges[3]){edges[3]=true;minX=ID;System.out.println("updated "+3+"rd edge");signalQueue.add(flag);}break;
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
                case FRIEND_EC:
                    minInd = -1;
                    seen = false;
                    for (int i = 11; i >= 0; i--) {
                        if (friendECs[i] == null) {
                            minInd = i;
                        }
                        else if (friendECs[i].equals(coord)) {
                            seen = true;
                            break;
                        }
                    }
                    if (minInd != -1 && !seen) {
                        friendECs[minInd] = coord;
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

         */
    }

}
