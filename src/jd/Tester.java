package jd;

import java.io.File;
import java.util.regex.Pattern;

import jd.nutils.io.JDIO;

public class Tester {

    public static void main(String s[]) throws Exception {

        File dec = new File("C:/Users/Coalado/workspace/JDownloader/src/jd/plugins/decrypter");

        for (File f : dec.listFiles()) {
            if (f.getAbsolutePath().endsWith(".java") && !f.getAbsolutePath().contains(".svn")) {
                String clt = JDIO.getLocalFile(f);

                clt = Pattern.compile("public String getVersion\\(\\)\\s*?\\{.*?\\}", Pattern.DOTALL).matcher(clt).replaceAll("");

                JDIO.writeLocalFile(f, clt);
            }
        }

    }

}
