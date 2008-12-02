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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Vector;
import java.util.logging.Logger;

import javazoom.jl.decoder.JavaLayerException;
import javazoom.jl.player.advanced.AdvancedPlayer;
import jd.JDFileFilter;
import jd.http.Encoding;
import jd.nutils.io.JDIO;
import jd.parser.Regex;

public class JDSounds {
    private static HashMap<String, String> data = new HashMap<String, String>();

    private static boolean enabled = true;

    private static Logger logger = JDUtilities.getLogger();

    private static boolean paralellSounds = false;

    public static final String PARAM_CURRENTTHEME = "SOUND_CURRENTTHEME";

    private static boolean playing = false;

    private static final String SOUND_DIR = "jd/snd/";

    private static String THEMES_DIR = "jd/themes/";

    public static boolean getParalell() {
        return paralellSounds;
    }

    public static Vector<String> getSoundIDs() {
        File dir = JDUtilities.getResourceFile(THEMES_DIR);
        if (!dir.exists()) { return null; }
        File[] files = dir.listFiles(new JDFileFilter(null, ".snd", false));
        Vector<String> ret = new Vector<String>();
        for (File element : files) {
            ret.add(element.getName().split("\\.")[0]);
        }
        return ret;
    }

    public static String getSoundValue(String key, String def) {
        if (data == null) {
            logger.severe("Use setSoundTheme() first!");
            return key;
        }

        if (data.containsKey(key)) { return Encoding.UTF8Decode(data.get(key)); }
        return null;

    }

    public static boolean isEnabled() {
        return enabled;
    }

    /**
     * Spielt einen Sound ab und blockiert bis der Sound fertig gespielt wurde
     * 
     * @param key
     * @return
     */
    public static boolean P(String key) {
        return JDSounds.playSound(JDSounds.SF(key), false);
    }

    public static boolean P(String key, String def) {
        return JDSounds.playSound(JDSounds.SF(key, def), false);
    }

    /**
     * Spielt einen Sound ab
     * 
     * @param f
     * @param threated
     * @return
     */
    private static boolean playSound(final File f, boolean threated) {
        if (f == null || !f.exists()) return false;
        if (playing && !paralellSounds) return false;
        playing = true;
        if (threated) {
            new Thread() {
                @Override
                public void run() {
                    JDSounds.playMp3(f);
                    playing = false;
                }
            }.start();
        } else {
            JDSounds.playMp3(f);
            playing = false;
        }

        return true;
    }

    private static void playMp3(File f) {
        try {
            new AdvancedPlayer(new FileInputStream(f.getAbsolutePath())).play();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (JavaLayerException e) {
            e.printStackTrace();
        }
    }

    /**
     * Spielt den Sound zu key. Das Spielen läuft in einem Thread, also nicht
     * blockierend ab
     * 
     * @return
     */
    public static boolean PT(String key) {
        return JDSounds.playSound(JDSounds.SF(key), true);
    }

    public static boolean PT(String key, String def) {
        return JDSounds.playSound(JDSounds.SF(key, def), true);
    }

    /**
     * aktiviert/deaktiviert sounds
     * 
     * @param enabled
     */
    public static void setEnabled(boolean enabled) {
        JDSounds.enabled = enabled;
    }

    /**
     * Legt fest ob sich sounds überschneiden dürfen. Default ist false.d.h.
     * dass ein 2. Sound nicht gespielt wird wenn der erste noch läuft
     * 
     * @param v
     */
    public static void setParalell(boolean v) {
        paralellSounds = v;
    }

    public static void setSoundTheme(String themeID) {
        File file = JDUtilities.getResourceFile(THEMES_DIR + themeID + ".snd");
        if (!file.exists()) {
            logger.severe("SoundTheme " + themeID + " not installed");
            return;
        }
        logger.info("SoundTheme " + themeID + " loaded");
        data = new HashMap<String, String>();
        String str = JDIO.getLocalFile(file);
        String[] lines = Regex.getLines(str);
        for (String element : lines) {
            int split = element.indexOf("=");
            if (split <= 0 || element.startsWith("#")) {
                continue;
            }
            String key = element.substring(0, split).trim();
            String value = element.substring(split + 1).trim();
            if (data.containsKey(key)) {
                logger.severe("Dupe found: " + key);
            } else {
                data.put(key, value);
            }

        }

    }

    /**
     * Gibt ein Soundfile
     * 
     * @param key
     * @return
     */
    public static File SF(String key) {
        key = JDSounds.V(key);
        if (key == null) { return null; }
        return JDUtilities.getResourceFile(SOUND_DIR + key + ".mp3");
    }

    public static File SF(String key, String def) {
        key = JDSounds.V(key, def);
        if (key == null) { return null; }
        return JDUtilities.getResourceFile(SOUND_DIR + key + ".mp3");
    }

    /**
     * Gibt einen Sound String zum Key zurück
     * 
     * @param key
     * @return
     */
    public static String V(String key) {
        return JDSounds.getSoundValue(key, null);
    }

    /**
     * Gibt einen Sound String zum Key zurück
     * 
     * @param key
     * @return
     */
    public static String V(String key, String def) {
        return JDSounds.getSoundValue(key, def);
    }

}