package org.jdownloader.updatev2.restart;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.appwork.utils.Application;

public class MacRestarter extends LinuxRestarter {
    protected File getRunInDirectory(File root) {
        File app = getApp(root);
        if (app.exists()) {
            return app.getParentFile();
        } else {
            return super.getRunInDirectory(root);
        }
    }

    @Override
    protected List<String> getApplicationStartCommands(File root) {
        ArrayList<String> lst = new ArrayList<String>();
        File app = getApp(root);
        if (app.exists()) {

            lst.add("open");
            lst.add("-n");
            lst.add(app.getAbsolutePath());
            getLogger().info(lst + "");
            return lst;
        } else {
            getLogger().warning("MAX " + app + " is missing");

        }
        // user Fallback
        return super.getApplicationStartCommands(root);
    }

    private File getApp(File root2) {
        String appname = "JDownloader.app";
        File apppath = new File(root2, "../../../../");
        try {
            getLogger().info("Look in " + apppath + " - " + apppath.getCanonicalPath());
        } catch (IOException e1) {
            e1.printStackTrace();
        }
        try {

            File root = Application.getApplicationRoot();
            final HashSet<File> loopMap = new HashSet<File>();
            while (root != null && loopMap.add(root)) {
                if (root.getName().endsWith(".app")) {
                    apppath = root.getParentFile();
                    appname = root.getName();
                    getLogger().info("Found App: " + apppath);
                    break;

                }
                root = root.getParentFile();

            }

            if (root != null) {
                getLogger().info("APPNAME " + appname + " - " + root.getCanonicalPath());
            }
        } catch (final IOException e) {

            getLogger().log(e);
        }
        return new File(apppath, appname);

    }

}
