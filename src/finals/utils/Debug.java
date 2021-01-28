package finals.utils;

import static quals.Robot.debugOn;

public class Debug {

    public static void p(Object msg) {
        if (debugOn) System.out.println(msg);
    }
}
