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
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;
import java.util.Map.Entry;
import java.util.logging.Logger;

import jd.JDFileFilter;
import jd.gui.skins.simple.SimpleGUI;
import jd.http.PostRequest;
import jd.parser.Regex;
import jd.parser.SimpleMatches;

/**
 * Diese Klasse stellt Methoden zur Verfügung um in einen String mitPlatzhaltern
 * werte einzusetzen
 */
public class JDLocale {

    private static HashMap<String, String> data = new HashMap<String, String>();

    private static HashMap<String, String> defaultData = new HashMap<String, String>();

    private static final String DEFAULTLANGUAGE = JDLocale.isGerman() ? "german" : "english";

    private static String LANGUAGES_DIR = "jd/languages/";

    /*
     * private static Vector<String[]> send = new Vector<String[]>(); private
     * static Vector<String> sent = new Vector<String>(); private static
     * Thread sender;
     */
    private static String lID;

    public static final String LOCALE_EDIT_MODE = "LOCALE_EDIT_MODE";
    private static File localeFile;
    private static Logger logger = JDUtilities.getLogger();

    private static HashMap<String, String> missingData = new HashMap<String, String>();;

    public static String getLocale() {
        return lID;
    }

    public static Vector<String> getLocaleIDs() {
        File dir = JDUtilities.getResourceFile(LANGUAGES_DIR);
        if (!dir.exists()) {
            return null;
        }
        File[] files = dir.listFiles(new JDFileFilter(null, ".lng", false));
        Vector<String> ret = new Vector<String>();
        for (File element : files) {
            ret.add(element.getName().split("\\.")[0]);
        }
        return ret;
    }

    public static String getLocaleString(String key, String def) {
        if (data == null || localeFile == null) {
            // logger.severe("Use setLocale() first!");
            JDLocale.setLocale(JDUtilities.getSubConfig(SimpleGUI.GUICONFIGNAME).getStringProperty(SimpleGUI.PARAM_LOCALE, JDLocale.isGerman() ? "german" : "english"));

        }

        if (def == null) {
            def = key;
        }
        if (data.containsKey(key)) { return JDUtilities.UTF8Decode(data.get(key)).replace("\\r", "\r").replace("\\n", "\n"); }
        logger.info("Key not found: " + key);
        if (defaultData.containsKey(key)) {
            def = JDUtilities.UTF8Decode(defaultData.get(key)).replace("\\r", "\r").replace("\\n", "\n");
        }
        data.put(key, JDUtilities.UTF8Encode(def));
        missingData.put(key, JDUtilities.UTF8Encode(def));

        JDLocale.saveData(new File(localeFile.getAbsolutePath() + ".extended"), data);
        JDLocale.saveData(new File(localeFile.getAbsolutePath() + ".missing"), missingData);
        return def;

    }

    private static boolean isGerman() {
        return System.getProperty("user.country") != null && System.getProperty("user.country").equalsIgnoreCase("DE");
    }

    public static String L(String key) {
        return JDLocale.getLocaleString(key, null);
    }

    public static String L(String key, String def) {
        return JDLocale.getLocaleString(key, def);
    }

    public static void main(String[] argv) {
        logger = JDUtilities.getLogger();
        File code = new File("G:/jdworkspace/JD/src");
        final Vector<File> javas = new Vector<File>();
        FileFilter ff = new FileFilter() {

            public boolean accept(File pathname) {
                if (pathname.isDirectory()) {
                    pathname.listFiles(this);
                }
                if (pathname.getAbsolutePath().endsWith(".java")) {
                    javas.add(pathname);
                }

                return true;
            }
        };
        code.listFiles(ff);

        StringBuffer sb = new StringBuffer();
        HashMap<String, String> map = new HashMap<String, String>();

        for (File java : javas) {
            String c = JDUtilities.getLocalFile(java);
            logger.info(java.getAbsolutePath());
            ArrayList<ArrayList<String>> res = SimpleMatches.getAllSimpleMatches(c, "JDLocale.L(°,°)");
            logger.info("Found " + res.size() + " entries");
            for (ArrayList<String> entry : res) {
                if (!map.containsKey(entry.get(0))) {
                    String key = entry.get(0).trim();
                    String value = entry.get(1).trim();
                    if (key.contains(";")) {
                        continue;
                    }
                    while (key.startsWith("\"")) {
                        key = key.substring(1);
                    }
                    while (value.startsWith("\"")) {
                        value = value.substring(1);
                    }
                    while (key.endsWith(")")) {
                        key = key.substring(0, key.length() - 1);
                    }
                    while (key.endsWith("\"")) {
                        key = key.substring(0, key.length() - 1);
                    }

                    while (value.endsWith("\"")) {
                        value = value.substring(0, value.length() - 1);
                    }
                    value.replaceAll("\r\n", "\\r\\n");
                    value.replaceAll("\n\r", "\\r\\n");
                    value.replaceAll("//", "");
                    map.put(key, value);

                    sb.append("\r\n" + key + " = " + value);
                }
            }
        }
        logger.info(sb.toString());
    }

    private static HashMap<String, String> parseLanguageFile(File file) {
        HashMap<String, String> dat = new HashMap<String, String>();
        if (!file.exists()) {

            logger.severe("JDLocale: " + file + " not found");
            return dat;
        }
        String str = JDUtilities.getLocalFile(file);
        String[] lines = Regex.getLines(str);
        boolean dupes = false;
        for (String element : lines) {
            int split = element.indexOf("=");
            if (split <= 0 || element.startsWith("#")) {
                continue;
            }
            String key = element.substring(0, split).trim();
            String value = element.substring(split + 1).trim() + (element.endsWith(" ") ? " " : "");
            if (dat.containsKey(key)) {
                logger.severe("Dupe found: " + key);
                dat.put(key, value);
                dupes = true;
            } else {
                dat.put(key, value);
            }

        }
        if (dupes) {
            logger.warning("Duplicate entries found in " + file + ". Wrote fixed Version to " + new File(file.getAbsolutePath() + ".nodupes"));
            JDLocale.saveData(new File(file.getAbsolutePath() + ".nodupes"), dat);

        }
        return dat;
    }

    private static void saveData(File lc, HashMap<String, String> dat) {

        if (lc == null) {
            lc = localeFile;
        }
        if (dat == null) {
            dat = data;
        }
        if (!JDUtilities.getSubConfig(SimpleGUI.GUICONFIGNAME).getBooleanProperty(JDLocale.LOCALE_EDIT_MODE, false)) {
            return;
        }
        Iterator<Entry<String, String>> iterator;
        if (dat == null) {
            return;
        }
        iterator = dat.entrySet().iterator();
        // stellt die Wartezeiten zurück
        Entry<String, String> i;
        String str = "";
        Vector<String> ret = new Vector<String>();
        while (iterator.hasNext()) {
            i = iterator.next();
            ret.add(i.getKey() + " = " + i.getValue().replace("\r", "\\r").replace("\n", "\\n"));
        }
        Collections.sort(ret);
        for (int x = 0; x < ret.size(); x++) {
            str += ret.get(x) + System.getProperty("line.separator");
        }
        JDUtilities.writeLocalFile(lc, str);

    }

    public static void setLocale(String localeID) {
        File file = JDUtilities.getResourceFile(LANGUAGES_DIR + localeID + ".lng");
        File defaultFile = JDUtilities.getResourceFile(LANGUAGES_DIR + DEFAULTLANGUAGE + ".lng");
        localeFile = file;
        lID = localeID;
        if (!file.exists()) {
            logger.severe("Lanuage " + localeID + " not installed");
            return;
        }

        data = JDLocale.parseLanguageFile(file);

        if (defaultFile.exists()) {
            defaultData = JDLocale.parseLanguageFile(defaultFile);
        } else {
            logger.warning("Could not load The default languagefile: " + defaultFile);

        }
        missingData = JDLocale.parseLanguageFile(JDUtilities.getResourceFile(LANGUAGES_DIR + localeID + ".lng.missing"));

    }

    public static String translate(String to, String msg) {

        PostRequest r = new PostRequest("http://translate.google.com/translate_t?sl=" + "auto" + "&tl=" + to);

        r.setPostVariable("hl", "de");
        r.setPostVariable("text", msg);
        r.setPostVariable("sl", "auto");
        r.setPostVariable("tl", to);
        r.setPostVariable("ie", "UTF8");

        r.getHeaders().put("User-Agent", "Mozilla/5.0 (Windows; U; Windows NT 6.0; de; rv:1.8.1.14)");
        r.getHeaders().put("Accept", "text/xml,application/xml,application/xhtml+xml,text/html;q=0.9,text/plain;q=0.8,image/png,*/*;q=0.5");
        r.getHeaders().put("Accept-Language", "de-de,de;q=0.8,en-us;q=0.5,en;q=0.3");
        r.getHeaders().put("Accept-Encoding", "gzip,deflate");
        r.getHeaders().put("Accept-Charset", "ISO-8859-1,utf-8;q=0.7,*;q=0.7");
        r.getHeaders().put("Referer", "http://translate.google.com/translate_t?sl=en&tl=de");
        r.getHeaders().put("Content-Type", "application/x-www-form-urlencoded");
        String page;
        try {
            page = r.load();

            return JDUtilities.UTF8Decode(JDUtilities.htmlDecode(new Regex(page, "<div id\\=result_box dir\\=\"ltr\">(.*?)</div>").getFirstMatch()));
        } catch (Exception e) {

            e.printStackTrace();
            return msg;
        }

        // POST /translate_t?sl=en&tl=de HTTP/1.1
        // Host: translate.google.com
        // User-Agent: Mozilla/5.0 (Windows; U; Windows NT 6.0; de; rv:1.8.1.14)
        // Gecko/20080404 Firefox/2.0.0.14;MEGAUPLOAD 1.0
        // Accept:
        // text/xml,application/xml,application/xhtml+xml,text/html;q=0.9,text/
        // plain;q=0.8,image/png,*/*;q=0.5
        // Accept-Language: de-de,de;q=0.8,en-us;q=0.5,en;q=0.3
        // Accept-Encoding: gzip,deflate
        // Accept-Charset: ISO-8859-1,utf-8;q=0.7,*;q=0.7
        // Keep-Alive: 300
        // Connection: keep-alive
        // Referer: http://translate.google.com/translate_t?sl=en&tl=de
        // Cookie:
        // PREF=ID=58dc3a7b038af491:TM=1213636773:LM=1213636773:S=vBGFf-GXvSvFFztt
        // Content-Type: application/x-www-form-urlencoded
        // Content-Length: 38
        // hl=de&ie=UTF8&text=testing&sl=en&tl=de

    }

    public static String translate(String from, String to, String msg) {

        PostRequest r = new PostRequest("http://translate.google.com/translate_t?sl=" + from + "&tl=" + to);

        r.setPostVariable("hl", "de");
        r.setPostVariable("text", msg);
        r.setPostVariable("sl", from);
        r.setPostVariable("tl", to);
        r.setPostVariable("ie", "UTF8");

        String page;
        try {
            page = r.load();

            return JDUtilities.UTF8Decode(JDUtilities.htmlDecode(new Regex(page, "<div id\\=result_box dir\\=\"ltr\">(.*?)</div>").getFirstMatch()));
        } catch (IOException e) {

            e.printStackTrace();
            return null;
        }

        // POST /translate_t?sl=en&tl=de HTTP/1.1
        // Host: translate.google.com
        // User-Agent: Mozilla/5.0 (Windows; U; Windows NT 6.0; de; rv:1.8.1.14)
        // Gecko/20080404 Firefox/2.0.0.14;MEGAUPLOAD 1.0
        // Accept:
        // text/xml,application/xml,application/xhtml+xml,text/html;q=0.9,text/
        // plain;q=0.8,image/png,*/*;q=0.5
        // Accept-Language: de-de,de;q=0.8,en-us;q=0.5,en;q=0.3
        // Accept-Encoding: gzip,deflate
        // Accept-Charset: ISO-8859-1,utf-8;q=0.7,*;q=0.7
        // Keep-Alive: 300
        // Connection: keep-alive
        // Referer: http://translate.google.com/translate_t?sl=en&tl=de
        // Cookie:
        // PREF=ID=58dc3a7b038af491:TM=1213636773:LM=1213636773:S=vBGFf-GXvSvFFztt
        // Content-Type: application/x-www-form-urlencoded
        // Content-Length: 38
        // hl=de&ie=UTF8&text=testing&sl=en&tl=de

    }

    public static void translate(String from, String afrom, String ato, String to) {

        File file = JDUtilities.getResourceFile(LANGUAGES_DIR + from + ".lng");

        if (!file.exists()) {
            logger.severe("Lanuage " + file + " not installed");
            return;
        }
        int i = 0;

        HashMap<String, String> ret = new HashMap<String, String>();
        HashMap<String, String> data = JDLocale.parseLanguageFile(file);
        for (Entry<String, String> next : data.entrySet()) {
            i++;
            ret.put(next.getKey(), JDLocale.translate(afrom, ato, next.getValue()));

            logger.info(i + " : " + next.getKey() + " = " + ret.get(next.getKey()));
            // saveData(JDUtilities.getResourceFile(LANGUAGES_DIR + "google_" +
            // to + ".lng"), ret);
        }
        JDLocale.saveData(JDUtilities.getResourceFile(LANGUAGES_DIR + "google_" + to + ".lng"), ret);

    }

}