package jd;

import java.io.File;
import java.io.FileFilter;

import jd.nutils.io.JDIO;
import jd.parser.Regex;


public class Tester {
 
    public static void main(String ss[]) throws Exception {
        File dir = new File("C:\\Dokumente und Einstellungen\\Towelie\\Eigene Dateien\\Java\\pure-compiler\\src\\pure\\embedded\\");
        if (dir == null || !dir.exists()) return;
        
        File[] files = dir.listFiles(new FileFilter() {
            
            public boolean accept(File pathname) {
                return pathname.getName().matches("(IO|Pure(Boolean|Double|Integer|String))\\.java");
            }

        });
        for (File file : files) {
            String content = JDIO.getLocalFile(file);
            String[][] matches = new Regex(content, "classFile\\.addMethod\\(Modifiers\\..*?, \"(.*?)\", (.*?), (.*?)\\);").getMatches();
            System.out.println(file.getName());
            for (String[] match : matches) {
                System.out.println("ecn.addFunction(\"" + match[0] + "\", \"" + match[1] + "\", \"" + ((match[2] == null) ? match[2] : match[2].replace("new TypeDesc[] { ", "").replace(" }", "")) + "\");");
            }
            System.out.println("=============================");
        }
    }

}