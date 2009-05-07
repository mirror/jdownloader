//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Vector;

import javax.swing.UIManager;

import jd.config.SubConfiguration;
import jd.controlling.JDLogger;
import jd.http.Encoding;
import jd.http.requests.PostRequest;
import jd.nutils.io.JDFileFilter;
import jd.parser.Regex;

public class JDLocale {

    private static HashMap<Integer, String> data = new HashMap<Integer, String>();

    private static HashMap<Integer, String> defaultData = null;

    private static final String DEFAULTLANGUAGE = "english";

    public static final String CONFIG = "LOCALE";

    public static final String LOCALE_ID = "LOCALE";

    private static String LANGUAGES_DIR = "jd/languages/";

    private static String localeID;

    private static File localeFile;

    private static int key;

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
        File dir = JDUtilities.getResourceFile(LANGUAGES_DIR);
        if (!dir.exists()) return null;
        File[] files = dir.listFiles(new JDFileFilter(null, ".lng", false));
        Vector<String> ret = new Vector<String>();
        for (File element : files) {
            ret.add(element.getName().split("\\.")[0]);
        }
        return ret;
    }

    public static String getLocaleString(String key2, String def) {
        if (data == null || localeFile == null) {
            JDLocale.setLocale(SubConfiguration.getConfig(JDLocale.CONFIG).getStringProperty(JDLocale.LOCALE_ID, JDLocale.isGerman() ? "german" : "english"));
        }

        key = key2.toLowerCase().hashCode();
        if (data.containsKey(key)) return data.get(key);

        System.out.println("Key not found: " + key2 + " Defaultvalue: " + def);
        if (def == null) {
            // defaultData nur im absoluten Notfall laden
            loadDefault();
            if (defaultData.containsKey(key)) {
                def = defaultData.get(key);
            }
            if (def == null) def = key2;
        }

        data.put(key, def);

        return def;
    }

    public static boolean isGerman() {
        String country = System.getProperty("user.country");
        return country != null && country.equalsIgnoreCase("DE");
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
        try {
            return String.format(JDLocale.L(key, def), args);
        } catch (Exception e) {
            JDLogger.getLogger().severe("FIXME: " + key);
            return "FIXME: " + key;
        }
    }

    public static void parseLanguageFile(File file, HashMap<Integer, String> data) {
        data.clear();

        if (file == null || !file.exists()) {
            System.out.println("JDLocale: " + file + " not found");
            return;
        }

        BufferedReader f;
        try {
            f = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF8"));

            String line;
            String key;
            String value;
            while ((line = f.readLine()) != null) {
                if (line.startsWith("#")) continue;
                int split = line.indexOf("=");
                if (split <= 0) continue;

                key = line.substring(0, split).trim().toLowerCase();
                value = line.substring(split + 1).trim() + (line.endsWith(" ") ? " " : "");
                value = value.replace("\\r", "\r").replace("\\n", "\n");

                data.put(key.hashCode(), value);
            }
            f.close();
        } catch (IOException e) {

            jd.controlling.JDLogger.getLogger().log(java.util.logging.Level.SEVERE, "Exception occured", e);
        }

    }

    private static void loadDefault() {
        if (defaultData == null) {
            System.err.println("JD have to load the default language, there is an missing entry");
            defaultData = new HashMap<Integer, String>();
            File defaultFile = JDUtilities.getResourceFile(LANGUAGES_DIR + DEFAULTLANGUAGE + ".lng");
            if (defaultFile.exists()) {
                JDLocale.parseLanguageFile(defaultFile, defaultData);
            } else {
                System.out.println("Could not load the default languagefile: " + defaultFile);
            }
        }
    }

    public static void setLocale(String lID) {
        if (data != null && localeFile != null) return;

        localeID = lID;

        localeFile = JDUtilities.getResourceFile(LANGUAGES_DIR + localeID + ".lng");
        if (localeFile.exists()) {
            JDLocale.parseLanguageFile(localeFile, data);
        } else {
            System.out.println("Language " + localeID + " not installed");
            return;
        }
    }

    public static void initLocalisation() {
        UIManager.put("FileChooser.upFolderToolTipText", JDLocale.L("gui.filechooser.upFolderToolTipText", "Up one level"));
        UIManager.put("FileChooser.filesOfTypeLabelText", JDLocale.L("gui.filechooser.filesOfTypeLabelText", "Files of type:"));
        UIManager.put("FileChooser.lookInLabelText", JDLocale.L("gui.filechooser.lookInLabelText", "Look in:"));
        UIManager.put("FileChooser.saveInLabelText", JDLocale.L("gui.filechooser.saveInLabelText", "Save in:"));
        UIManager.put("FileChooser.fileNameLabelText", JDLocale.L("gui.filechooser.fileNameLabelText", "File name:"));
        UIManager.put("FileChooser.homeFolderToolTipText", JDLocale.L("gui.filechooser.homeFolderToolTipText", "Home folder"));
        UIManager.put("FileChooser.newFolderToolTipText", JDLocale.L("gui.filechooser.newFolderToolTipText", "Make a new folder"));
        UIManager.put("FileChooser.listViewButtonToolTipText", JDLocale.L("gui.filechooser.listViewButtonToolTipText", "List view"));
        UIManager.put("FileChooser.detailsViewButtonToolTipText", JDLocale.L("gui.filechooser.detailsViewButtonToolTipText", "Details"));
        UIManager.put("FileChooser.saveButtonText", JDLocale.L("gui.filechooser.saveButtonText", "Save"));
        UIManager.put("FileChooser.openButtonText", JDLocale.L("gui.filechooser.openButtonText", "Open"));
        UIManager.put("FileChooser.cancelButtonText", JDLocale.L("gui.filechooser.cancelButtonText", "Cancel"));
        UIManager.put("FileChooser.updateButtonText", JDLocale.L("gui.filechooser.updateButtonText", "Update"));
        UIManager.put("FileChooser.helpButtonText", JDLocale.L("gui.filechooser.helpButtonText", "Help"));
        UIManager.put("FileChooser.deleteButtonText", JDLocale.L("gui.filechooser.deleteButtonText", "Delete"));
        UIManager.put("FileChooser.saveButtonToolTipText", JDLocale.L("gui.filechooser.saveButtonToolTipText", "Save"));
        UIManager.put("FileChooser.openButtonToolTipText", JDLocale.L("gui.filechooser.openButtonToolTipText", "Open"));
        UIManager.put("FileChooser.cancelButtonToolTipText", JDLocale.L("gui.filechooser.cancelButtonToolTipText", "Cancel"));
        UIManager.put("FileChooser.updateButtonToolTipText", JDLocale.L("gui.filechooser.updateButtonToolTipText", "Update"));
        UIManager.put("FileChooser.helpButtonToolTipText", JDLocale.L("gui.filechooser.helpButtonToolTipText", "Help"));
        UIManager.put("FileChooser.deleteButtonToolTipText", JDLocale.L("gui.filechooser.deleteButtonToolTipText", "Delete"));
        UIManager.put("FileChooser.openDialogTitleText", JDLocale.L("gui.filechooser.openWindowTitleText", "Open"));
        UIManager.put("FileChooser.saveDialogTitleText", JDLocale.L("gui.filechooser.saveWindowTitleText", "Save"));
        UIManager.put("FileChooser.acceptAllFileFilterText", JDLocale.L("gui.filechooser.acceptAllFileFilterText", "All files"));
        UIManager.put("FileChooser.other.newFolder", JDLocale.L("gui.filechooser.other.newFoldert", "New folder"));
        UIManager.put("FileChooser.other.newFolder.subsequent", JDLocale.L("gui.filechooser.other.newFolder.subsequent", "New folder {0}"));
        UIManager.put("FileChooser.win32.newFolder", JDLocale.L("gui.filechooser.win32.newFolder", "New folder"));
        UIManager.put("FileChooser.win32.newFolder.subsequent", JDLocale.L("gui.filechooser.win32.newFolder.subsequent", "New folder {0}"));
        UIManager.put("FileChooser.pathLabelText", JDLocale.L("gui.filechooser.pathLabelText", "Path"));

        UIManager.put("JXTable.column.packSelected", JDLocale.L("gui.treetable.packSelected", "Pack selected column"));
        UIManager.put("JXTable.column.packAll", JDLocale.L("gui.treetable.packAll", "Pack all columns"));
        UIManager.put("JXTable.column.horizontalScroll", JDLocale.L("gui.treetable.horizontalScroll", "Horizontal scroll"));
    }

    public static String translate(String to, String msg) {
        return JDLocale.translate("auto", to, msg);
    }

    public static String translate(String from, String to, String msg) {
        try {
            PostRequest r = new PostRequest("http://translate.google.com/translate_t?sl=" + from + "&tl=" + to);

            r.addVariable("hl", "de");
            r.addVariable("text", msg);
            r.addVariable("sl", from);
            r.addVariable("tl", to);
            r.addVariable("ie", "UTF8");

            return Encoding.UTF8Decode(Encoding.htmlDecode(new Regex(r.load(), "<div id\\=result_box dir\\=\"ltr\">(.*?)</div>").getMatch(0)));
        } catch (IOException e) {
            jd.controlling.JDLogger.getLogger().log(java.util.logging.Level.SEVERE, "Exception occured", e);
            return null;
        }
    }

}