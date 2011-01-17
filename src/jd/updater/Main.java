package jd.updater;

import jd.http.Browser;
import jd.update.WebUpdater;

import org.appwork.storage.JSonStorage;
import org.appwork.utils.Application;
import org.appwork.utils.swing.EDTRunner;

public class Main {
    private static boolean NO_RESTART;
    private static boolean GUILESS = false;
    private static boolean DISABLE_OSFILTER;
    private static boolean RESTORE;

    public static void main(String[] args) {
        Application.setApplication(".jd_home");
        init();
        parseParams(args);
        UpdaterController.getInstance().start();
        if (!GUILESS) {
            new EDTRunner() {

                @Override
                protected void runInEDT() {
                    UpdaterGui.getInstance().start();
                }
            };
        }
    }

    private static void init() {
        // only use ipv4, because debian changed default stack to ipv6
        System.setProperty("java.net.preferIPv4Stack", "true");
    }

    private static void parseParams(String[] args) {
        for (int i = 0; i < args.length; i++) {
            String p = args[i];
            if (p.trim().equalsIgnoreCase("-norestart")) {
                NO_RESTART = true;
            } else if (p.trim().equalsIgnoreCase("-guiless")) {
                GUILESS = true;
            } else if (p.trim().equalsIgnoreCase("-noosfilter")) {
                DISABLE_OSFILTER = false;

            } else if (p.trim().equalsIgnoreCase("-brdebug")) {
                Browser.setGlobalVerbose(true);
            } else if (p.trim().equalsIgnoreCase("-restore")) {
                RESTORE = true;

            } else if (p.trim().equalsIgnoreCase("-branch")) {
                String br = args[++i];
                if (br.equalsIgnoreCase("reset")) br = null;

                JSonStorage.getPlainStorage("WEBUPDATE").put(WebUpdater.PARAM_BRANCH, br);
                JSonStorage.getPlainStorage("WEBUPDATE").save();
                System.out.println("Switched branch: " + br);
            }
        }
    }
}
