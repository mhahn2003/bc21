package selfbuff;

import battlecode.common.*;
import selfbuff.utils.*;

import java.util.Map;

public class EC extends Robot {
    int voteCount = -1;
    int bidCount = 1;
    boolean bidded = true;

    // unit count near EC
    int muckCount = 0;
    int polCount = 0;
    int fMuckCount = 0;
    int fPolCount = 0;

    // total unit count
    int tS = 0;
    int tP = 0;
    int tM = 0;

    boolean buffing=false;

    public EC(RobotController rc) {
        super(rc);
    }

    public void takeTurn() throws GameActionException {
        super.takeTurn();
        if (rc.getRoundNum() >= 500) bid();
        // check what kind of units are outside our base
        muckCount = 0;
        polCount = 0;
        fMuckCount = 0;
        fPolCount = 0;
        int maxConv = 0;
        boolean muckHelp = false;
        MapLocation muckHelpLoc = null;
        for (RobotInfo r : robots) {
            if (r.getTeam() == team.opponent()) {
                if (r.getType() == RobotType.POLITICIAN) {
                    polCount++;
                    if (r.getConviction() > maxConv) maxConv = r.getConviction();
                }
                if (r.getType() == RobotType.MUCKRAKER) {
                    if (r.getLocation().isWithinDistanceSquared(rc.getLocation(), 20)) {
                        muckHelp = true;
                        muckHelpLoc = r.getLocation();
                    }
                    muckCount++;
                }
            }
            if (r.getTeam() == team) {
                if (r.getType() == RobotType.POLITICIAN) fPolCount++;
                if (r.getType() == RobotType.MUCKRAKER) fMuckCount++;
            }
        }
        // if muckraker nearby, signal for help
        if (muckHelp) Coms.signalQueue.add(Coms.getMessage(Coms.IC.MUCKRAKER_HELP, muckHelpLoc));
        // scenario 1: if enemy units nearby
        if (polCount > 0) {
            int random = (int) (Math.random() * 4);
            if (random < 3) {
                build(RobotType.MUCKRAKER, 1);
                tM--;
            }
            else build(RobotType.POLITICIAN, 30);
        }
        else if (muckCount > 0) build(RobotType.POLITICIAN, 25);
        // scenario 2
        // if there is a gain from self buffing, start self buffing
        double expectation = rc.getEmpowerFactor(team,10);
        if ((expectation>1.05) &&
            (rc.getInfluence()/(float) GameConstants.ROBOT_INFLUENCE_LIMIT<0.8) ){
            buffing = true;
            boolean builtBuffer=false;
            for (RobotInfo rb: rc.senseNearbyRobots(1)){
                if(rb.type==RobotType.POLITICIAN && rb.influence>rc.getInfluence()){
                    builtBuffer=true;
                }
            }
            if ( !builtBuffer && rc.getInfluence()>200){
                Debug.p("with expectation "+ rc.getEmpowerFactor(team,10));
                // prevent low influence self buffing politician
                if (build(RobotType.POLITICIAN,(int) (rc.getInfluence()*0.99) , true)){
                    Debug.p("ec at " + rc.getLocation().toString() + " build an buffer" );
                }else {
                    Debug.p("ec at " + rc.getLocation().toString() + " failed to build an buffer" );
                }
            }
        }else{
            buffing = false;
        }
        // scenario 3: no enemy units nearby
        // initially build in a 1:4:4 ratio of p, s, m
        // then build in a 2:1:5 ratio of p, s, m
        // then build in a 4:1:2 ratio of p, s, m
        if (rc.getRoundNum() <= 50) {
            if (4*tP < tS) {
                if (rc.getInfluence() >= 400) build(RobotType.POLITICIAN, 400);
                build(RobotType.POLITICIAN, 20);
            }
            else if (tM < tS) {
                build(RobotType.MUCKRAKER, 1);
            }
            else build(RobotType.SLANDERER, Constants.getBestSlanderer(Math.max(rc.getInfluence(), 21)));
        }
        else if (rc.getRoundNum() <= 400) {
            if (tP < 2*tS) {
                if (rc.getInfluence() >= 600) build(RobotType.POLITICIAN, 400);
                build(RobotType.POLITICIAN, 25);
            }
            if (tM < 5*tS) {
                build(RobotType.MUCKRAKER, 1);
            }
            build(RobotType.SLANDERER, Constants.getBestSlanderer(Math.max(rc.getInfluence(), 21)));
        }
        else {
            if (tP < 4*tS) {
                if (rc.getInfluence() >= 800) build(RobotType.POLITICIAN, Math.max(400, rc.getInfluence()/20));
                build(RobotType.POLITICIAN, 50);
            }
            if (tM < 2*tS) {
                build(RobotType.MUCKRAKER, 1);
            }
            build(RobotType.SLANDERER, Constants.getBestSlanderer(Math.max(rc.getInfluence()-200, 21)));
        }
    }

    public boolean build(RobotType toBuild, int influence) throws GameActionException {
        return build(toBuild, influence,false);
    }

    public boolean build(RobotType toBuild, int influence, boolean onlyCardinal) throws GameActionException {
        int safetyNet = 0;
        if (muckCount > 0 && toBuild == RobotType.SLANDERER) return false;
        if (polCount > 0) safetyNet = 100;
        if (toBuild == RobotType.MUCKRAKER) safetyNet = 0;
        if (influence + safetyNet > rc.getInfluence()) return false;
        for (Direction dir :  (onlyCardinal? Direction.cardinalDirections() : directions)) {
            if (rc.canBuildRobot(toBuild, dir, influence)) {
                if(buffing) {
                    //no unit is built near politicians, politician can be build near other units assume that they will move away
                    MapLocation nLoc = rc.getLocation().add(dir.rotateLeft());
                    if (rc.canSenseLocation(nLoc) && rc.senseRobotAtLocation(nLoc) != null && rc.senseRobotAtLocation(nLoc).getType()==RobotType.POLITICIAN ) {
                        continue;
                    }
                    nLoc = rc.getLocation().add(dir.rotateRight());
                    if (rc.canSenseLocation(nLoc) && rc.senseRobotAtLocation(nLoc) != null && rc.senseRobotAtLocation(nLoc).getType()==RobotType.POLITICIAN) {
                        continue;
                    }
                }
                switch (toBuild) {
                    case POLITICIAN: tP++; break;
                    case SLANDERER: tS++; break;
                    case MUCKRAKER: tM++; break;
                }
                rc.buildRobot(toBuild, dir, influence);
                return true;
            }
        }
        return  false;
    }

    public void bid() throws GameActionException {
        int curVote = rc.getTeamVotes();
        if (curVote >= 751) return;
        if (curVote == voteCount && bidded) bidCount *= 2;
        if (rc.canBid(bidCount)) {
            rc.bid(bidCount);
            bidded = true;
        } else bidded = false;
        bidCount -= bidCount/16;
        voteCount = curVote;
    }
}
