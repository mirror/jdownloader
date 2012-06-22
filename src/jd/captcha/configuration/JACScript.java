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

package jd.captcha.configuration;

import java.io.File;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.Vector;

import jd.captcha.JAntiCaptcha;
import jd.captcha.pixelgrid.Captcha;
import jd.captcha.pixelgrid.Letter;
import jd.captcha.utils.Utilities;
import jd.nutils.io.JDIO;
import jd.utils.JDUtilities;

import org.appwork.utils.Regex;
import org.appwork.utils.logging2.LogSource;
import org.jdownloader.logging.LogController;

/**
 * Diese Klasse parsed das JAC Script
 * 
 * @author JD-Team
 */
public class JACScript {
    /**
     * Vector für die Befehle die für die Vorverarbeitung des Captchas verwendet werden. (script.jas)
     */
    private Vector<String[]>          captchaPrepareCommands;

    /**
     * Farbwert für den verwendeten Farbraum. 0: hsb 1: RGB
     */
    private int                       color;

    /**
     * Internes Farbarray. Hier werden die Eingaben über setColorFormat abgelegt
     */
    private int[]                     colorComponents = { 3, 3, 3 };

    /**
     * Internet Umrechnungsfaktor. Je nach verwendetem Farbmodell. Wird automatisch gesetzt
     */
    private int                       colorFaktor;

    /**
     * Werte-Array Wird gaps != null, so werden die Werte als Trennpositionen für die letter detection verwendet. Alle anderen Erkennungen werden dann ignoriert
     */
    private int[]                     gaps;

    /**
     * Vector für die Befehle die für die Ekennung allgemein gelten (script.jas)
     */
    private Vector<String[]>          jacCommands;
    private Vector<String[]>          letterCommands;

    /**
     * Methodenname
     */
    private String                    method;
    /**
     * owner
     */
    private JAntiCaptcha              owner;
    /**
     * Hashtable für die parameter
     */
    private Hashtable<String, Object> parameter       = new Hashtable<String, Object>();

    /**
     * Adresse zum Jacscript
     */
    private String                    scriptFile;

    private final LogSource           logger;

    /**
     * @param owner
     * @param cl
     * @param jarFile
     *            Die Jar Datei mit den Methods
     * @param script
     *            Der Skriptfile in der JAR, das ausgelesen werden soll
     * @param method
     *            Name der Methode, die genutzt wird
     */
    public JACScript(JAntiCaptcha owner, String method) {
        logger = LogController.CL();
        init();
        this.owner = owner;
        this.method = method;
        scriptFile = "script.jas";
        parseScriptFile();
        executeParameterCommands();
    }

    /**
     * @param letter
     */
    public void executeLetterPrepareCommands(Letter letter) {

        logger.fine("Execute Script.jas Letter Prepare scripts");

        String[] params;
        try {
            for (int i = 0; i < letterCommands.size(); i++) {
                String[] cmd = letterCommands.elementAt(i);

                logger.info("Execute Function: " + cmd[1] + "(" + cmd[2] + ")");

                if (cmd[0].equals("parameter")) {

                    logger.severe("Syntax Error in " + method + "/+script.jas(letter)");
                    // captchaPrepareCommands

                } else if (cmd[0].equals("function") && cmd[2] == null) {
                    if (cmd[1].equalsIgnoreCase("normalize")) {
                        letter.normalize();
                        continue;
                    } else if (cmd[1].equalsIgnoreCase("clean")) {
                        letter.clean();
                        continue;
                    } else if (cmd[1].equalsIgnoreCase("autoAlign")) {
                        letter.autoAlign();
                        continue;
                    } else if (cmd[1].equalsIgnoreCase("invertIfMoreBackground")) {
                        letter.invertIfMoreBackground();
                        continue;
                    } else if (cmd[1].equalsIgnoreCase("toBlackAndWhite")) {
                        letter.toBlackAndWhite();
                        continue;
                    } else if (cmd[1].equalsIgnoreCase("invert")) {
                        letter.invert();
                        continue;

                    } else {

                        logger.severe("Error in " + method + "/+script.jas : Function not valid: " + cmd[1] + "(" + cmd[2] + ")");

                    }

                } else if (cmd[0].equals("function") && (params = cmd[2].split("\\,")).length == 1) {

                    if (cmd[1].equalsIgnoreCase("toBlackAndWhite")) {
                        letter.toBlackAndWhite(Double.parseDouble(params[0].trim()));
                        continue;

                    } else if (cmd[1].equalsIgnoreCase("resizetoHeight")) {
                        letter.resizetoHeight(Integer.parseInt(params[0].trim()));

                        continue;

                    } else if (cmd[1].equalsIgnoreCase("turn")) {
                        Letter tmp = letter.turn(Integer.parseInt(params[0].trim()));
                        letter.grid = tmp.grid;
                        continue;

                    } else if (cmd[1].equalsIgnoreCase("reduceWhiteNoise")) {
                        letter.reduceWhiteNoise(Integer.parseInt(params[0].trim()));
                        continue;
                    } else if (cmd[1].equalsIgnoreCase("normalize")) {
                        letter.normalize(Double.parseDouble(params[0].trim()));
                        continue;
                    } else if (cmd[1].equalsIgnoreCase("reduceBlackNoise")) {
                        letter.reduceBlackNoise(Integer.parseInt(params[0].trim()));
                        continue;
                    } else if (cmd[1].equalsIgnoreCase("blurIt")) {
                        letter.blurIt(Integer.parseInt(params[0].trim()));
                        continue;
                    } else if (cmd[1].equalsIgnoreCase("sampleDown")) {
                        letter.sampleDown(Integer.parseInt(params[0].trim()));
                        continue;
                    } else if (cmd[1].equalsIgnoreCase("doSpecial")) {
                        String[] ref = params[0].trim().split("\\.");
                        if (ref.length != 2) {

                            logger.severe("dpSpecial-Parameter should have the format Class.Method");

                            continue;
                        }
                        String cl = ref[0];
                        String methodname = ref[1];
                        Class<?> newClass;
                        try {
                            newClass = Class.forName("jd.captcha.specials." + cl);

                            Class<?>[] parameterTypes = new Class[] { letter.getClass() };
                            Method method = newClass.getMethod(methodname, parameterTypes);
                            Object[] arguments = new Object[] { letter };
                            Object instance = null;
                            method.invoke(instance, arguments);

                        } catch (Exception e) {

                            logger.severe("Fehler in doSpecial:" + e.getLocalizedMessage());

                            logger.log(e);
                        }

                    } else if (cmd[1].equalsIgnoreCase("cleanBackgroundByColor")) {
                        letter.cleanBackgroundByColor(Integer.parseInt(params[0].trim()));
                        continue;
                    } else {

                        logger.severe("Error in " + method + "/+script.jas : Function not valid: " + cmd[1] + "(" + cmd[2] + ")");

                    }
                } else if (cmd[0].equals("function") && (params = cmd[2].split("\\,")).length == 2) {
                    if (cmd[1].equalsIgnoreCase("reduceWhiteNoise")) {
                        letter.reduceWhiteNoise(Integer.parseInt(params[0].trim()), Double.parseDouble(params[1].trim()));
                        continue;
                    } else if (cmd[1].equalsIgnoreCase("resizetoHeight")) {
                        letter.resizetoHeight(Integer.parseInt(params[0].trim()), Double.parseDouble(params[1].trim()));
                        continue;

                    } else if (cmd[1].equalsIgnoreCase("reduceBlackNoise")) {
                        letter.reduceBlackNoise(Integer.parseInt(params[0].trim()), Double.parseDouble(params[1].trim()));
                        continue;
                    } else if (cmd[1].equalsIgnoreCase("align")) {
                        Letter tmp = letter.align(Integer.parseInt(params[0].trim()), Integer.parseInt(params[1].trim()));

                        letter.grid = tmp.grid;
                        continue;

                    } else if (cmd[1].equalsIgnoreCase("betterAlign")) {
                        Letter tmp = letter.betterAlign(Integer.parseInt(params[0].trim()), Integer.parseInt(params[1].trim()));
                        if (tmp != null && tmp.grid != null) {
                            letter.grid = tmp.grid;
                        }
                        continue;

                    } else {

                        logger.severe("Error in " + method + "/+script.jas : Function not valid: " + cmd[1] + "(" + cmd[2] + ")");

                    }
                } else if (cmd[0].equals("function") && (params = cmd[2].split("\\,")).length == 3) {
                    if (cmd[1].equalsIgnoreCase("removeSmallObjects")) {

                        letter.removeSmallObjects(Double.parseDouble(params[0].trim()), Double.parseDouble(params[1].trim()), Integer.parseInt(params[2].trim()));
                        continue;
                    } else if (cmd[1].equalsIgnoreCase("align")) {

                        letter.align(Double.parseDouble(params[0].trim()), Integer.parseInt(params[1].trim()), Integer.parseInt(params[2].trim()));
                    } else if (cmd[1].equalsIgnoreCase("desinx")) {
                        letter.desinx(Double.parseDouble(params[0].trim()), Double.parseDouble(params[1].trim()), Double.parseDouble(params[2].trim()));
                        continue;
                    } else if (cmd[1].equalsIgnoreCase("desiny")) {
                        letter.desiny(Double.parseDouble(params[0].trim()), Double.parseDouble(params[1].trim()), Double.parseDouble(params[2].trim()));
                        continue;

                    } else if (cmd[1].equalsIgnoreCase("normalize")) {
                        letter.normalize(Double.parseDouble(params[0].trim()), Double.parseDouble(params[1].trim()), Double.parseDouble(params[2].trim()));
                        continue;

                    } else {

                        logger.severe("Error in " + method + "/+script.jas : Function not valid: " + cmd[1] + "(" + cmd[2] + ")");

                    }

                } else if (cmd[0].equals("function") && (params = cmd[2].split("\\,")).length == 4) {

                } else if (cmd[0].equals("function") && (params = cmd[2].split("\\,")).length == 5) {

                } else if (cmd[0].equals("function") && (params = cmd[2].split("\\,")).length == 6) {

                }

            }
        } catch (Exception e) {

            logger.severe("Syntax Error in " + method + "/script.jas");

            logger.log(e);

        }
        // BasicWindow.showImage(captcha.getImage(),120,80);
    }

    /**
     * Diese Methode führt die zuvor eingelesenen JAC Script Befehle aus
     */
    private void executeParameterCommands() {
        if (jacCommands == null || jacCommands.size() == 0) {

            logger.warning("KEINE JAC COMMANDS");

            return;
        }

        logger.fine("Execute Script.jas Parameter scripts");

        try {
            for (int i = 0; i < jacCommands.size(); i++) {
                String[] cmd = jacCommands.elementAt(i);

                if (cmd[0].equals("parameter")) {

                    if (cmd[1].equalsIgnoreCase("gaps")) {

                        cmd[2] = cmd[2].substring(1, cmd[2].length() - 1);
                        String[] gaps = cmd[2].split("\\,");
                        int[] newGaps = new int[gaps.length];
                        for (int ii = 0; ii < gaps.length; ii++) {
                            newGaps[ii] = Integer.parseInt(gaps[ii]);
                        }
                        setGaps(newGaps);
                    } else if (cmd[1].equalsIgnoreCase("colortype")) {
                        setColorType(cmd[2]);

                    } else {

                        logger.info(cmd[1] + " - " + cmd[2]);

                        set(cmd[1].toLowerCase(), toType(cmd[2], cmd[1]));
                    }

                } else {

                    logger.severe("Syntax Error in " + method + "/+script.jas (" + cmd[0] + ")");

                }
            }
        } catch (Exception e) {

            logger.severe("Syntax Error in " + method + "/+script.jas");

            logger.log(e);
        }

    }

    /**
     * Diese Methode führt die zuvor eingelesenen JAc Script Prepare Befehle aus
     * 
     * @param captcha
     */
    public void executePrepareCommands(File file, Captcha captcha) {
        if (captcha == null || captcha.isPrepared()) {
            // ISt schon prepared
            return;
        }
        captcha.setCaptchaFile(file);

        logger.fine("Execute Script.jas Prepare scripts");

        captcha.setPrepared(true);
        String[] params;
        try {
            if (captchaPrepareCommands != null) {
                for (int i = 0; i < captchaPrepareCommands.size(); i++) {
                    String[] cmd = captchaPrepareCommands.elementAt(i);

                    logger.fine("Execute Function: " + cmd[1] + "(" + cmd[2] + ")");

                    if (cmd[0].equals("parameter")) {

                        logger.severe("Syntax Error in " + method + "/+script.jas");
                        // captchaPrepareCommands

                    } else if (cmd[0].equals("function") && cmd[1].matches("(?is)captchacommand\\..*")) {
                        String[] parm = null;
                        if (cmd[2] != null) {
                            parm = cmd[2].trim().split("[\\s]*\\,[\\s]*");
                        }
                        String methodname = cmd[1].replaceFirst("(?is)captchacommand\\.", "");
                        Class<?> newClass;
                        try {
                            newClass = Captcha.class;
                            Method[] methods = newClass.getMethods();
                            Method method = null;
                            for (Method method1 : methods) {
                                if (method1.getName().equals(methodname) && method1.getParameterTypes().length == parm.length) {
                                    method = method1;
                                }
                            }
                            if (method == null) {

                                logger.severe("Fehler in captchacommand: Methode existiert nicht");

                            }
                            Object instance = captcha;
                            Object[] paramObj = null;
                            if (parm != null && parm.length > 0) {
                                paramObj = new Object[parm.length];
                                Class<?>[] types = method.getParameterTypes();
                                for (int c = 0; c < types.length; c++) {
                                    Class<?> class1 = types[c];
                                    parm[c] = parm[c].trim();
                                    Object d = parm[c];
                                    if (class1.getName().equals("String"))
                                        d = parm[c];
                                    else if (class1.getName().equals("int"))
                                        d = Integer.parseInt(parm[c]);
                                    else if (class1.getName().equals("float"))
                                        d = Float.parseFloat(parm[c]);
                                    else if (class1.getName().equals("double"))
                                        d = Double.parseDouble(parm[c]);
                                    else if (class1.getName().equals("long"))
                                        d = Long.parseLong(parm[c]);
                                    else if (class1.getName().equals("short"))
                                        d = Short.parseShort(parm[c]);
                                    else if (class1.getName().equals("byte"))
                                        d = Byte.parseByte(parm[c]);
                                    else if (class1.getName().equals("boolean"))
                                        d = Boolean.parseBoolean(parm[c]);
                                    else if (class1.getName().equals("char"))
                                        d = Byte.parseByte(parm[c]);
                                    else
                                        d = class1.cast(d);
                                    paramObj[c] = d;
                                }
                            }
                            method.invoke(instance, paramObj);

                        } catch (Exception e) {

                            logger.severe("Fehler in captchacommand: " + e.getMessage());

                            logger.log(e);
                        }

                    } else if (cmd[0].equals("function") && cmd[2] == null) {

                        if (cmd[1].equalsIgnoreCase("invert")) {
                            captcha.invert();
                            continue;
                        } else if (cmd[1].equalsIgnoreCase("toBlackAndWhite")) {
                            captcha.toBlackAndWhite();
                            continue;
                        } else if (cmd[1].equalsIgnoreCase("normalize")) {
                            captcha.normalize();
                            continue;

                        } else if (cmd[1].equalsIgnoreCase("clean")) {
                            captcha.clean();
                            continue;
                        } else {

                            logger.severe("Error in " + method + "/+script.jas : Function not valid: " + cmd[1] + "(" + cmd[2] + ")");

                        }
                    } else if (cmd[0].equals("function") && (params = cmd[2].split("\\,")).length == 1) {

                        if (cmd[1].equalsIgnoreCase("toBlackAndWhite")) {
                            captcha.toBlackAndWhite(Double.parseDouble(params[0].trim()));
                            continue;
                        } else if (cmd[1].equalsIgnoreCase("reduceWhiteNoise")) {
                            captcha.reduceWhiteNoise(Integer.parseInt(params[0].trim()));
                            continue;
                        } else if (cmd[1].equalsIgnoreCase("normalize")) {
                            captcha.normalize(Double.parseDouble(params[0].trim()));
                            continue;
                        } else if (cmd[1].equalsIgnoreCase("convertPixel")) {
                            captcha.convertPixel(params[0].trim());
                            continue;
                        } else if (cmd[1].equalsIgnoreCase("reduceBlackNoise")) {
                            captcha.reduceBlackNoise(Integer.parseInt(params[0].trim()));
                            continue;
                        } else if (cmd[1].equalsIgnoreCase("blurIt")) {
                            captcha.blurIt(Integer.parseInt(params[0].trim()));
                            continue;
                        } else if (cmd[1].equalsIgnoreCase("sampleDown")) {
                            captcha.sampleDown(Integer.parseInt(params[0].trim()));
                            continue;
                        } else if (cmd[1].equalsIgnoreCase("cleanBackgroundByColor")) {
                            captcha.cleanBackgroundByColor(Integer.parseInt(params[0].trim()));
                            continue;

                        } else if (cmd[1].equalsIgnoreCase("doSpecial")) {
                            String[] ref = params[0].trim().split("\\.");
                            if (ref.length != 2) {

                                logger.severe("dpSpecial-Parameter should have the format Class.Method");

                                continue;
                            }
                            String cl = ref[0];
                            String methodname = ref[1];
                            Class<?> newClass;
                            try {
                                newClass = Class.forName("jd.captcha.specials." + cl);

                                Class<?>[] parameterTypes = new Class[] { captcha.getClass() };
                                Method method = newClass.getMethod(methodname, parameterTypes);
                                Object[] arguments = new Object[] { captcha };
                                Object instance = null;
                                method.invoke(instance, arguments);

                            } catch (Exception e) {

                                logger.severe("Fehler in doSpecial:" + e.getLocalizedMessage());

                                logger.log(e);
                            }

                        } else if (cmd[1].equalsIgnoreCase("saveImageasJpg")) {

                            captcha.saveImageasJpg(new File(params[0].trim()));
                            continue;
                        } else {

                            logger.severe("Error in " + method + "/+script.jas : Function not valid: " + cmd[1] + "(" + cmd[2] + ")");

                        }

                    } else if (cmd[0].equals("function") && (params = cmd[2].split("\\,")).length == 2) {
                        if (cmd[1].equalsIgnoreCase("reduceWhiteNoise")) {
                            captcha.reduceWhiteNoise(Integer.parseInt(params[0].trim()), Double.parseDouble(params[1].trim()));
                            continue;
                        } else if (cmd[1].equalsIgnoreCase("reduceBlackNoise")) {
                            captcha.reduceBlackNoise(Integer.parseInt(params[0].trim()), Double.parseDouble(params[1].trim()));
                            continue;
                        } else if (cmd[1].equalsIgnoreCase("removeBridges")) {
                            captcha.removeBridges(Integer.parseInt(params[0].trim()), Double.parseDouble(params[1].trim()));
                            continue;
                        } else if (cmd[1].equalsIgnoreCase("cleanByRGBDistance")) {
                            captcha.cleanByRGBDistance(Integer.parseInt(params[0].trim()), Integer.parseInt(params[1].trim()));
                            continue;
                        } else if (cmd[1].equalsIgnoreCase("cleanWithDetailMask")) {
                            captcha.cleanWithDetailMask(owner.createCaptcha(Utilities.loadImage(owner.getResourceFile(params[0].trim()))), Integer.parseInt(params[1].trim()));
                            continue;
                        } else {

                            logger.severe("Error in " + method + "/+script.jas : Function not valid: " + cmd[1] + "(" + cmd[2] + ")");

                        }

                    } else if (cmd[0].equals("function") && (params = cmd[2].split("\\,")).length == 3) {
                        if (cmd[1].equalsIgnoreCase("cleanWithMask")) {
                            logger.info("" + owner.getResourceFile(params[0].trim()));
                            captcha.cleanWithMask(owner.createCaptcha(Utilities.loadImage(owner.getResourceFile(params[0].trim()))), Integer.parseInt(params[1].trim()), Integer.parseInt(params[2].trim()));
                            continue;
                        } else if (cmd[1].equalsIgnoreCase("removeSmallObjects")) {
                            captcha.removeSmallObjects(Double.parseDouble(params[0].trim()), Double.parseDouble(params[1].trim()), Integer.parseInt(params[2].trim()));
                            continue;
                        } else if (cmd[1].equalsIgnoreCase("normalize")) {
                            captcha.normalize(Double.parseDouble(params[0].trim()), Double.parseDouble(params[1].trim()), Double.parseDouble(params[2].trim()));
                            continue;
                        } else if (cmd[1].equalsIgnoreCase("desinx")) {
                            captcha.desinx(Double.parseDouble(params[0].trim()), Double.parseDouble(params[1].trim()), Double.parseDouble(params[2].trim()));
                            continue;
                        } else if (cmd[1].equalsIgnoreCase("desiny")) {
                            captcha.desiny(Double.parseDouble(params[0].trim()), Double.parseDouble(params[1].trim()), Double.parseDouble(params[2].trim()));
                            continue;

                        } else {

                            logger.severe("Error in " + method + "/+script.jas : Function not valid: " + cmd[1] + "(" + cmd[2] + ")");

                        }

                    } else if (cmd[0].equals("function") && (params = cmd[2].split("\\,")).length == 4) {

                        if (cmd[1].equalsIgnoreCase("cleanBackgroundBySample")) {
                            captcha.cleanBackgroundBySample(Integer.parseInt(params[0].trim()), Integer.parseInt(params[1].trim()), Integer.parseInt(params[2].trim()), Integer.parseInt(params[3].trim()));
                            continue;
                        } else if (cmd[1].equalsIgnoreCase("crop")) {
                            captcha.crop(Integer.parseInt(params[0].trim()), Integer.parseInt(params[1].trim()), Integer.parseInt(params[2].trim()), Integer.parseInt(params[3].trim()));

                            continue;
                        } else if (cmd[1].equalsIgnoreCase("cleanBackgroundByHorizontalSampleLine")) {
                            captcha.cleanBackgroundByHorizontalSampleLine(Integer.parseInt(params[0].trim()), Integer.parseInt(params[1].trim()), Integer.parseInt(params[2].trim()), Integer.parseInt(params[3].trim()));

                            continue;
                        }

                    } else if (cmd[0].equals("function") && (params = cmd[2].split("\\,")).length == 5) {

                    }

                }
            }
        } catch (Exception e) {

            logger.severe("Syntax Error in " + method + "/script.jas (captcha)");

            logger.log(e);

        }
        // BasicWindow.showImage(captcha.getImage(),120,80);
    }

    /**
     * 
     * @param key
     * @return Wert passend zu key. casting nötig!
     */
    public Object get(String key) {
        key = key.toLowerCase();
        return parameter.get(key);
    }

    /**
     * @param key
     * @return Boolean Wert
     */
    public boolean getBoolean(String key) {

        Object ret = get(key);
        if (!(ret instanceof Boolean)) {

            logger.severe("Kein boolean Parameter für " + key);

            return false;
        }
        return (Boolean) ret;
    }

    /**
     * @return the captchaPrepareCommands
     */
    public Vector<String[]> getCaptchaPrepareCommands() {
        return captchaPrepareCommands;
    }

    /**
     * Gibt die Farbkomponente für die 255^i Gewcihtung zurück
     * 
     * @param i
     * @return FarbKomponente an i
     */
    public int getColorComponent(int i) {

        return colorComponents[i];
    }

    /**
     * @return Aktueller Colorfaktor (255^verwendete farbkomponenten)
     */
    public int getColorFaktor() {
        return colorFaktor;
    }

    /**
     * @return the color
     */
    public int getColorFormat() {
        return color;
    }

    /**
     * @param key
     * @return Double Value
     */
    public double getDouble(String key) {
        Object ret = get(key);
        if (!(ret instanceof Double)) {

            logger.severe("Kein double Parameter für " + key);

            return 0;
        }
        return (Double) ret;
    }

    /**
     * @param key
     * @return Float Value
     */
    public float getFloat(String key) {
        Object ret = get(key);
        if (!(ret instanceof Float)) {

            logger.severe("Kein float Parameter für " + key);

            return 0;
        }
        return (Float) ret;
    }

    /**
     * @return the gaps
     */
    public int[] getGaps() {
        return gaps;
    }

    /**
     * @param key
     * @return IntegerValue
     */
    public int getInteger(String key) {

        Object ret = get(key);
        if (!(ret instanceof Integer)) {

            logger.severe("Kein Integer Parameter für " + key);

            return 0;
        }
        return (Integer) ret;
    }

    /**
     * @return the jacCommands
     */
    public Vector<String[]> getJacCommands() {
        return jacCommands;
    }

    /**
     * @param key
     * @return Long Value
     */
    public long getLong(String key) {
        Object ret = get(key);
        if (ret instanceof Long) return (Long) ret;
        if (ret instanceof Integer) {
            logger.severe("Casting Integer to Long");
            return (Integer) ret;
        }
        logger.severe("No Long,Integer found!");
        return 0;
    }

    /**
     * @param key
     * @return StringValue
     */
    public String getString(String key) {

        Object ret = get(key);
        if (!(ret instanceof String)) {

            logger.severe("Kein String Parameter für " + key);

            return null;
        }
        return (String) ret;
    }

    private void init() {
        /**
         * Prozentwert. Ab dieser Schwelle an Korektheit wird ein Letter als 100% richtig gewertet
         */
        set("LetterSearchLimitPerfectPercent", 10.0);

        /**
         * Kontrastwert für die Erkennung ob ein Pixel Farblich zu einem objekt passt (Kontrast Objektdurchschnitt/pixel)
         */
        set("objectColorContrast", 0.3);
        /**
         * Kontrastwert zur erkennung eines ObjektPixels (Kontrast Objekt/hintergrund)
         */
        set("objectDetectionContrast", 0.5);
        /**
         * Minimale Objektfläche für die Objekterkennung
         */
        set("minimumObjectArea", 200);

        /**
         * GIbt an Ob die langsammere Object Detection anstelle der sonst Üblichen Reihen Detection verwendet werden soll
         */
        set("useobjectDetection", false);
        set("useColorObjectDetection", false);
        set("colorObjectDetectionPercent", 15);
        set("colorObjectDetectionRunningAverage", 255);

        /**
         * Parameter: Gibt die Anzahl der Reihen(Pixel) an die zur peak detection verwendet werden sollen
         */
        set("gapWidthPeak", 1);

        /**
         * Gibt die Verwendete Farbkombination an. Siehe JAC Script Docu für genauere Infos
         */
        setColorType("h");

        /**
         * Parameter: Gibt die Anzahl der reihen an die zur Average Detection verwendet werden sollen
         */
        set("gapWidthAverage", 1);

        /**
         * Parameter: gapAndAverageLogic=true: Es werden Lücken verwendet bei denen Peak und Average detection zusammenfallen (AND) gapAndAverageLogic=false: Es
         * werden sowohl peak als Auch Average Lücken verwendet (nur in Ausnahmefällen) (OR)
         */
        set("gapAndAverageLogic", true);

        /**
         * Parameter: Der Kontrastwert für die Average Detection. ~1
         */
        set("gapDetectionAverageContrast", 1.3);

        /**
         * Parameter: Der Kontrastwert für die Peak Detection. ~0.25
         */
        set("gapDetectionPeakContrast", 0.25);

        set("useSpecialGetLetters", "");
        set("useLetterFilter", "");
        set("useLettercomparatorFilter", "");
        set("preValueFilter", "");
        set("postValueFilter", "");
        set("comparatorExtension", "");

        set("LetterSearchLimitFalsePercent", 100);
        /**
         * Parameter: Average Detection verwenden
         */
        set("useAverageGapDetection", false);
        /**
         * Winkel Schrittgröße bei der automatischen Ausrichtung
         */
        set("alignAngleSteps", 5);

        set("splitGapsOverlap", 4);
        /**
         * Linker winkelrand beim Scannen
         */
        set("scanAngleLeft", 0);
        /**
         * Rechter Winkelrand beim Scannen
         */
        set("scanAngleRight", 0);
        /**
         * WInkelschritte beim Scannen
         */
        set("scanAngleSteps", 5);
        /**
         * Parameter: Peak Detection verwenden
         */
        set("usePeakGapdetection", true);

        /**
         * Parameter: Kontrollwert über die minimale Buchstabenbreite
         */
        set("minimumLetterWidth", 0);

        /**
         * Parameter: Wert gibt an um welchen faktor die Fingerprints verkleinert werden. So groß wie möglich, so klein wie nötig Wenn dieser Wert verändert
         * wird, wrd die MTH File unbrauchbar und muss neu trainiert werden
         */
        set("simplifyFaktor", 1.0);
        /*
         * A: captchabild | B: datenbankbild
         */
        /**
         * Gewichtung des A Fehlers
         */
        set("errorAWeight", 1.0);
        /**
         * Gewichtung des B fehlers
         */
        set("errorBWeight", 1.0);
        /**
         * brucht die erkennung ab wennd ei object erkennung fehlgeschlagen hat
         */
        set("cancelIfObjectDetectionFailed", false);
        /**
         * Gewichtung des größenunterschieds Ausschnitt/Originale(Datenbank) Ist ein datebank bcuhstabe nur teilweise auf dem Trefferausschnitt, verschkechtert
         * das die Wertung
         * 
         */
        set("intersectionDimensionWeight", 0.9);

        /**
         * Gibt an wie viele überlagerungsstörungen ausgefiltert werden sollen -1 :>keine 0: bei 0 Nachbarn 1: bei höchstem einen Nachbarn etc
         */
        set("overlayNoiseSize", -1);
        /**
         * Gewichtung der A-Abdeckung
         */
        set("coverageFaktorAWeight", 1.0);
        /**
         * Gewichtung der B-Abdeckung
         */
        set("coverageFaktorBWeight", 1.0);
        /**
         * Reliabilitywert ab dem ein Quickscan gültig ist
         */
        set("quickScanReliabilityLimit", 2.0);
        /**
         * ValityPercentWert ab dem ein quickscan angenommen wird (perfectmatch)
         */
        set("quickScanValityLimit", 50.0);

        /**
         * Minimale Objektgröße die als Zerklüftung gezählt wird
         */
        set("minCleftSize", 3);
        /**
         * Gewichtung der Schittmengenzerküftuntung
         */
        set("cleftFaktor", 0.3);
        /**
         * Schwellwert für den quickscanfilter
         */
        set("preScanFilter", 100);

        /**
         * Schwellwert für den quickscanfilter. Wird dieser emergeny Wert gesetzt, so wird bei Erfolgloser Suche der filter auf diesen wert gesetzt und ein 2.
         * Lauf gestartet
         */
        set("preScanEmergencyFilter", 0);
        /**
         * Anzahl der Linien der prescan auswerten soll
         */
        set("preScanFaktor", 3);
        /**
         * Anzahld er Pixel die beim Trennen von Objekten doppelt gezählt werden (überlappung)
         */
        set("splitPixelObjectsOverlap", 4);

        /**
         * Parameter: Allgemeiner Bildkontrastparameter ~0.8 bis 1.2
         */
        set("relativeContrast", 0.85);

        /**
         * Parameter: Gibt die Tolleranz beim Säubern des Hintergrunds an ~0.05-0.5
         */
        set("backgroundSampleCleanContrast", 0.1);

        /**
         * Parameter: Gibt für dieverse SW Umwandlungen den Schwellwert an
         */
        set("blackPercent", 0.1);

        /**
         * Gibt an ob beim Training nur falscherkannte Buchstaben gespeichert werden (true) oder alle (False)
         */
        set("trainOnlyUnknown", true);
        /**
         * Gibt an ob bei EasyCaptcha kleine objekte entfernt werden sollen
         * 
         */
        set("easyCaptchaRemoveSmallObjects", true);
        /**
         * Gibt an ob bei EasyCaptcha die Buchstaben als Schwarzweiß gewertet werden sollen
         * 
         */
        set("easyCaptchaBW", false);

        /**
         * Gibt an ob bei der objekterenntung pixeln gefolg twerden die quer liegen.
         */
        set("followXLines", true);
        set("turnDB", false);

        /**
         * Parameter: Scan-Parameter. Gibt an um wieviele Pixel sich Letter und Vergleichsletter unterscheiden dürfen um verglichen zu werden. Hohe Werte machen
         * das ganze Langsam
         */
        set("borderVarianceX", 0);
        set("borderVarianceY", 0);
        /**
         * Spezielles extra für rapidshare filtert hunde raus
         */
        set("rapidshareSpecial", false);
        /**
         * Die Zeichenanzahl wird automatisch gesetzt
         */
        set("autoLetterNum", false);
        /**
         * Gleich nach der Objekterkennung wird versucht den buchstaben zu erkennung so können zusammenhängende Buchstaben miterkannt werden
         */
        set("directLetterDetection", false);

        set("prescandivider", 4.0);
        set("divider", 6.0);
        /**
         * Parameter: Scan-Parameter. Gibt an um wieviele Pixel Letter und Vergleichsletter gegeneinander verschoben werden um die beste Übereinstimung zu
         * finden. Hohe werte verlangemmen die Erkennung deutlich
         */
        set("scanVarianceX", 0);
        set("scanVarianceY", 0);

        /**
         * BestehenBuchstaben aus mehreren getrennten parts, werdens ie durch die object detection getrennt. Dieser wert gibt für ein neues objekt an wie weit
         * es sich vom mittelpunkt des letzten objekts entfernt aufhalten darf ohne zum letzten gezählt zu werden. beispiel: 7.0: der innere kreis von einer 0
         * wird zum äußeren gezählt wenn sich der innere nicht weiter als 7 % vom mittelunkte des äußeren befindet.
         */
        set("multiplePartMergeMinSize", 0);

        set("abortDirectDetectionOnDetectionError", false);

        /**
         * InverseFontWeight Unterschreitet die Pixelanzahl einer Intersection einen gewissen Teil der Intersectionfläche, wird der fehler auf 100% angehoben.
         * Beispiel: inverseFontWeight=8 Die gemeinsammen Pixel einer 400 px² Intersection betragen nur 20 pixel. 20*8 = 160; 160<400 =>Der Treffer wird nicht
         * gewertet, da die Intersection zu wenig gemeinsamme Pixel hat.
         */
        set("inverseFontWeight", 8.0);

        set("scanstepx", 1);
        set("scanstepy", 1);
        set("intersectionAHeightWeight", 0.0);
        set("intersectionAWidthWeight", 0.0);
        set("minObjectSize", 10);
    }

    /**
     * Diese methode nimmt eine zeile der script.jas entgegen und parsed sie
     * 
     * @param cmd
     *            Zeile eines *.jas scripts
     * @return Array:{Befehltyp, befehl,parameter}
     */
    private String[] parseCommand(String cmd) {
        String[] ret = new String[3];
        String[] matches;
        cmd = "#" + cmd + "#";
        if ((matches = new Regex(cmd, "#(.*?)=(.*?)#").getRow(0)) != null) {

            ret[0] = "parameter";
            ret[1] = matches[0].trim();
            ret[2] = matches[1].replaceAll("\\\"", "").trim();

        } else if ((matches = new Regex(cmd, "#(.*?)\\((.*?)\\)#").getRow(0)) != null) {
            ret[0] = "function";
            ret[1] = matches[0].trim();
            ret[2] = matches[1].replaceAll("\\\"", "").trim();
            if (ret[2].length() == 0) {
                ret[2] = null;
            }
        } else if ((matches = new Regex(cmd, "#(.*?)\\((.*?)\\)#").getRow(0)) != null) {
            ret[0] = "function";
            ret[1] = matches[0].trim();
            ret[2] = null;
        }
        return ret;
    }

    /**
     * Diese Methode liest das script.jas ein. und parsed es
     */
    private void parseScriptFile() {

        logger.fine("parsing Script.jas");

        File f = JDUtilities.getResourceFile(JDUtilities.getJACMethodsDirectory() + method + "/" + scriptFile);
        String script = JDIO.readFileToString(f);

        logger.info("JAC GET: " + f);
        if (script == null || script.length() == 0) {

            logger.severe("Keine Script.jas vorhanden  ");

            return;

        }
        String[] lines = Regex.getLines(script);
        Vector<String[]> localCaptchaPrepareCommands = new Vector<String[]>();
        Vector<String[]> localJacCommands = new Vector<String[]>();
        Vector<String[]> localLetterCommands = new Vector<String[]>();
        int startAt;
        String[] pcmd;
        for (int i = 0; i < lines.length; i++) {

            lines[i] = lines[i].trim();

            if (lines[i].indexOf("#") == 0 || lines[i].trim().length() < 3) {
                // leere Zeile, oder Comment
                continue;
            }
            if (!lines[i].substring(lines[i].length() - 1).equals(";")) {

                logger.severe(method + "/script.jas: Syntax error (; missing?) near line " + i + ": " + lines[i]);

                return;
            }
            lines[i] = lines[i].substring(0, lines[i].length() - 1);
            if ((startAt = lines[i].indexOf("captcha.prepare.")) == 0) {
                pcmd = parseCommand(lines[i].substring(startAt + 16));

                localCaptchaPrepareCommands.add(pcmd);
            } else if ((startAt = lines[i].indexOf("param.")) == 0) {
                pcmd = parseCommand(lines[i].substring(startAt + 6));
                if (!pcmd[0].equals("parameter")) {

                    logger.severe(method + "/script.jas: Syntax (parameter1)  error near line " + i + ": " + lines[i]);

                }

                localJacCommands.add(pcmd);
            } else if ((startAt = lines[i].indexOf("letter.prepare.")) == 0) {
                pcmd = parseCommand(lines[i].substring(startAt + 15));

                localLetterCommands.add(pcmd);

            } else {

                logger.severe(method + "/script.jas: Syntax error near line " + i + ": " + lines[i]);

            }
        }
        captchaPrepareCommands = localCaptchaPrepareCommands;
        jacCommands = localJacCommands;
        letterCommands = localLetterCommands;

    }

    /**
     * Setzt einen neuen parameter
     * 
     * @param key
     * @param value
     */
    public void set(String key, Object value) {
        key = key.toLowerCase();
        parameter.put(key, value);
    }

    /**
     * @param type
     *            the ColorType to set
     */
    public void setColorType(String type) {
        set("colorType", type);
        int[] components = { 3, 3, 3 };
        colorFaktor = 1;

        for (int i = type.length() - 1; i >= 0; i--) {
            if (type.charAt(i) == 'h') {
                components[i + 3 - type.length()] = 0;
                colorFaktor *= 256;
                color = 0;
            } else if (type.charAt(i) == 's') {
                components[i + 3 - type.length()] = 1;
                colorFaktor *= 256;
                color = 0;
            } else if (type.charAt(i) == 'b') {
                components[i + 3 - type.length()] = 2;
                colorFaktor *= 256;
                color = 0;
            } else if (type.charAt(i) == 'R') {
                components[i + 3 - type.length()] = 0;
                colorFaktor *= 256;
                color = 1;
            } else if (type.charAt(i) == 'G') {
                components[i + 3 - type.length()] = 1;
                colorFaktor *= 256;
                color = 1;
            } else if (type.charAt(i) == 'B') {
                components[i + 3 - type.length()] = 2;
                colorFaktor *= 256;
                color = 1;
            }
        }

        colorComponents = components;
    }

    /**
     * @param gaps
     *            the gaps to set
     */
    public void setGaps(int[] gaps) {

        logger.finer("SET PARAMETER: [gaps] = " + Arrays.toString(gaps));

        this.gaps = gaps;
    }

    /**
     * Wandelt einen String in den Passenden datentyp um
     * 
     * @param arg
     * @param key
     * @return neuer datentyp
     */
    private Object toType(String arg, String key) {
        Object current = get(key);
        if (current instanceof String) return arg;
        if (current instanceof Integer) return Integer.parseInt(arg);
        if (current instanceof Float) return Float.parseFloat(arg);
        if (current instanceof Double) return Double.parseDouble(arg);
        if (current instanceof Boolean) return arg.equalsIgnoreCase("true");

        if (current == null) {
            logger.severe("Parameter " + key + " ist nicht initialisiert worden!");
        } else {
            logger.severe(current + "Typ " + current.getClass() + " wird nicht unterstützt");
        }

        return null;
    }

}