package jd.dynamics;

import jd.config.SubConfiguration;
import jd.controlling.DynamicPluginInterface;
import jd.nutils.OSDetector;

public class Java17Workaround extends DynamicPluginInterface {

    @Override
    public void execute() {
        new Thread("Java17Workaround") {
            public void run() {
                try {
                    if (!OSDetector.isWindows()) {
                        SubConfiguration.getConfig("jdgui").setProperty("DECORATION_ENABLED2", false);
                        javax.swing.UIManager.put("Synthetica.window.decoration", false);
                        javax.swing.JFrame.setDefaultLookAndFeelDecorated(false);
                        javax.swing.JDialog.setDefaultLookAndFeelDecorated(false);
                    }
                } catch (final Throwable e) {
                    e.printStackTrace();
                }
            }
        }.start();

    }

}
