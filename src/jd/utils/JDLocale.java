//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team jdownloader@freenet.de
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.utils;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Vector;

import jd.JDFileFilter;
import jd.gui.skins.simple.SimpleGUI;
import jd.http.Encoding;
import jd.http.PostRequest;
import jd.nutils.io.JDIO;
import jd.parser.Regex;

public class JDLocale {

    private static HashMap<String, String> data = new HashMap<String, String>();

    private static HashMap<String, String> defaultData = new HashMap<String, String>();

    private static final String DEFAULTLANGUAGE = "english";

    private static String LANGUAGES_DIR = "jd/languages/";

    private static String localeID;

    private static File localeFile;

    public static String getLocale() {
        return localeID;
    }

    public static File getLanguageFile() {
        return localeFile;
    }

    public static String getTranslater() {
        return JDLocale.L("$translater$", "JD-Team");
    }

    public static String getVersion() {
        String info = JDLocale.L("$version$", "0.0");
        String ret = JDUtilities.getVersion(info);
        return (ret.equals("0.0")) ? info : ret;
    }

    public static Vector<String> getLocaleIDs() {
        File dir = JDIO.getResourceFile(LANGUAGES_DIR);
        if (!dir.exists()) { return null; }
        File[] files = dir.listFiles(new JDFileFilter(null, ".lng", false));
        Vector<String> ret = new Vector<String>();
        for (File element : files) {
            ret.add(element.getName().split("\\.")[0]);
        }
        return ret;
    }

    public static String getLocaleString(String key, String def) {
        if (data == null || localeFile == null) {
            JDLocale.setLocale(JDUtilities.getSubConfig(SimpleGUI.GUICONFIGNAME).getStringProperty(SimpleGUI.PARAM_LOCALE, JDLocale.isGerman() ? "german" : "english"));
        }

        key = key.toLowerCase();
        if (data.containsKey(key)) return data.get(key);

        System.out.println("Key not found: " + key);
        if (def == null) def = key;
        if (defaultData.containsKey(key)) {
            def = defaultData.get(key);
        }
        data.put(key, def);

        return def;

    }

    private static boolean isGerman() {
        return System.getProperty("user.country") != null && System.getProperty("user.country").equalsIgnoreCase("DE");
    }

    public static String L(String key, String def) {
        return JDLocale.getLocaleString(key, def);
    }

    /**
     * Wrapper für String.format(JDLocale.L(..),args)
     * 
     * @param key
     * @param def
     * @param args
     * @return
     */
    public static String LF(String key, String def, Object... args) {
        return String.format(L(key, def), args);
    }

    public static void parseLanguageFile(File file, HashMap<String, String> data) {
        JDLocale.parseLanguageFile(file, data, true);
    }
    
    public static void parseLanguageFile(File file, HashMap<String, String> data, boolean format) {
        data.clear();

        if (!file.exists()) {
            System.out.println("JDLocale: " + file + " not found");
            return;
        }

        String[] lines = Regex.getLines(JDIO.getLocalFile(file));
        String key;
        String value;
        for (String line : lines) {
            if (line.startsWith("#")) continue;
            int split = line.indexOf("=");
            if (split <= 0) continue;

            key = line.substring(0, split).trim().toLowerCase();
            value = line.substring(split + 1).trim() + (line.endsWith(" ") ? " " : "");
            if (format) value = value.replace("\\r", "\r").replace("\\n", "\n");
            // value = Encoding.UTF8Decode(value);
            data.put(key, value);
        }

    }

    public static void setLocale(String lID) {
        if (data != null && localeFile != null) return;

        localeID = lID;
        localeFile = JDIO.getResourceFile(LANGUAGES_DIR + localeID + ".lng");
        File defaultFile = JDIO.getResourceFile(LANGUAGES_DIR + DEFAULTLANGUAGE + ".lng");

        if (!localeFile.exists()) {
            System.out.println("Language " + localeID + " not installed");
            return;
        }

        JDLocale.parseLanguageFile(localeFile, data);

        if (defaultFile.exists()) {
            JDLocale.parseLanguageFile(defaultFile, defaultData);
        } else {
            System.out.println("Could not load the default languagefile: " + defaultFile);
        }

    }

    public static String translate(String to, String msg) {
        return translate("auto", to, msg);
    }

    public static String translate(String from, String to, String msg) {

        try {
            PostRequest r = new PostRequest("http://translate.google.com/translate_t?sl=" + from + "&tl=" + to);

            r.setPostVariable("hl", "de");
            r.setPostVariable("text", msg);
            r.setPostVariable("sl", from);
            r.setPostVariable("tl", to);
            r.setPostVariable("ie", "UTF8");

            return Encoding.UTF8Decode(Encoding.htmlDecode(new Regex(r.load(), "<div id\\=result_box dir\\=\"ltr\">(.*?)</div>").getMatch(0)));
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

    }

}