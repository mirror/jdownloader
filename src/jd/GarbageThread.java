package jd;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.logging.Logger;

import javax.swing.Timer;

import jd.event.ControlEvent;
import jd.event.ControlListener;
import jd.utils.JDUtilities;

public class GarbageThread implements ControlListener, ActionListener {

    private Timer cgTimer;
    private static GarbageThread INSTANCE = null;
    private Logger logger = jd.controlling.JDLogger.getLogger();

    private static final int NORMALPAUSE = 30000;
    private static final int LARGEPAUSE = 2 * 60000;

    public synchronized static GarbageThread getInstance() {
        if (INSTANCE == null) INSTANCE = new GarbageThread();
        return INSTANCE;
    }

    private GarbageThread() {
        cgTimer = new Timer(NORMALPAUSE, this);
        cgTimer.setRepeats(true);
        cgTimer.start();
        JDUtilities.getController().addControlListener(this);
    }

    public void controlEvent(ControlEvent event) {
        switch (event.getID()) {
        case ControlEvent.CONTROL_DOWNLOAD_START:
            cgTimer.stop();
            cgTimer.setDelay(LARGEPAUSE);
            cgTimer.start();
            break;
        case ControlEvent.CONTROL_DOWNLOAD_STOP:
            cgTimer.stop();
            cgTimer.setDelay(NORMALPAUSE);
            cgTimer.start();
            break;
        }

    }

    public void actionPerformed(ActionEvent arg0) {
        if (arg0.getSource() == cgTimer) {
            long before = Runtime.getRuntime().freeMemory();
            System.runFinalization();
            System.gc();
            logger.warning("GC! (Free Heap| before: " + JDUtilities.formatKbReadable(before / 1024) + " after: " + JDUtilities.formatKbReadable(Runtime.getRuntime().freeMemory() / 1024));
        }
    }

}
