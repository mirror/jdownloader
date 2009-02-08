//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.update;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Enumeration;

public class utils {

    public static final int RUNTYPE_LOCAL = 1;

    public static final int RUNTYPE_LOCAL_ENV = 3;

    public static final int RUNTYPE_LOCAL_JARED = 2;

    private static final int RUNTYPE_WEBSTART = 0;

    public static Object loadObject(File fileInput) {

        Object objectLoaded = null;
        if (fileInput != null) {

            try {
                FileInputStream fis = new FileInputStream(fileInput);
                ObjectInputStream ois = new ObjectInputStream(fis);
                objectLoaded = ois.readObject();
                ois.close();
                return objectLoaded;
            } catch (Exception e) {
                // e.printStackTrace();
            }
        }
        return null;

    }

    public static String getLocalFile(File file) {
        if (!file.exists()) { return ""; }
        BufferedReader f;
        try {
            f = new BufferedReader(new FileReader(file));

            String line;
            StringBuilder ret = new StringBuilder();
            String sep = System.getProperty("line.separator");
            while ((line = f.readLine()) != null) {
                ret.append(line + sep);
            }
            f.close();
            return ret.toString();
        } catch (IOException e) {

            e.printStackTrace();
        }
        return "";
    }

    public static void saveObject(Object objectToSave, File fileOutput) {

        if (fileOutput != null) {

            if (fileOutput.exists()) {
                fileOutput.delete();
            }
            try {
                FileOutputStream fos = new FileOutputStream(fileOutput);

                ObjectOutputStream oos = new ObjectOutputStream(fos);
                oos.writeObject(objectToSave);
                oos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }

    public static File getJDHomeDirectoryFromEnvironment() {
        String envDir = null;// System.getenv("JD_HOME");
        File currentDir = null;

        String dir = Thread.currentThread().getContextClassLoader().getResource("jd/update/Main.class") + "";
        dir = dir.split("\\.jar\\!")[0] + ".jar";
        dir = dir.substring(Math.max(dir.indexOf("file:"), 0));
        try {
            currentDir = new File(new URI(dir));

            // logger.info(" App dir: "+currentDir+" -
            // "+System.getProperty("java.class.path"));
            if (currentDir.isFile()) {
                currentDir = currentDir.getParentFile();
            }

        } catch (URISyntaxException e) {

            e.printStackTrace();
        }

        // logger.info("RunDir: " + currentDir);

        switch (getRunType()) {
        case RUNTYPE_LOCAL_JARED:
            envDir = currentDir.getAbsolutePath();
            // logger.info("JD_HOME from current Path :" + envDir);
            break;
        case RUNTYPE_LOCAL_ENV:
            envDir = System.getenv("JD_HOME");
            // logger.info("JD_HOME from environment:" + envDir);
            break;
        default:
            envDir = System.getProperty("user.home") + System.getProperty("file.separator") + ".jd_home/";
            // logger.info("JD_HOME from user.home :" + envDir);

        }

        if (envDir == null) {
            envDir = "." + System.getProperty("file.separator") + ".jd_home/";
            System.out.println(envDir);
        }
        File jdHomeDir = new File(envDir);
        if (!jdHomeDir.exists()) {
            jdHomeDir.mkdirs();
        }
        return jdHomeDir;
    }

    public static int getRunType() {

        try {

            Enumeration<URL> en = Thread.currentThread().getContextClassLoader().getResources("jd/update/Main.class");
            if (en.hasMoreElements()) {
                String root = en.nextElement().toString();
                // logger.info(root);
                if (root.indexOf("http://") >= 0) {
                    System.out.println("Depr.: Webstart");
                    return RUNTYPE_WEBSTART;
                }
                if (root.indexOf("jar") >= 0) {
                    // logger.info("Default: Local jared");
                    return RUNTYPE_LOCAL_JARED;
                }
            }
            if (System.getenv("JD_HOME") != null) {
                if (new File(System.getenv("JD_HOME")).exists()) {
                    System.out.println("Dev.: Local splitted from environment variable");
                    return RUNTYPE_LOCAL_ENV;
                }
            }
            // logger.info("Dev.: Local splitted");
            return RUNTYPE_LOCAL;
        } catch (Exception e) {

            e.printStackTrace();
        }
        return 0;

    }

}