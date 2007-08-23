package jd.captcha;

import java.io.File;
import java.io.IOException;

import java.net.URL;
import java.util.Hashtable;
import java.util.Vector;
import java.util.logging.Logger;

/**
 * Diese Klasse parsed das JAC Script
 * 
 * @author coalado
 */
public class JACScript {
    /**
     * Logger
     */
    private Logger                    logger          = UTILITIES.getLogger();

    /**
     * Vector für die Befehle die für die Vorverarbeitung des Captchas verwendet
     * werden. (script.jas)
     */
    private Vector<String[]>          captchaPrepareCommands;

    /**
     * Vector für die Befehle die für die Ekennung allgemein gelten (script.jas)
     */
    private Vector<String[]>          jacCommands;

    private Vector<String[]>          letterCommands;

    /**
     * Werte-Array Wird gaps != null, so werden die Werte als Trennpositionen
     * für die letter detection verwendet. Alle anderen Erkennungen werden dann
     * ignoriert
     */
    private int[]                     gaps;

    /**
     * nternes Farbarray. Hier werden die Eingaben über setColorFormat abgelegt
     */
    private int[]                     colorComponents = { 3, 3, 3 };
    /**
     * Internet Umrechnungsfaktor. Jenach verwendetem farbmodell. Wird
     * automatisch gesetzt
     */
    private int                       colorFaktor;
    /**
     * Farbwert für den verwendeten Farbraum. 0: hsb 1: RGB
     */
    private int                       color;
    /**
     * owner
     */
    private JAntiCaptcha              owner;
    /**
     * Adresse zum Jacscript
     */
    private URL                       scriptFile;
    /**
     * Methodenname
     */
    private String                    method;

    /**
     * Hashtable für die parameter
     */
    private Hashtable<String, Object> parameter       = new Hashtable<String, Object>();

    /**
     * @param owner
     * @param jarFile
     *            Die Jar Datei mit den Methods
     * @param script
     *            Der Skriptfile in der JAR, das ausgelesen werden soll
     * @param method
     *            Name der Methode, die genutzt wird
     */
    public JACScript(JAntiCaptcha owner, URL script, String method) {

        try {
            init();
            this.owner = owner;
            this.method = method;
            this.scriptFile = script;          
            this.parseScriptFile();
            this.executeParameterCommands();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void init() {
        /**
         * Prozentwert. Ab dieser Schwelle an Korektheit wird ein Letter als
         * 100% richtig gewertet
         */
        set("letterSearchLimitValue", 0.15);

        /**
         * Pixel die Beim Quickscan übersprungen werden (1-...)
         */
        set("quickScan", 1);

        /**
         * Schwellfaktor für den Quickscan Filter
         */
        set("quickScanFilter", 0.3);

        /**
         * Kontrastwert für die Erkennung ob ein Pixel Farblich zu einem objekt
         * passt (Kontrast Objektdurchschnitt/pixel)
         */
        set("objectColorContrast", 0.3);
        /**
         * Kontrastwert zur erkennung eines ObjektPixels (Kontrast
         * Objekt/hintergrund)
         */
        set("objectDetectionContrast", 0.5);
        /**
         * Minimale Objektfläche für die Objekterkennung
         */
        set("minimumObjectArea", 200);

        /**
         * GIbt an Ob die langsammere Object Detection anstelle der sonst
         * Üblichen Reihen Detection verwendet werden soll
         */
        set("useobjectDetection", false);

        /**
         * Parameter: Gibt die Anzahl der Reihen(Pixel) an die zur peak
         * detection verwendet werden sollen
         */
        set("gapWidthPeak", 1);

        /**
         * Gibt die Verwendete Farbkombination an. Siehe JAC Script Docu für
         * genauere Infos
         */
        setColorType("h");

        /**
         * Parameter: Gibt die Anzahl der reihen an die zur Average Detection
         * verwendet werden sollen
         */
        set("gapWidthAverage", 1);

        /**
         * Parameter: gapAndAverageLogic=true: Es werden Lücken verwendet bei
         * denen Peak und Average detection zusammenfallen (AND)
         * gapAndAverageLogic=false: Es werden sowohl peak als Auch Average
         * Lücken verwendet (nur in Ausnahmefällen) (OR)
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

        /**
         * Parameter: Average Detection verwenden
         */
        set("useAverageGapDetection", false);

        set("alignAngleSteps", 5);
        /**
         * Parameter: Peak Detection verwenden
         */
        set("usePeakGapdetection", true);

        /**
         * Parameter: Kontrollwert über die minimale Buchstabenbreite
         */
        set("minimumLetterWidth", 10);

        /**
         * Parameter: Wert gibt an um welchen faktor die Fingerprints
         * verkleinert werden. So groß wie möglich, so klein wie nötig Wenn
         * dieser Wert verändert wird, wrd die MTH File unbrauchbar und muss neu
         * trainiert werden
         */
        set("simplifyFaktor", 1);

        /**
         * Parameter: Allgemeiner Bildkontrastparameter ~0.8 bis 1.2
         */
        set("relativeContrast", 0.85);

        /**
         * Parameter: Gibt die Tolleranz beim Säubern des Hintergrunds an
         * ~0.05-0.5
         */
        set("backgroundSampleCleanContrast", 0.1);

        /**
         * Parameter: Gibt für dieverse SW Umwandlungen den Schwellwert an
         */
        set("blackPercent", 0.1);

        /**
         * Gibt an ob beim Training nur falscherkannte Buchstaben gespeichert
         * werden (true) oder alle (False)
         */
        set("trainOnlyUnknown", true);

        /**
         * Parameter: Scan-Parameter. Gibt an um wieviele Pixel sich Letter und
         * Vergleichsletter unterscheiden dürfen um verglichen zu werden. Hohe
         * Werte machen das ganze Langsam
         */
        set("borderVariance", 0);

        /**
         * Parameter: Scan-Parameter. Gibt an um wieviele Pixel Letter und
         * Vergleichsletter gegeneinander verschoben werden um die beste
         * Übereinstimung zu finden. Hohe werte verlangemmen die Erkennung
         * deutlich
         */
        set("scanVariance", 0);
        
        
        set("findBadLettersRatio",0.3);
       
        set("letterMapTollerance",10);
        
        set("ignoreLetterMapLimit",20);

    }

    /**
     * Setzt einen neuen parameter
     * @param key
     * @param value
     */
    public void set(String key, Object value) {
        key=key.toLowerCase();
        if(get(key)==null){
            logger.info("INIT Parameter: "+key+" = "+value+"("+value.getClass().getName()+")"); 
        }else{
        logger.info("Update Parameter: "+key+" = "+value+"("+value.getClass().getName()+")");
        }
        parameter.put(key, value);
    }

    /**
     * 
     * @param key
     * @return Wert passend zu key. casting nötig!
     */
    public Object get(String key) {
        key=key.toLowerCase();
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

    /**
     * @param key
     * @return IntegerValue
     */
    public int getInteger(String key) {
       
        Object ret = get(key);
        if (!(ret instanceof Integer)) {
            logger.severe("Kein Integer Parameter für " + key+" ("+ret.getClass().getName()+")");
            return 0;
        }
        return (Integer) ret;
    }

    /**
     * @param key
     * @return Long Value
     */
    public long getLong(String key) {
        Object ret = get(key);
        if (!(ret instanceof Integer)) {
            logger.severe("Kein Long Parameter für " + key);
            return 0;
        }
        return (Long) ret;
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
 * Wandelt einen String in den Passenden datentyp um
 * @param arg
 * @param key
 * @return neuer datentyp
 */
    private Object toType(String arg, String key) {
        Object current=get(key);
        if (current instanceof String) {
            return arg;
        }
        if (current instanceof Integer) {
            return Integer.parseInt(arg);
        }
        if (current instanceof Float) {
            return Float.parseFloat(arg);
        }
        if (current instanceof Double) {
            return Double.parseDouble(arg);
        }
        if (current instanceof Boolean) {
            return arg.equalsIgnoreCase("true");
        }
        if(current==null){
            logger.severe("Parameter "+key+" ist nicht initialisiert worden!");
        }else{
            logger.severe(current+"Typ " + current.getClass() + " wird nicht unterstützt");
        }
        return null;
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
                        this.setGaps(newGaps);
                    } else if (cmd[1].equalsIgnoreCase("colortype")) {
                        this.setColorType(cmd[2]);

                    } else {
                       
                        this.set(cmd[1].toLowerCase(), this.toType(cmd[2], cmd[1]));
                    }

                } else {
                    logger.severe("Syntax Error in " + method + "/+script.jas ("+cmd[0]+")");
                }
            }
        } catch (Exception e) {
            logger.severe("Syntax Error in " + method + "/+script.jas");
            e.printStackTrace();
        }

    }

    /**
     * Diese Methode führt die zuvor eingelesenen JAc Script Prepare Befehle aus
     * 
     * @param captcha
     */
    public void executePrepareCommands(Captcha captcha) {
        if (captcha.isPrepared()) {
            // ISt schon prepared
            return;
        }

        logger.fine("Execute Script.jas Prepare scripts");
        captcha.setPrepared(true);
        String[] params;
        try {
            for (int i = 0; i < this.captchaPrepareCommands.size(); i++) {
                String[] cmd = captchaPrepareCommands.elementAt(i);
                logger.fine("Execute Function: " + cmd[1] + "(" + cmd[2] + ")");

                if (cmd[0].equals("parameter")) {
                    logger.severe("Syntax Error in " + method + "/+script.jas");
                    // captchaPrepareCommands

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
                    }

                    else if (cmd[1].equalsIgnoreCase("saveImageasJpg")) {

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
                    } else {
                        logger.severe("Error in " + method + "/+script.jas : Function not valid: " + cmd[1] + "(" + cmd[2] + ")");
                    }

                } else if (cmd[0].equals("function") && (params = cmd[2].split("\\,")).length == 3) {
                    if (cmd[1].equalsIgnoreCase("cleanWithMask")) {
                        captcha.cleanWithMask(owner.createCaptcha(UTILITIES.loadImage(new File(params[0].trim()))), Integer.parseInt(params[1].trim()), Integer.parseInt(params[2].trim()));
                        continue;
                    } else if (cmd[1].equalsIgnoreCase("removeSmallObjects")) {
                        captcha.removeSmallObjects(Double.parseDouble(params[0].trim()), Double.parseDouble(params[1].trim()), Integer.parseInt(params[2].trim()));
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
                    }

                } else if (cmd[0].equals("function") && (params = cmd[2].split("\\,")).length == 5) {

                } else if (cmd[0].equals("function") && (params = cmd[2].split("\\,")).length == 6) {

                }

            }
        } catch (Exception e) {
            logger.severe("Syntax Error in " + method + "/script.jas");

        }
        // BasicWindow.showImage(captcha.getImage(),120,80);
    }

    /**
     * @param letter
     */
    public void executeLetterPrepareCommands(Letter letter) {

        logger.fine("Execute Script.jas Letter Prepare scripts");

        String[] params;
        try {
            for (int i = 0; i < this.letterCommands.size(); i++) {
                String[] cmd = letterCommands.elementAt(i);
                logger.info("Execute Function: " + cmd[1] + "(" + cmd[2] + ")");

                if (cmd[0].equals("parameter")) {
                    logger.severe("Syntax Error in " + method + "/+script.jas");
                    // captchaPrepareCommands

                } else if (cmd[0].equals("function") && cmd[2] == null) {
                if (cmd[1].equalsIgnoreCase("normalize")) {
                    letter.normalize();
                    continue;
                }
            
                } else if (cmd[0].equals("function") && (params = cmd[2].split("\\,")).length == 1) {

                    if (cmd[1].equalsIgnoreCase("toBlackAndWhite")) {
                        letter.toBlackAndWhite(Double.parseDouble(params[0].trim()));
                        continue;
                    } else if (cmd[1].equalsIgnoreCase("reduceWhiteNoise")) {
                        letter.reduceWhiteNoise(Integer.parseInt(params[0].trim()));
                        continue;
                    } else if (cmd[1].equalsIgnoreCase("normalize")) {
                        letter.normalize(Double.parseDouble(params[0].trim()));
                        continue;
                    }  else if (cmd[1].equalsIgnoreCase("reduceBlackNoise")) {
                        letter.reduceBlackNoise(Integer.parseInt(params[0].trim()));
                        continue;
                    } else if (cmd[1].equalsIgnoreCase("blurIt")) {
                        letter.blurIt(Integer.parseInt(params[0].trim()));
                        continue;
                    } else if (cmd[1].equalsIgnoreCase("sampleDown")) {
                        letter.sampleDown(Integer.parseInt(params[0].trim()));
                        continue;
                    } else if (cmd[1].equalsIgnoreCase("cleanBackgroundByColor")) {
                        letter.cleanBackgroundByColor(Integer.parseInt(params[0].trim()));
                        continue;
                    }
                } else if (cmd[0].equals("function") && (params = cmd[2].split("\\,")).length == 2) {
                    if (cmd[1].equalsIgnoreCase("reduceWhiteNoise")) {
                        letter.reduceWhiteNoise(Integer.parseInt(params[0].trim()), Double.parseDouble(params[1].trim()));
                        continue;
                    } else if (cmd[1].equalsIgnoreCase("reduceBlackNoise")) {
                        letter.reduceBlackNoise(Integer.parseInt(params[0].trim()), Double.parseDouble(params[1].trim()));
                        continue;
                   
                    } else {
                        logger.severe("Error in " + method + "/+script.jas : Function not valid: " + cmd[1] + "(" + cmd[2] + ")");
                    }
                } else if (cmd[0].equals("function") && (params = cmd[2].split("\\,")).length == 3) {
                    if (cmd[1].equalsIgnoreCase("removeSmallObjects")) {

                        letter.removeSmallObjects(Double.parseDouble(params[0].trim()), Double.parseDouble(params[1].trim()), Integer.parseInt(params[2].trim()));
                        continue;
                    }  else if (cmd[1].equalsIgnoreCase("align")){
                        logger.info(letter.getDim()+"ä");
                       letter.align( Double.parseDouble(params[0].trim()),Integer.parseInt(params[1].trim()),Integer.parseInt(params[2].trim()));
                       logger.info(letter.getDim()+"bä");
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

        }
        // BasicWindow.showImage(captcha.getImage(),120,80);
    }

    /**
     * Diese Methode liest das script.jas ein. und parsed es
     */
    private void parseScriptFile() throws IOException {
        logger.fine("parsing Script.jas");

        String script = UTILITIES.getFromInputStream(scriptFile.openStream());
        String[] lines = script.split("\r\n");
        if (lines.length == 1)
            lines = script.split("\n\r");
        if (lines.length == 1)
            lines = script.split("\n");
        if (lines.length == 1)
            lines = script.split("\r");
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
        this.captchaPrepareCommands = localCaptchaPrepareCommands;
        this.jacCommands = localJacCommands;
        this.letterCommands = localLetterCommands;

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
        if ((matches = UTILITIES.getMatches(cmd, "#°=°#")) != null) {

            ret[0] = "parameter";
            ret[1] = matches[0].trim();
            ret[2] = matches[1].replaceAll("\\\"", "").trim();

        } else if ((matches = UTILITIES.getMatches(cmd, "#°(°)#")) != null) {
            ret[0] = "function";
            ret[1] = matches[0].trim();
            ret[2] = matches[1].replaceAll("\\\"", "").trim();
            if (ret[2].length() == 0)
                ret[2] = null;
        } else if ((matches = UTILITIES.getMatches(cmd, "#°()#")) != null) {
            ret[0] = "function";
            ret[1] = matches[0].trim();
            ret[2] = null;
        }
        return ret;
    }

    /**
     * @return the captchaPrepareCommands
     */
    public Vector<String[]> getCaptchaPrepareCommands() {
        return captchaPrepareCommands;
    }

    /**
     * @return the jacCommands
     */
    public Vector<String[]> getJacCommands() {
        return jacCommands;
    }

    /**
     * @return the gaps
     */
    public int[] getGaps() {
        return gaps;
    }

    /**
     * @param gaps
     *            the gaps to set
     */
    public void setGaps(int[] gaps) {
        logger.finer("SET PARAMETER: [gaps] = " + gaps.toString());

        this.gaps = gaps;
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
            }
            if (type.charAt(i) == 's') {
                components[i + 3 - type.length()] = 1;
                colorFaktor *= 256;
                color = 0;
            }
            if (type.charAt(i) == 'b') {
                components[i + 3 - type.length()] = 2;
                colorFaktor *= 256;
                color = 0;
            }
            if (type.charAt(i) == 'R') {
                components[i + 3 - type.length()] = 0;
                colorFaktor *= 256;
                color = 1;
            }
            if (type.charAt(i) == 'G') {
                components[i + 3 - type.length()] = 1;
                colorFaktor *= 256;
                color = 1;
            }
            if (type.charAt(i) == 'B') {
                components[i + 3 - type.length()] = 2;
                colorFaktor *= 256;
                color = 1;
            }
        }

        colorComponents = components;
    }

    /**
     * @return Aktueller Colorfaktor (255^verwendete farbkomponenten)
     */
    public int getColorFaktor() {
        return colorFaktor;
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
     * @return the color
     */
    public int getColorFormat() {
        return color;
    }

}