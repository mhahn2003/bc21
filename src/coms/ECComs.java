package coms;

import battlecode.common.Clock;
import battlecode.common.GameActionException;
import battlecode.common.RobotController;

import static coms.Robot.*;
import static coms.RobotPlayer.turnCount;

import java.util.ArrayList;


public class ECComs extends Coms {

    private static int IDcheck = 10000;
    private static boolean allSearched = false;

    public ArrayList<Integer> knownRobotId = new ArrayList<Integer>();

    public ECComs() {
        super();
        ECLoc.put(rc.getID(), rc.getLocation());
        ECIds[0] = rc.getID();
        friendECs[0] = rc.getLocation();
    }

    public void appendNewUnit(int unitid){
        knownRobotId.add(unitid);
    }

    public void loopBots() throws GameActionException {
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
        while (Clock.getBytecodesLeft() >= 1500 && IDcheck <= 14096) {
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
        if (turnCount < 10) {
            rc.setFlag(getMessage(InformationCategory.EC_ID, rc.getID()));
            loopFlags();
        }
        loopBots();
    }
}
