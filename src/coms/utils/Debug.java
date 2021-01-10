package coms.utils;

import static coms.Robot.debugOn;

public class Debug {
    static void debug(String msg) {
        if (debugOn) System.out.println(msg);
    }
}
