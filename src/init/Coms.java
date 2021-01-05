package init;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;

public class Coms {
    static RobotController rc;
    static int senseRadius;
    static int[][] ongoingConversations = new int[2][10]; //2 cols one for id one for genre. 10 rows for accepting 10 inputs at ones.



    public Coms(RobotController r){
        rc=r;
        senseRadius=getSenseRadius();
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

    static int getSenseRadius(){
        switch (rc.getType()){
            case ENLIGHTENMENT_CENTER: return 40;
            case POLITICIAN:           return 25;
            case SLANDERER:            return 20;
            case MUCKRAKER:            return 30;
        }
        assert (false);
        return 0;
    }

    static boolean isConversing(int id){
        for (int conversingId : ongoingConversations[0]){
            if (id==conversingId){
                return true;
            }
        }
        return false;
    }

    // key is used to check whether that other bot is trying to communicate with this bot.
    // use id 9999 to contact every friendly robot that can see
    static int getKey(int id){
        return (rc.getID() ^ rc.getRoundNum() *7)^0b11011011;
    }

    static int findEmptyConversationSlot(){
        for(int i=0;i<10;i++){
            if (ongoingConversations[0][i]==0){
                return i;
            }
        }
        //todo: if more than 10 robots are trying to pass in a message that needs multiple rounds happen, do something.
        assert (false);
        return 0;
    }

    static int process(int id, int genre, int info){
        // information into 16 bit number
        switch (genre) {
            case 0b00000001: return (genre << 8 + info); // these
            case 0b00000010: return (genre << 8 + info); // are
            case 0b00000011: return (genre << 8 + info); // place-holders
        }
        assert (false);
        return -1;
    }

    static int process(int id, int genre){
        assert ( genre>>7 == 1 );
        return process(id,genre,0);
    }

    static void reverseProcess16(int id , int message){
        // 16 bit number into usable information (kind of wasteful, but is okay to refactor)
        switch (message>>8){
            case 0b00000000: break; //needs multiple rounds to pass all information.
            case 0b00000001: break; //do stuff, do what ever stuff that needs to be done, don't return.
        }
        int slot=findEmptyConversationSlot();
        ongoingConversations[0][slot] = id;
        ongoingConversations[1][slot] = message&0xff;
    }

    static void reverseProcess24(int genre, int message){
        // 24 bit number into usable information (in case some information needs multiple turns)
        switch (genre){
            case 0b10000000: break; // do stuff, with the message
            case 0b10000001: break; // do stuff, with the message 1
            case 0b10000010: break; // do stuff, with the message 2 these are just place holders
        }
    }


    static void sendPath(Direction[] path) throws GameActionException {
        int path_value = 0;
        for (int i =0; i<8; i++){
            path_value+=directionToInt(path[i])  <<(3*i);
        }
        if(rc.canSetFlag(path_value)){
            rc.setFlag(path_value);
        }
    }

    static Direction[] receivePath(int id) throws GameActionException {
        int path_value=0;
        if (rc.canGetFlag(id)){
            path_value = rc.getFlag(id);
        }else{
            assert (false);
        }

        Direction[] path = new Direction[8];

        for (int i =0; i<8; i++){
            path[i] = intToDirection( ((7<<(3*i))&path_value)>>3*i );
        }

        return path;

    }

    //todo: somehow split the data into multiple chunks and queue up the transmission

    public static void establishConversationRequest(int id, int genre, int info) throws GameActionException {
        int flag=getKey(id)<<16 + process(id,genre,info);

        if(rc.canSetFlag(flag)){
            rc.setFlag( flag );
        }
    }

    public static void receiveConversationRequest() throws GameActionException {
        int key = getKey(rc.getID());

        for (RobotInfo rb : rc.senseNearbyRobots( senseRadius, rc.getTeam() ) ){
            if (isConversing(rb.ID)){break;}

            int v = rc.getFlag(rb.ID);
            if( v>>16 == key | v>>16 == getKey(9999) ){
                reverseProcess16(rb.ID,v&0x00ffff);
            }
        }

    }

    public static void goingOverOngoingConversation() throws GameActionException {
        for(int i =0; i<10;i++){
            if (ongoingConversations[0][i]!=0){
                if (rc.canGetFlag(ongoingConversations[0][i])) {
                    reverseProcess24(ongoingConversations[1][i], rc.getFlag(ongoingConversations[0][i]));
                }else{
                    ongoingConversations[0][i]=0;
                }
            }
        }
    }
}
