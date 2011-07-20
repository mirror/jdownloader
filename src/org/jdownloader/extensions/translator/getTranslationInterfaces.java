package org.jdownloader.extensions.translator;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.appwork.utils.Application;
import org.appwork.utils.Files;
import org.appwork.utils.logging.Log;

public class getTranslationInterfaces {

    /**
     * @param args
     */
    private static void getSVNTranslations() {

    }

    public static void main(String[] args) {
        Application.setApplication(".jd_home");

        getSVNTranslations();

        String wd = Application.getRoot();

        File ojt = new File(wd);

        System.out.println("OJT: " + ojt.getAbsolutePath());
        ArrayList<File> list = Files.getFiles(true, true, ojt.listFiles());

        ArrayList<String> keys = new ArrayList<String>();

        for (File f : list) {
            if (!f.getName().endsWith("en.lng")) continue;

            System.out.println("\t" + f.getAbsolutePath());

            Pattern jsonKeyPattern = Pattern.compile("\\\"(.+?)\\\"");

            try {
                BufferedReader br = new BufferedReader(new InputStreamReader(new DataInputStream(new FileInputStream(f.getAbsolutePath()))));
                String line;
                while ((line = br.readLine()) != null) {
                    Matcher m = jsonKeyPattern.matcher(line);
                    if (m.find()) {
                        keys.add(m.group(1));
                    }
                }

            } catch (FileNotFoundException e) {
                Log.L.warning("Translation file missing: " + f.getAbsolutePath());
            } catch (IOException e) {
                Log.L.warning("Translation file cannot be read: " + f.getAbsolutePath());
            }

        }

        for (String k : keys)
            System.out.println(k);

        return;
    }
}
