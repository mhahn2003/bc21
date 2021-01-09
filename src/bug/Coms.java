package bug;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.RobotController;

import java.util.LinkedList;
import java.util.Queue;

public class Coms {
    private final RobotController rc;
    private final int senseRadius;
    // 3 cols one for id one for genre, one for number of remaining rounds
    // 10 rows for accepting 10 inputs at ones.
//    private final int[][] ongoingConversations = new int[3][10];
    private final int[] enlightenmentCenterIds = new int[12];
    private final Queue<Integer> signalQueue = new LinkedList<>();

    /* code look up table
    * 0000
    * 0001
    * 0010
    * 0011
    * 0100
    * 0101
    * 0110
    * 0111
    * 1000
    * 1001
    * 1010
    * 1011
    * 1100
    * 1101
    * 1110
    * 1111
    * 0b10000001 for sending path
    * */

    public Coms(RobotController r){
        rc = r;
        senseRadius = rc.getType().sensorRadiusSquared;
    }

    static int directionToInt(Direction dir) {
        switch(dir) {
            case NORTH    : return 0;
            case NORTHEAST: return 1;
            case EAST     : return 2;
            case SOUTHEAST: return 3;
            case SOUTH    : return 4;
            case SOUTHWEST: return 5;
            case WEST     : return 6;
            case NORTHWEST: return 7;
        }
        assert (false);
        return -1;
    }

    static Direction intToDirection(int path) {
        switch(path) {
            case 0: return Direction.NORTH    ;
            case 1: return Direction.NORTHEAST;
            case 2: return Direction.EAST     ;
            case 3: return Direction.SOUTHEAST;
            case 4: return Direction.SOUTH    ;
            case 5: return Direction.SOUTHWEST;
            case 6: return Direction.WEST     ;
            case 7: return Direction.NORTHWEST;
        }
        assert (false);
        return Direction.NORTH    ;
    }

    // key is used to check whether that other bot is trying to communicate with this bot.
    // use id 9999 to contact every friendly robot that can see
    static int getKey(int id){
        return ((id ^ 0xAAAA *7)^0b11011011)&0xff;
    }

//    private boolean isConversing(int id){
//        for (int conversingId : ongoingConversations[0]){
//            if (id==conversingId){
//                return true;
//            }
//        }
//        return false;
//    }
//
//    private int findEmptyConversationSlot(){
//        for(int i=0;i<10;i++){
//            if (ongoingConversations[2][i]==0){
//                return i;
//            }
//        }
//        //todo: if more than 10 robots are trying to pass in a message that needs multiple rounds happen, do something.
//        assert (false);
//        return 0;
//    }

    // one function for short messages
    private int processShort(int genre, int info){
        assert ( genre>>7 == 0 );
        // information into 16 bit number
        switch (genre) {
            case 0b00000001: return (genre << 8 + info); // these
            case 0b00000010: return (genre << 8 + info); // are
            case 0b00000011: return (genre << 8 + info); // place-holders
        }
        return (genre << 8 + info);
    }

    // one function for long messages
    private int processLong(int genre, int message){
        assert ( genre>>7 == 1 );
        return ( message );
    }

    // reverse short messages
    private void reverseProcess16(int id , int message){
        // 16 bit number into usable information (kind of wasteful, but is okay to refactor later)
        if ((message)>>15 ==0 ){
            switch (message >> 8) {
                case 0b0000000:
                    break; //needs multiple rounds to pass all information.
                case 0b0000001:
                    break; //do stuff, do what ever stuff that needs to be done, don't return.
            }
        }
//        else {
//            int slot = findEmptyConversationSlot();
//            ongoingConversations[0][slot] = id;
//            ongoingConversations[1][slot] = message >> 8;
//            ongoingConversations[2][slot] = message & 0x00ff;
//        }
    }

    // reverse long messages
    private void reverseProcess24(int genre, int message){
        // 24 bit number into usable information (in case some information needs multiple turns)
        switch (genre){
            case 0b10000000: break; // do stuff, with the message
            case 0b10000001: break; // do stuff, with the message 1
            case 0b10000010: break; // do stuff, with the message 2 these are just place holders
        }
    }

    //
    public void sendPath(int id, Direction[] path) throws GameActionException {
        int[] path_value = new int[path.length/8+1];
        for (int i =0; i<path.length; i++){
            path_value[i/8]+= Coms.directionToInt(path[i])  <<(3*(i%8) );
        }
        queueSignal(id,0b10000001,path_value);
    }

    public Direction[] receivePath(int id) throws GameActionException {
        int path_value=0;
        if (rc.canGetFlag(id)){
            path_value = rc.getFlag(id);
        }else{
            assert (false);
        }

        Direction[] path = new Direction[8];

        for (int i =0; i<8; i++){
            path[i] = Coms.intToDirection( ((7<<(3*i))&path_value)>>3*i );
        }

        return path;

    }

    public void queueSignal(int id, int genre, int message){
        assert ((genre>>7)==0);
        int flag = Coms.getKey(id) << 16 + processShort(genre, message);
        signalQueue.add(flag);
    }

    public void queueSignal(int id, int genre, int[] message){
        assert ((genre>>7)==1);
        int flag = Coms.getKey(id) << 16 + processShort(genre, message.length);
        signalQueue.add(flag);
        for (int value : message) {
            signalQueue.add(processLong(genre, value));
        }
    }

    public void displayNewFlag() throws GameActionException {
        if(signalQueue.size()>0 && rc.canSetFlag(signalQueue.peek())){
            rc.setFlag( signalQueue.remove() );
        }
    }

//    public void goOverVisibleFlags() throws GameActionException {
//        int key = getKey(rc.getID());
//
//        for (RobotInfo rb : rc.senseNearbyRobots( senseRadius, rc.getTeam() ) ){
//            if (isConversing(rb.ID) || rc.canGetFlag(rb.ID)){break;}
//
//            int v = rc.getFlag(rb.ID);
//            if( v>>16 == key | v>>16 == getKey(9999) ){
//                reverseProcess16(rb.ID,v&0x00ffff);
//            }
//        }
//
//        for (int ECid : enlightenmentCenterIds){
//            if (ECid==0 || isConversing(ECid) || !rc.canGetFlag(ECid)){break;}
//
//            int v = rc.getFlag(ECid);
//            if( v>>16 == key | v>>16 == getKey(9999) ){
//                reverseProcess16(ECid,v&0x00ffff);
//            }
//        }
//    }

//    public void goOverOngoingConversation() throws GameActionException {
//        for(int i =0; i<10;i++){
//            if (ongoingConversations[2][i]!=0){
//                if (rc.canGetFlag(ongoingConversations[0][i])) {
//                    reverseProcess24(ongoingConversations[1][i], rc.getFlag(ongoingConversations[0][i]));
//                    ongoingConversations[2][i]--;
//                }else{
//                    ongoingConversations[2][i]=0;
//                }
//            }
//        }
//    }

}
