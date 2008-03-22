//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team jdownloader@freenet.de
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program  is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSSee the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://wnu.org/licenses/>.


package jd.utils;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;
import java.util.Map.Entry;
import java.util.logging.Logger;

import jd.JDFileFilter;
/**
 * Diese Klasse stellt Methoden zur Verfügung um in einen String mitPlatzhaltern
 * werte einzusetzen
 */
public class JDLocale {
    private static final String DEFAULTLANGUAGE = "german";

    private static String                  LANGUAGES_DIR = "jd/languages/";

    private static Logger                  logger        = JDUtilities.getLogger();

    private static HashMap<String, String> data          = new HashMap<String, String>();
    private static HashMap<String, String> missingData          = new HashMap<String, String>();
    private static HashMap<String, String> defaultData          = new HashMap<String, String>();

    private static File                    localeFile;                                    ;

    public static Vector<String> getLocaleIDs() {
        File dir = JDUtilities.getResourceFile(LANGUAGES_DIR);
        if (!dir.exists()) return null;
        File[] files = dir.listFiles(new JDFileFilter(null, ".lng", false));
        Vector<String> ret = new Vector<String>();
        for (int i = 0; i < files.length; i++) {
            ret.add(files[i].getName().split("\\.")[0]);
        }
        return ret;
    }

    public static String getLocaleString(String key, String def) {
        if (data == null) {
            logger.severe("Use setLocale() first!");
            return key;
        }
        
        if (def == null) def = key;
        if (data.containsKey(key)) {
            return JDUtilities.UTF8Decode(data.get(key)).replace("\\r", "\r").replace("\\n", "\n");
        }
        logger.info("Key not found: " + key);
        if (defaultData.containsKey(key)){
            def=JDUtilities.UTF8Decode(defaultData.get(key)).replace("\\r", "\r").replace("\\n", "\n");
        }
        data.put(key, JDUtilities.UTF8Encode(def));
        missingData.put(key, JDUtilities.UTF8Encode(def));
        saveData(new File(localeFile.getAbsolutePath()+".extended"),data);
        saveData(new File(localeFile.getAbsolutePath()+".missing"),missingData);
        return def;

    }
/*
    private static Vector<String[]> send = new Vector<String[]>();
    private static Vector<String> sent = new Vector<String>();
    private static Thread           sender;
*/
    private static String           lID;



    public static String L(String key) {
        return getLocaleString(key, null);
    }

    public static String L(String key, String def) {
        return getLocaleString(key, def);
    }

    private static void saveData(File lc,HashMap<String, String> dat) {
        if(lc==null)lc=localeFile;
        if(dat==null)dat=data;
        Iterator<Entry<String, String>> iterator;
        if (dat == null) return;
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
        for (int x = 0; x < ret.size(); x++)
            str += ret.get(x) + System.getProperty("line.separator");
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
        
      data=parseLanguageFile(file);
        
   
       if(defaultFile.exists()){
        defaultData= parseLanguageFile(defaultFile);
        }else{
            logger.warning("Could not load The default languagefile: "+defaultFile);
            
        }
       missingData= parseLanguageFile(JDUtilities.getResourceFile(LANGUAGES_DIR + localeID + ".lng.missing"));
       
       
    }

    private static HashMap<String, String> parseLanguageFile(File file) {
        HashMap<String, String> dat = new HashMap<String, String>();
        if(!file.exists()){
            
            
            logger.severe("JDLocale: "+file+" not found");
            return dat;
        }
        String str = JDUtilities.getLocalFile(file);
        String[] lines = JDUtilities.splitByNewline(str);
        boolean dupes=false;
        for (int i = 0; i < lines.length; i++) {
            int split = lines[i].indexOf("=");
            if (split <= 0 || lines[i].startsWith("#")) continue;
            String key = lines[i].substring(0, split).trim();
            String value = lines[i].substring(split + 1).trim();
            if (dat.containsKey(key)) {
                logger.severe("Dupe found: " + key);
                dat.put(key, value);
                dupes=true;
            }
            else {
                dat.put(key, value);
            }

        }
        if(dupes){
            logger.warning("Duplicate entries found in "+file+". Wrote fixed Version to "+new File(file.getAbsolutePath()+".nodupes"));
            saveData(new File(file.getAbsolutePath()+".nodupes"),dat);
            
        }
        return dat;
    }

    public static String getLocale() {
       return lID;
    }

}