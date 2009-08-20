package jd;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.regex.Pattern;

import jd.nutils.io.JDIO;
import jd.nutils.zip.Zip;

public class Tester {

    public static void main(String s[]) throws Exception {
        
        
        File[] files = new File("c:/Users/Coalado/Desktop/pack/").listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                if (name.endsWith(".svn")) { return false; }
                return true;

            }

        });
    
        Zip zip = new Zip(files, new File("c:/Users/Coalado/Desktop/substance.zip"));
        zip.setExcludeFilter(Pattern.compile("\\.svn", Pattern.CASE_INSENSITIVE));
        zip.fillSize = 5 * 1024 * 1024 + 30000 + (int) (Math.random() * 1024.0 * 150.0);
      
            zip.zip();
    }

}
