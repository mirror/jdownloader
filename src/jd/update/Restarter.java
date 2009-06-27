package jd.update;

import java.io.File;

import jd.nutils.Executer;
import jd.nutils.OSDetector;

public class Restarter {

    private static boolean RESTART = false;
  
    /**
     * @param args
     */
    public static void main(String[] args) {
        for (String arg : args) {
            if (arg.equalsIgnoreCase("-restart")) RESTART = true;
        }

        String master = new File("JDownloader.jar").getAbsolutePath();
        while (new File("JDownloader.jar").exists() && !new File("JDownloader.jar").canWrite()) {
            System.out.println("Wait for jdownloader terminating");
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        move(new File("update"));

        if (RESTART) {
            if (OSDetector.isMac()) {
                Executer exec = new Executer("open");
                exec.addParameters(new String[] { "-n", "jDownloader.app" });
                exec.setRunin(new File(".").getAbsolutePath());
                exec.start();

            } else {

                Executer exec = new Executer("java");
                exec.addParameters(new String[] { "-jar", "-Xmx512m", "JDownloader.jar", "-rtfu2" });
                exec.setRunin(new File(".").getAbsolutePath());
                exec.start();

            }
        }

    }

    public static void move(File dir) {
        if (!dir.isDirectory()) return;

        for (File f : dir.listFiles()) {
            if (f.isDirectory()) {
                move(f);

            } else {

                String n = new File("update").getAbsolutePath();
                File newFile = new File(f.getAbsolutePath().replace(n, "").substring(1));
                System.out.println(newFile.getAbsolutePath());
                newFile.delete();
                newFile.getParentFile().mkdirs();
                f.renameTo(newFile);
                if (f.getParentFile().list().length == 0) f.getParentFile().delete();
            }
        }
        if (dir.list() != null) {
            if (dir.list().length == 0) dir.delete();
        }

    }
}
