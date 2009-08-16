package jd;

import java.io.File;
import java.util.ArrayList;

import jd.nutils.io.JDIO;

public class Tester {

    public static void main(String s[]) throws Exception {
        File file = new File("C:\\Dokumente und Einstellungen\\Towelie\\Desktop\\Uni Temp\\www.sra.uni-hannover.de");

        ArrayList<File> files = JDIO.listFiles(file);

        for (File f : files) {
            if (f.getAbsolutePath().endsWith(".ram")) {
                System.out.println(f.getAbsolutePath());
                String content = JDIO.getLocalFile(f);
                JDIO.writeLocalFile(new File(f.getAbsolutePath() + ".bak"), content);
                content = content.replace("http://www.sra.uni-hannover.de/vorlesung_online/bs/SS09/", "file:///C:/Dokumente%20und%20Einstellungen/Towelie/Desktop/Uni%20Temp/www.sra.uni-hannover.de/");
                JDIO.writeLocalFile(f, content);
            }
        }
    }

}
