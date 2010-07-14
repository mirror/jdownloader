package jd.dynamics;

import java.io.File;
import java.util.ArrayList;

import jd.controlling.AccountController;
import jd.controlling.DownloadWatchDog;
import jd.controlling.DynamicPluginInterface;
import jd.gui.UserIO;
import jd.nutils.JDFlags;
import jd.nutils.JDHash;
import jd.plugins.Account;
import jd.plugins.PluginForHost;
import jd.utils.JDTheme;
import jd.utils.JDUtilities;
import jd.utils.WebUpdate;
import jd.utils.locale.JDL;

public class RSUpdateNotifier extends DynamicPluginInterface {

    @Override
    public void execute() {
        try {
            PluginForHost old = JDUtilities.getPluginForHost("rapidshare.com");
            if (old != null && old.getVersion() != null) {
                long version = Long.parseLong(old.getVersion());
                final ArrayList<Account> ret = AccountController.getInstance().getAllAccounts("rapidshare.com");
                if (version < 12008) {
                    if (ret.size() > 0) {
                        new Thread(new Runnable() {
                            public void run() {
                                int tries = 30;
                                while (true) {
                                    try {
                                        File rsFile = JDUtilities.getResourceFile("jd/plugins/hoster/Rapidshare.class");
                                        String hash = JDHash.getMD5(rsFile);
                                        if (hash.equalsIgnoreCase("21ea8a82c865739ca87059552aac7e39")) break;
                                    } catch (Throwable e) {
                                    }
                                    try {
                                        Thread.sleep(1000);
                                    } catch (InterruptedException e) {
                                    }
                                    tries--;
                                    if (tries == 0) break;
                                }
                                int answer = UserIO.getInstance().requestConfirmDialog(UserIO.NO_COUNTDOWN, JDL.L("system.dialogs.update", "Updates available"), "RapidShare Hotfix for disabled Premium accounts available! JDownloader needs a restart to update RapidShare plugin. You have to enable Premium accounts again after restart. Restart now?", JDTheme.II("gui.images.update", 32, 32), null, null);
                                if (JDFlags.hasAllFlags(answer, UserIO.RETURN_OK)) {
                                    if (tries <= 0) {
                                        WebUpdate.doUpdateCheck(false);
                                        try {
                                            Thread.sleep(20 * 1000);
                                        } catch (InterruptedException e) {
                                        }
                                    }
                                    DownloadWatchDog.getInstance().stopDownloads();
                                    JDUtilities.restartJDandWait();
                                }
                            }
                        }).start();
                    }
                } else {
                    if (!JDUtilities.getConfiguration().getBooleanProperty("rshotfixreenabled", false)) {
                        if (ret.size() > 0) {
                            int answer = UserIO.getInstance().requestConfirmDialog(UserIO.NO_COUNTDOWN, "RapidShare HotFix", "RapidShare Hotfix for disabled Premium accounts available! JDownloader enables all available Premium accounts cause they might got disabled. Proceed?", JDTheme.II("gui.images.update", 32, 32), null, null);
                            if (JDFlags.hasAllFlags(answer, UserIO.RETURN_OK)) {
                                for (Account acc : ret) {
                                    acc.setEnabled(true);
                                }
                            }
                        }
                        JDUtilities.getConfiguration().setProperty("rshotfixreenabled", Boolean.TRUE);
                        JDUtilities.getConfiguration().save();
                    }

                }
            }
        } catch (Throwable e) {
        }
    }
}
