package jd.dynamics;

import java.net.URL;

import jd.JDInitFlags;
import jd.config.SubConfiguration;
import jd.controlling.DynamicPluginInterface;
import jd.gui.UserIO;
import jd.nutils.nativeintegration.LocalBrowser;

public class UpdateWarning extends DynamicPluginInterface {

    @Override
    public void execute() {
        new Thread("Updater Warner Dynamic") {
            public void run() {
                try {
                    Thread.sleep(10000);
                } catch (final InterruptedException e1) {
                    e1.printStackTrace();
                }
                final String id = "4";
                if (SubConfiguration.getConfig("update").getGenericProperty(id, false) && JDInitFlags.SWITCH_DEBUG == false) {
                    // Schon durchgefuehrt
                    return;
                }
                SubConfiguration.getConfig("update").setProperty(id, true);

                if (UserIO.isOK(UserIO.getInstance().requestConfirmDialog(UserIO.NO_COUNTDOWN, "Java update Warning!", "A new Java version has been released recently. We discovered that upgrading \r\nprevious java versions > to java 1.6.0_26 will cause your JDownloader download \r\nlist to disappear. \r\n\r\nSo if you are planning to upgrade to java 1.6.0_26 > first complete your \r\ndownloads, empty your list, and only then continue with the java update. After the \r\nupgrade is done you can continue adding new links to your JDownloader safely.", null, "Read more", "Cancel"))) {
                    try {
                        LocalBrowser.openDefaultURL(new URL("http://www.facebook.com/jdownloader/posts/181364435253153"));
                    } catch (final Exception e) {
                        e.printStackTrace();
                    }
                }

                SubConfiguration.getConfig("update").save();

            }
        }.start();

    }

}
