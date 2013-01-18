package org.jdownloader.updatev2.restart;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.appwork.utils.Application;

public class MacRestarter extends LinuxRestarter {
    protected File getRunInDirectory() {
        File app = getApp();
        if (app.exists()) {
            return app.getParentFile();
        } else {
            return super.getRunInDirectory();
        }
    }

    @Override
    protected List<String> getApplicationStartCommands() {
        ArrayList<String> lst = new ArrayList<String>();
        File app = getApp();
        if (app.exists()) {

            lst.add("open");
            lst.add("-n");
            lst.add(app.getAbsolutePath());

            return lst;
        } else {
            getLogger().warning("MAX " + app + " is missing");

        }
        // user Fallback
        return super.getApplicationStartCommands();
    }

    private File getApp() {
        String appname = "JDownloader.app";
        File apppath = Application.getResource("../../../../");
        try {

            File root = Application.getApplicationRoot();
            final HashSet<File> loopMap = new HashSet<File>();
            while (root != null && loopMap.add(root)) {
                if (root.getName().endsWith(".app")) {
                    apppath = root.getParentFile();
                    appname = root.getName();
                    getLogger().finer("Found App: " + apppath);
                    break;

                }
                root = root.getParentFile();

            }

            if (root != null) {
                System.out.println("APPNAME " + appname + " - " + root.getCanonicalPath());
            }
        } catch (final IOException e) {

            getLogger().log(e);
        }
        return new File(apppath, appname);

    }

}
