package jd;
import javax.swing.RepaintManager;
import javax.swing.SwingUtilities;

import debug.EDT_ThreadHangMonitor;
import debug.EDT_ViolationsDetector;


public class Main_EDT_Debug {
    
    public static void main(String[] args) {
        // Start VM with -Xmx256M
        // JD won't print out shit if it hasn't enough RAM...cheaky bastard.
        DebugEDTViolations();
        
        // Vergesst DebugThreadHangs erstmal. EDT violations sind wichtiger und vor allem haeufiger.
        // DebugThreadHangs();
    }

    /**
     * Used to detect Event Dispatch Thread rule violations<br>
     * See <a href=
     * "http://java.sun.com/docs/books/tutorial/uiswing/misc/threads.html">How
     * to Use Threads</a> for more info
     */
    private static void DebugEDTViolations() {
        RepaintManager.setCurrentManager(new EDT_ViolationsDetector());
        // JD is now started in the EDT. Still later on it can make calls to
        // Swing methods outside of the EDT. The Program will detect this and
        // print a warning on the console giving you the code-location where
        // this violation happened.        
        // Solution: You have to wrap the calls to Swing
        // methods in a Runnable and give it to SwingUtilities.invokeLater(...)
        // to invoke the calls to the Swing methods in the EDT, as we did below.
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                jd.Main.main(new String[]{});
            }
        });
        
        // Example: setVisible is a Swing method. Calling it like this (outside
        // EDT) will print an error to the console. Comment it out to remove
        // this problem.
        // new JFrame().setVisible(true);
    }
    
    
    /**
     * Monitors the AWT event dispatch thread for events that take longer than
     * a certain time to be dispatched. The principle is to record the time
     * at which we start processing an event, and have another thread check
     * frequently to see if we're still processing. If the other thread notices
     * that we've been processing a single event for too long, it prints a stack
     * trace showing what the event dispatch thread is doing, and continues to
     * time it until it finally finishes. This is useful in determining
     * what code is causing your Java application's GUI to be unresponsive.
     */
    private static void DebugThreadHangs() {
        EDT_ThreadHangMonitor.initMonitoring();
        
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                jd.Main.main(new String[]{});
            }
        });
    }

}
