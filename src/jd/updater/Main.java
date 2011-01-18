package jd.updater;

import jd.http.Browser;

import org.appwork.utils.Application;
import org.appwork.utils.swing.EDTRunner;

public class Main {

    private static WebUpdaterOptions OPTIONS;

    public static void main(String[] args) {
        Application.setApplication(".jd_home");
        init();

        parseParams(args);
        UpdaterController.getInstance().start(OPTIONS);
        if (!OPTIONS.isGuiless()) {
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
        OPTIONS = new WebUpdaterOptions();
        for (int i = 0; i < args.length; i++) {
            String p = args[i];
            if (p.trim().equalsIgnoreCase("-norestart")) {
                OPTIONS.setRestart(false);
            } else if (p.trim().equalsIgnoreCase("-guiless")) {
                OPTIONS.setGuiless(true);
            } else if (p.trim().equalsIgnoreCase("-noosfilter")) {
                OPTIONS.setDisableOsfilter(true);
            } else if (p.trim().equalsIgnoreCase("-brdebug")) {
                Browser.setGlobalVerbose(true);
            } else if (p.trim().equalsIgnoreCase("-restore")) {
                OPTIONS.setRestore(true);
            } else if (p.trim().equalsIgnoreCase("-branch")) {
                String br = args[++i];
                if (br.equalsIgnoreCase("reset")) br = null;
                OPTIONS.setBranch(br);

            }
        }
    }
}
