package org.jdownloader.gui.swt.widgets;

import org.appwork.utils.logging2.LogSource;
import org.appwork.utils.os.CrossSystem;
import org.jdownloader.logging.LogController;

public class NativeUtils {

    private static boolean INIT = false;

    public synchronized static void init() {
        if (!INIT) {
            try {
                if (!CrossSystem.isRaspberryPi()) {
                    final LogSource LOGGER = LogController.getInstance().getLogger(NativeUtils.class.getName());
                    long start = System.currentTimeMillis();
                    try {

                        chrriis.dj.nativeswing.swtimpl.NativeInterface.open();
                        chrriis.dj.nativeswing.swtimpl.NativeInterface.runEventPump();
                        LOGGER.info("Native Init took: " + (System.currentTimeMillis() - start) + " ms");
                    } catch (Throwable e) {
                        LOGGER.log(e);
                    } finally {
                        LOGGER.close();
                    }
                }
            } finally {
                INIT = true;
            }
        }
    }
}
