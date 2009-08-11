package jd;

import java.io.File;

import jd.nutils.io.JDIO;

public class Tester {

    public static void main(String s[]) throws Exception {

        File file = new File("C:/Users/Coalado/workspace/JDownloader/src/jd/plugins/decrypter");

        for (File f : file.listFiles()) {
            if (f.getAbsolutePath().contains("svn") || !f.getAbsolutePath().contains(".java")) continue;

            String contents = JDIO.getLocalFile(f);
            String clname = f.getName().replace(".java", "");

            String clean = "";

            for (int i = 0; i < clname.length(); i++) {
                String c = clname.charAt(i) + "";
                switch (c.toLowerCase().charAt(0)) {
                case 'a':
                case 'e':
                case 'u':
                case 'i':
                case 'o':
                case 'j':
                case 'y':
                    continue;

                default:

                    if (clean.length() == 0) {
                        clean += c.toUpperCase();
                    } else {
                        clean += clname.charAt(i);
                    }

                }
            }
            contents = contents.replace(clname, clean);
            if (clname.equalsIgnoreCase(clean)) {
                continue;

            }

            File newFile = new File(file, clean + ".java");
            if (!newFile.exists()) {
                f.renameTo(newFile);
                JDIO.writeLocalFile(newFile, contents);
            }
        }

    }

}
