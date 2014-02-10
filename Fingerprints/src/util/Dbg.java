package util;

import android.util.Log;

import java.io.*;
import java.util.Date;


public class Dbg {

    public static boolean onPhone = true;
    public static boolean debugmode = true;

    private static BufferedWriter logWriter = null;

    public static Error die(String tag, String msg, Throwable e) {
        loge(tag, msg, e);
        return new Error(e);
    }

    private static void log(String msg) {
        if (logWriter == null) {
            System.out.println(new Date() + ": " + msg);
            System.out.flush();
        } else {
            try {
                logWriter.write(new Date() + ": " + msg + "\n");
                logWriter.flush();
            } catch (IOException e) {
                System.err.println(new Date() + ": " + "Couldn't write '" + msg + "' to log file");
                System.err.flush();
            } catch (Error e) {
                System.err.println(new Date() + ": " + "Couldn't write '" + msg + "' to log file");
                System.err.flush();
            }
        }
    }

    /**
     * Logs to standard out or Android logger depending on which platform we are running on according to the onPhone
     * boolean.
     *
     * @param tag
     * @param msg
     */
    public static void loge(String tag, String msg) {
        if (onPhone) {
            if (msg == null) {
                msg = "No message provided";
            }
            Log.e(tag, msg);
        } else {
            try {
                log("ERROR: "+tag+": "+msg+"\n");
            } catch (Error e){
                System.err.println(tag + ": " + msg);
                System.err.flush();
                System.err.println("(Also, could not print the above to the log file: " + e.getMessage() + ")");
                System.err.flush();
            }
        }
    }

    public static void loge(String tag, String msg, Throwable exception) {
        if (onPhone) {
            if (msg == null) {
                msg = "No message provided";
            }
            Log.e(tag, msg, exception);
        } else {
            try {
                log("ERROR: " + tag + ": " + msg + "\n");
                log(stackString(exception) + "\n");
            } catch (Error e) {
                System.err.println("ERROR: " + tag + ": " + msg);
                System.err.flush();
                exception.printStackTrace();
                System.err.println("(Also, could not print the above to the log file: " + e.getMessage() + ")");
                System.err.flush();
            }
        }
    }

    public static void logw(String tag, String msg) {
        if (onPhone) {
            if (msg == null) {
                msg = "No message provided";
            }
            Log.w(tag, msg);
        } else {
            log("WARN: " + tag + ": " + msg);
        }
    }

    public static void logw(String tag, String msg, Throwable nonCriticalException) {
        if (onPhone) {
            if (msg == null) {
                msg = "No message provided";
            }
            Log.w(tag, msg);
        } else {
            log("WARN: Non-critical error: " + tag + ": " + msg);
            log(stackString(nonCriticalException));
        }
    }

    public static void logi(String tag, String msg) {
        if (onPhone) {
            if (msg == null) {
                msg = "No message provided";
            }
            Log.i(tag, msg);
        } else {
            log("INFO: " + tag + ": " + msg);
        }
    }

    public static void logd(String tag, String msg) {
        if (onPhone) {
            if (msg == null) {
                msg = "No message provided";
            }
            Log.d(tag, msg);
        } else {
            log("DEBUG: " + tag + ": " + msg);
        }
    }

    public static void logv(String tag, String msg) {
        if (onPhone) {
            if (msg == null) {
                msg = "No message provided";
            }
            Log.v(tag, msg);
        } else {
            log("VERBOSE: "+tag+": "+msg);
        }
    }

    public static void setLogFile(File logFile) {
        if (logFile == null) {
            logWriter = null;
        }
        try {
            logWriter = new BufferedWriter(new FileWriter(logFile, true));
        } catch (IOException e) {
            logWriter = null;
            Dbg.loge("Dbg", "Couldn't create log file, using Standard Out instead", e);
        } catch (NullPointerException e) {
            logWriter = null;
            //Dbg.loge("Dbg", "Couldn't create log file, using Standard Out instead", e);
        }
    }

    private static String stackString(Throwable e) {
        StringWriter b = new StringWriter();
        e.printStackTrace(new PrintWriter(b));
        return b.toString();
    }

    /**
     * Use this method to check for bugs. The passed error will be PRINTED (not thrown)
     * if the boolean parameter evaluates to true.
     * @param tag
     * @param aBugHasOccured
     * @param errorToPrint
     */
    public static void bugHunt(String tag, boolean aBugHasOccured, Error errorToPrint) {
        if (aBugHasOccured) {
            if (debugmode) {
                throw Dbg.die(tag, "A suspected bug was detected", errorToPrint);
            } else {
                Dbg.loge(tag, "A suspected bug was detected", errorToPrint);
            }
        }
    }
}
