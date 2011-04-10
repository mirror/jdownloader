package jd.utils.locale;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.HashMap;
import java.util.regex.Pattern;

import jd.event.MessageEvent;
import jd.event.MessageListener;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.jackson.JacksonMapper;
import org.appwork.utils.IO;
import org.appwork.utils.Regex;

public class Converter implements MessageListener {
    static {
        // USe Jacksonmapper in this project
        JSonStorage.setMapper(new JacksonMapper());

    }

    public static void main(String[] args) throws IOException {

        new Converter().start();
    }

    private HashMap<String, String[]> old;

    public Converter() throws IOException {

        old = new HashMap<String, String[]>();

        for (File f : new File("ressourcen/jd/languages/").listFiles(new FilenameFilter() {

            public boolean accept(File dir, String name) {
                return name.endsWith(".loc");
            }
        })) {

            old.put(f.getName().substring(0, f.getName().length() - 4), Regex.getLines(IO.readFileToString(f)));

        }
        System.gc();
    }

    private SrcParser sourceParser;

    private void start() throws IOException {
        sourceParser = new SrcParser(new File("src/"));
        sourceParser.getBroadcaster().addListener(this);
        sourceParser.parse();

        convert();

    }

    private String getValueOf(String variable, String content, String clazz) {
        /*
         * Workaround for JDL prefixes with static access. Simple remove the
         * static prefix!
         */
        variable = variable.replace(clazz + ".", "");

        String[] matches = new Regex(content, variable + "\\s*=(.*?);").getColumn(0);
        if (matches.length == 0) { return null;

        }
        String ret = matches[matches.length - 1].trim();
        while (ret.startsWith("\"")) {
            ret = ret.substring(1);
        }
        while (ret.endsWith("\"")) {
            ret = ret.substring(0, ret.length() - 1);
        }

        return ret;
    }

    private String prepareContent(String content, File f, ConvertEntry c) {
        String[] stats = new Regex(content, "JDL\\.LF?\\(([^\"].*?) +").getColumn(0);

        for (String s : stats) {

            String v = this.getValueOf(s, content, f.getName().substring(0, f.getName().length() - 5));
            if (v != null) {
                System.out.println("Replaced " + s + " <" + v);
                content = content.replace(s + " + \"", "\"" + v);
                content = content.replace("\" + \"", "");
            }
        }
        int found = content.split("PREFIX").length - 2;
        return content;

    }

    private void convert() throws IOException {
        for (TInterface ti : InterfaceCache.list()) {
            System.gc();
            if (ti.getMap().size() == 0) continue;
            if (ti.getPath().toString().contains(".java")) continue;
            System.out.println("--->" + ti.getPath());

            StringBuilder sb = new StringBuilder();
            HashMap<String, HashMap<String, String>> lngfiles = new HashMap<String, HashMap<String, String>>();
            for (File f : new File("ressourcen/jd/languages/").listFiles(new FilenameFilter() {

                public boolean accept(File dir, String name) {
                    return name.endsWith(".loc");
                }
            })) {
                lngfiles.put(f.getName().substring(0, f.getName().length() - 4), new HashMap<String, String>());
            }
            for (ConvertEntry c : ti.getMap().values()) {

                String defValue = c.getValue();
                if (defValue == null || defValue.equals("null")) {
                    defValue = readValue("en", c.getKey());
                }
                String key = c.getKey().replaceAll("\\W+", "_");
                sb.append("\r\n");
                String value = defValue;
                int count = 1;
                int index = 0;
                while (true) {
                    index = value.indexOf("%s", index);
                    if (index < 0) break;

                    value = value.substring(0, index) + "%s" + count + value.substring(index + 2);
                    index += 3;
                    count++;
                }
                count--;
                sb.append("@Default(lngs = { \"en\" }, values = { \"" + value.replace("\"", "\\\"") + "\" })");
                for (String s : lngfiles.keySet()) {
                    HashMap<String, String> lsb = lngfiles.get(s);
                    String r = convert(readValue(s, c.getKey()));
                    if (r != null) lsb.put(key, r);
                }
                sb.append("\r\n");
                sb.append("String " + key + "(");
                boolean first = true;
                for (int i = 0; i < count; i++) {
                    if (!first) sb.append(", ");
                    first = false;
                    sb.append("Object s" + (i + 1));
                }
                sb.append(");");

                for (File f : c.getFiles()) {
                    String content = IO.readFileToString(f);
                    content = prepareContent(content, f, c);
                    String pkg = ti.getPath().toString().substring(4).replace("\\", ".").replace("/", ".");

                    if (!content.contains("import " + pkg + ".*")) {
                        int i = content.indexOf("import ");
                        content = content.substring(0, i) + "\r\n import " + pkg + ".*;\r\n" + content.substring(i);
                    }
                    String pat;
                    if (content.indexOf("\"" + c.getKey() + "\"") < 0) {
                        System.out.println("error");
                    }
                    int found;
                    int matches = content.split("T\\._\\.").length;
                    pat = "JDL\\.L\\(\"" + Pattern.quote(c.getKey()) + "\",\\s\"" + Pattern.quote(defValue) + "\"\\)";
                    content = content.replaceAll(pat, "T._." + key + "()");
                    found = content.indexOf("\"" + c.getKey() + "\"");
                    if (found >= 0) {
                        pat = "JDL\\.LF\\(\"" + Pattern.quote(c.getKey()) + "\",\\s\"" + Pattern.quote(defValue) + "\",";
                        content = content.replaceAll(pat, "T._." + key + "(");
                        found = content.indexOf("\"" + c.getKey() + "\"");
                        if (found >= 0) {

                            pat = "JDL\\.L\\(\"" + Pattern.quote(c.getKey()) + "\",.*?\\)";
                            content = content.replaceAll(pat, "T._." + key + "()");
                            found = content.indexOf("\"" + c.getKey() + "\"");
                            if (found >= 0) {
                                pat = "JDL\\.LF\\(\"" + Pattern.quote(c.getKey()) + "\",.*?,";
                                content = content.replaceAll(pat, "T._." + key + "(");
                                found = content.indexOf("\"" + c.getKey() + "\"");
                                if (found >= 0) {

                                    System.out.println(2);

                                }
                            }

                        }
                    }
                    f.delete();
                    IO.writeStringToFile(f, content);
                }

            }
            System.out.println(sb);
            ti.getTranslationFile().delete();

            StringBuilder sb2 = new StringBuilder();
            String pkg = ti.getPath().toString().substring(4).replace("\\", ".").replace("/", ".");
            sb2.append("package " + pkg + ";\r\n");
            sb2.append("import org.appwork.txtresource.*;\r\n");
            sb2.append("@Defaults(lngs = { \"en\"})\r\n");

            sb2.append("public interface " + ti.getClassName() + "Translation extends TranslateInterface {\r\n");
            sb2.append(sb + "\r\n");
            sb2.append("}");
            ti.getTranslationFile().getParentFile().mkdirs();
            IO.writeStringToFile(ti.getTranslationFile(), sb2.toString());
            System.out.println("Write " + ti.getTranslationFile());
            sb2 = new StringBuilder();

            sb2.append("package " + pkg + ";\r\n");
            sb2.append("import org.appwork.txtresource.TranslationFactory;\r\n");
            sb2.append("public class T {\r\n");
            sb2.append("public static final " + ti.getClassName() + "Translation _ = TranslationFactory.create(" + ti.getClassName() + "Translation.class);\r\n");
            sb2.append("}");
            ti.getShortFile().delete();
            IO.writeStringToFile(ti.getShortFile(), sb2.toString());
            System.out.println("Write " + ti.getShortFile());
            for (String s : lngfiles.keySet()) {
                HashMap<String, String> lsb = lngfiles.get(s);

                File lngF = new File("translations/" + ti.getPath().toString().substring(4) + "/" + ti.getClassName() + "Translation." + s + ".lng");
                lngF.delete();
                if (lsb.size() > 0) {
                    lngF.getParentFile().mkdirs();
                    IO.writeStringToFile(lngF, JSonStorage.toString(lsb));

                }

            }
        }
    }

    private String convert(String value) {
        if (value == null) return null;
        int count = 1;
        int index = 0;
        while (true) {
            index = value.indexOf("%s", index);
            if (index < 0) break;

            value = value.substring(0, index) + "%s" + count + value.substring(index + 2);
            index += 3;
            count++;
        }
        return value;
    }

    private String readValue(String lng, String key) throws IOException {

        for (String l : old.get(lng)) {
            l = l.trim();
            int i = l.indexOf("=");
            if (key.equals(l.substring(0, i).trim())) {
                String v = l.substring(i + 1).trim();
                return v;
            }

        }
        return null;
    }

    public void onMessage(MessageEvent event) {
        System.out.println(event.getMessage());
    }
}
