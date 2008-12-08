package Parser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SourceParser {

    private static String getLocalFile(File file) {
        if (!file.exists()) return "";
        BufferedReader f;
        try {
            f = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"));

            String line;
            StringBuffer ret = new StringBuffer();
            String sep = "\r\n";
            while ((line = f.readLine()) != null) {
                ret.append(line + sep);
            }
            f.close();
            return ret.toString();
        } catch (IOException e) {

        }
        return "";
    }

    private static void parseAdditon(File file, String regexp) {
        String text = getLocalFile(file);
        text = text.replaceAll("(?is)/\\*.*?\\*/", "");
        text = text.replaceAll("//.*", "");
        text = text.replaceAll(".*final .*", ""); // kann man nich verändern
        Matcher r = Pattern.compile(regexp).matcher(text);
        if (r.find()) System.out.println(file);
    }

    private static Vector<File> getFiles(File directory) {
        Vector<File> ret = new Vector<File>();

        for (File file : directory.listFiles()) {
            if (file.isDirectory()) {
                ret.addAll(getFiles(file));
            } else if (file.getName().matches(".*\\.java$")) {
                ret.add(file);
            }
        }

        return ret;
    }

    public static void parse(String regexp) {
        for (File file : getFiles(new File("src"))) {
            parseAdditon(file, regexp);
        }
    }

    public static void main(String[] args) {
        parse("system.update.error.message");
    }

}
