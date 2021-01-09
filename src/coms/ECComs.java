package coms;

import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.RobotController;

import static coms.Robot.*;

import java.util.ArrayList;

public class ECComs extends Coms {

    private int loopingIndex=10000;

    public ArrayList<Integer> knownRobotId = new ArrayList<Integer>();

    public ECComs() {
        super();
    }

    public void appendNewUnit(int unitid){
        knownRobotId.add(unitid);
    }

    public void collectInfo() throws GameActionException {
        System.out.println("before checking other bots " + Clock.getBytecodesLeft());
        if (knownRobotId.size()>0) {
            System.out.println(knownRobotId.toString());
            for (int unitid_dex=0 ; unitid_dex<knownRobotId.size();unitid_dex++) {
                int unitid = knownRobotId.get(unitid_dex);
                if (rc.canGetFlag(unitid)) {
                    getInfo(rc.getFlag(unitid));
                } else {
                    knownRobotId.remove(knownRobotId.indexOf(unitid));
                    unitid_dex--;
                }
            }
        }
        System.out.println("after checking other bots " + Clock.getBytecodesLeft());

        // a brutal way to find all friendEC
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

        System.out.println("after checking other ids " + Clock.getBytecodesLeft());
    }

}
