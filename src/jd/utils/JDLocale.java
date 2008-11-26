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
import jd.parser.Regex;
import jd.utils.io.JDIO;

/**
 * Diese Klasse stellt Methoden zur Verfügung um in einen String mit
 * Platzhaltern werte einzusetzen
 */
public class JDLocale {

    public static final String LOCALE_EDIT_MODE = "LOCALE_EDIT_MODE";

    private static HashMap<String, String> data = new HashMap<String, String>();

    private static HashMap<String, String> defaultData = new HashMap<String, String>();

//    private static HashMap<String, String> missingData = new HashMap<String, String>();

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
        data.put(key, Encoding.UTF8Encode(def));
//        missingData.put(key, Encoding.UTF8Encode(def));

//        JDLocale.saveData(new File(localeFile.getAbsolutePath() + ".extended"), data);
//        JDLocale.saveData(new File(localeFile.getAbsolutePath() + ".missing"), missingData);
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

    private static HashMap<String, String> parseLanguageFile(File file) {
        HashMap<String, String> dat = new HashMap<String, String>();
        if (!file.exists()) {
            System.out.println("JDLocale: " + file + " not found");
            return dat;
        }
        String str = JDIO.getLocalFile(file);
        String[] lines = Regex.getLines(str);
       
        for (String element : lines) {
            int split = element.indexOf("=");
            if (split <= 0 || element.startsWith("#")) {
                continue;
            }
            String key = element.substring(0, split).trim().toLowerCase();
            String value = element.substring(split + 1).trim() + (element.endsWith(" ") ? " " : "");
            value = value.replace("\\r", "\r").replace("\\n", "\n");
            value = Encoding.UTF8Decode(value);
            if (dat.containsKey(key)) {
                System.out.println("Dupe found: " + key);
                dat.put(key, value);
               
            } else {
                dat.put(key, value);
            }

        }
//        if (dupes) {
//            System.out.println("Duplicate entries found in " + file + ". Wrote fixed Version to " + new File(file.getAbsolutePath() + ".nodupes"));
//            JDLocale.saveData(new File(file.getAbsolutePath() + ".nodupes"), dat);
//        }
        return dat;
    }

//    private static void saveData(File lc, HashMap<String, String> dat) {
//
//        if (!JDUtilities.getSubConfig(SimpleGUI.GUICONFIGNAME).getBooleanProperty(JDLocale.LOCALE_EDIT_MODE, false)) return;
//
//        if (lc == null) lc = localeFile;
//        if (dat == null) dat = data;
//
//        Vector<String> ret = new Vector<String>();
//        for (Entry<String, String> i : dat.entrySet()) {
//            ret.add(i.getKey() + " = " + i.getValue().replace("\r", "\\r").replace("\n", "\\n"));
//        }
//        Collections.sort(ret);
//
//        String str = "";
//        for (int x = 0; x < ret.size(); x++) {
//            str += ret.get(x) + System.getProperty("line.separator");
//        }
//        JDUtilities.writeLocalFile(lc, str);
//
//    }

    public static void setLocale(String lID) {
        if (data != null && localeFile != null) return;

        localeID = lID;
        localeFile = JDIO.getResourceFile(LANGUAGES_DIR + localeID + ".lng");
        File defaultFile = JDIO.getResourceFile(LANGUAGES_DIR + DEFAULTLANGUAGE + ".lng");

        if (!localeFile.exists()) {
            System.out.println("Language " + localeID + " not installed");
            return;
        }

        data = JDLocale.parseLanguageFile(localeFile);

        if (defaultFile.exists()) {
            defaultData = JDLocale.parseLanguageFile(defaultFile);
        } else {
            System.out.println("Could not load the default languagefile: " + defaultFile);
        }
//        missingData = JDLocale.parseLanguageFile(JDUtilities.getResourceFile(LANGUAGES_DIR + localeID + ".lng.missing"));

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