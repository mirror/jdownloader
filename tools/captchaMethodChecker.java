import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Vector;

import jd.captcha.JACMethod;
import jd.parser.Regex;
import jd.utils.JDUtilities;

public class captchaMethodChecker {
    public static int count = 0;
    public static int count2 = 0;

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

    private static String getHost(String source) {
        return new Regex(source, "names\\s*=\\s*\\{\\s*\"([^\"]*)\"\\s*\\}").getMatch(0);
    }

    private static String getMethode(String host) {
        for (JACMethod method : JACMethod.getMethods()) {
            if (host.equalsIgnoreCase(method.getServiceName())) { return method.getFileName(); }
        }
        File dir = JDUtilities.getResourceFile(JDUtilities.getJACMethodsDirectory() + host);
        if (dir.exists()) return host;
        return null;
    }

    private static void parseAdditon(File file, String regexp) {
        String text = getLocalFile(file);
        // text = text.replaceAll("(?is)/\\*.*?\\*/", "");
        // text = text.replaceAll("//.*", "");
        // text = text.replaceAll(".*final .*", ""); // kann man nich verändern
        boolean showPluginsWithountMethod = true;
        String[] res = new Regex(text, regexp).getRow(0);

        if (res != null && res.length > 0) {
            String[] lines = text.split("(\\r\\n)|\\r|\\n");
            int ln = 0;
            for (; ln < lines.length; ln++) {
                String string = lines[ln];
                if (string.contains("getCaptchaCode"))
                {
                    ln ++;
                    break;
                }
            }
            String host = getHost(text);
            for (String string : res) {
                if (string.split(",").length == 2) {
                    String meth = getMethode(host);

                    if (showPluginsWithountMethod) {
                        if (meth == null) {
                            //new line in print cause System.out.println ignores System.err.println
                            System.out.print(host+"\r\n");
                            if (text.toLowerCase().contains("recaptcha")) System.out.print("ReCaptcha\r\n");

                            System.err.print("getCaptchaCode at (" + file.getName() + ":" + ln + ")\r\n");
                            // System.out.println("");
                            // System.out.println(string);
                            System.out.println("__________________________");
                            count2++;
                        } else
                            count++;
                    } else {
                        if (meth != null) {
                            System.out.println(host);
                            System.out.println("Methodname: " + meth);
                            System.out.println("__________________________");
                        }
                    }
                } else {
                    String host2 = string.replaceFirst(".*?\"", "").replaceFirst("\".*", "");
                    String meth = getMethode(host2);
                    if (showPluginsWithountMethod) {

                        if (meth == null) {
                            System.out.print(host+"\r\n");
                            if (text.toLowerCase().contains("recaptcha"))
                                System.out.print("ReCaptcha\r\n");
                            else if (host != null && !host.equals(host2)) System.out.print(host2+"\r\n");
                            // System.out.println(); //

                            System.err.print("getCaptchaCode at (" + file.getName() + ":" + ln + ")\r\n");
                            // System.out.println(string);
                            System.out.println("__________________________");
                            count2++;
                        } else
                            count++;
                    } else {
                        if (meth != null) {
                            System.out.println(host2);
                            System.out.println("Methodname: " + meth);
                            System.out.println("__________________________");
                        }
                    }

                }
            }

        }
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
        System.out.println("Hoster:");
        System.out.println(":::::::::::::::::");

        for (File file : getFiles(new File("src/jd/plugins/hoster"))) {
            parseAdditon(file, regexp);
        }
        System.out.println("");
        System.out.println("");
        System.out.println("Decrypter:");
        System.out.println(":::::::::::::::::");
        for (File file : getFiles(new File("src/jd/plugins/decrypter"))) {
            parseAdditon(file, regexp);
        }

    }

    public static void main(String[] args) {
        parse("(getCaptchaCode\\(.*?\\);)");
        System.out.println(count);
        System.out.println(count2);

    }

}
