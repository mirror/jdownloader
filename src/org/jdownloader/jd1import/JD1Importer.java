package org.jdownloader.jd1import;

import java.io.File;
import java.util.Map;

import org.appwork.utils.Application;
import org.appwork.utils.IO;
import org.appwork.utils.Regex;

public class JD1Importer {

    private static File CONFIG;
    static {

        try {
            File file = Application.getResource("redirect.txt");
            if (file.exists()) {
                String redirect = IO.readFileToString(file);
                final String org = new Regex(redirect, "org=(.*)").getMatch(0);
                if (org != null) {
                    File fold = new File(new File(org).getParentFile(), "config");
                    if (fold.exists()) {
                        CONFIG = fold;
                    }
                }

            }
            if (CONFIG == null) {
                File fold = Application.getResource("config");
                if (fold.exists()) {
                    CONFIG = fold;
                }
            }

            ;
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    public boolean isAvailable() {

        return CONFIG != null;
    }

    public Map<String, Object> getHashMap(String cfg) {
        try {

            ClassLoader cl = new SandboxClassloader(CONFIG.getParentFile());

            Class<?> clazz = cl.loadClass(JD1ImportSandbox.class.getName());
            Map<String, Object> map = (Map<String, Object>) clazz.getMethod("getSubConfigurationHashMap", new Class[] { File.class, String.class }).invoke(null, new Object[] { CONFIG.getParentFile(), cfg });

            return map;

        } catch (Throwable e) {
            e.printStackTrace();
        }
        return null;
    }
}
