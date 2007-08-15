package jd.captcha;

import java.io.File;
import java.util.Vector;
import java.util.logging.Logger;

import jd.plugins.Plugin;


/**
 * Diese Klasse parsed das JAC Script
 * 
 * 
 * 
 * @author coalado
 */
public class JACScript {
    /**
     * Logger
     */
    private Logger           logger                        = Plugin.getLogger();

    /**
     * Prozentwert. Ab dieser Schwelle an Korektheit wird ein Letter als 100%
     * richtig gewertet
     */
    private double           letterSearchLimitValue        = 0.15;

     
    
    /**
     * Kontrastwert für die Erkennung ob ein Pixel Farblich zu einem objekt passt (Kontrast  Objektdurchschnitt/pixel)
     */
    private  double objectColorContrast=0.3;
    /**
     * Kontrastwert zur erkennung eines ObjektPixels (Kontrast  Objekt/hintergrund)
     */
    private  double objectDetectionContrast=0.5;
    /**
     * Minimale Objektfläche für die Objekterkennung
     */
    private int minimumObjectArea=200;
        
    /**
     * GIbt an Ob die langsammere Object Detection anstelle der sonst Üblichen Reihen Detection verwendet werden soll
     */
    private boolean useObjectDetection =false;
    


    /**
     * Vector für die Befehle die für die Vorverarbeitung des Captchas verwendet
     * werden. (script.jas)
     */
    private Vector<String[]> captchaPrepareCommands;

    /**
     * Vector für die Befehle die für die Ekennung allgemein gelten (script.jas)
     */
    private Vector<String[]> jacCommands;
    
    private Vector<String[]> letterCommands;
/**
 * Maximaler Drehwinkel nach links
 */
    private int leftAngle=-10;
    /**
     * Maximaler drehwinkel nach rechts
     */
    private int rightAngle=10;
    /**
     * Parameter: Gibt die Anzahl der Reihen(Pixel) an die zur peak detection
     * verwendet werden sollen
     */
    private int              gapWidthPeak                  = 1;

    /**
     * Gibt die Verwendete Farbkombination an. Siehe JAC Script Docu für
     * genauere Infos
     */
    private String           ColorType                     = "h";

    /**
     * Parameter: Gibt die Anzahl der reihen an die zur Average Detection
     * verwendet werden sollen
     */
    private int              gapWidthAverage               = 1;

    /**
     * Parameter: gapAndAverageLogic=true: Es werden Lücken verwendet bei denen
     * Peak und Average detection zusammenfallen (AND) gapAndAverageLogic=false:
     * Es werden sowohl peak als Auch Average Lücken verwendet (nur in
     * Ausnahmefällen) (OR)
     */
    private boolean          gapAndAverageLogic            = true;

    /**
     * Parameter: Der Kontrastwert für die Average Detection. ~1
     */
    private double           gapDetectionAverageContrast   = 1.3;

    /**
     * Parameter: Der Kontrastwert für die Peak Detection. ~0.25
     */
    private double           gapDetectionPeakContrast      = 0.25;

    /**
     * Parameter: Average Detection verwenden
     */
    private boolean          useAverageGapDetection        = false;

    /**
     * Parameter: Peak Detection verwenden
     */
    private boolean          usePeakGapdetection           = true;

    /**
     * Parameter: Kontrollwert über die minimale Buchstabenbreite
     */
    private int              minimumLetterWidth            = 10;

    /**
     * Parameter: Wert gibt an um welchen faktor die Fingerprints verkleinert
     * werden. So groß wie möglich, so klein wie nötig Wenn dieser Wert
     * verändert wird, wrd die MTH File unbrauchbar und muss neu trainiert
     * werden
     */
    private int              simplifyFaktor                = 1;




    /**
     * Parameter: Allgemeiner Bildkontrastparameter ~0.8 bis 1.2
     */
    private double           relativeContrast              = 0.85;

    /**
     * Parameter: Gibt die Tolleranz beim Säubern des Hintergrunds an ~0.05-0.5
     */
    private double           backgroundSampleCleanContrast = 0.1;

    /**
     * Parameter: Gibt für dieverse SW Umwandlungen den Schwellwert an
     */
    private double           blackPercent                  = 0.1;

    /**
     * Werte-Array Wird gaps != null, so werden die Werte als Trennpositionen
     * für die letter detection verwendet. Alle anderen Erkennungen werden dann
     * ignoriert
     */
    private int[]            gaps;


    /**
     * Gibt an ob beim Training nur falscherkannte Buchstaben gespeichert werden
     * (true) oder alle (False)
     */
    private boolean          trainOnlyUnknown              = true;

  

    /**
     * Parameter: Scan-Parameter. Gibt an um wieviele Pixel sich Letter und
     * Vergleichsletter unterscheiden dürfen um verglichen zu werden. Hohe Werte
     * machen das ganze Langsam
     */
    private int              borderVariance                = 0;

    /**
     * Parameter: Scan-Parameter. Gibt an um wieviele Pixel Letter und
     * Vergleichsletter gegeneinander verschoben werden um die beste
     * Übereinstimung zu finden. Hohe werte verlangemmen die Erkennung deutlich
     */
    private int              scanVariance                  = 0;
/**
 * Internes Farbarray. Hier werden die Eingaben über setColorFormat abgelegt
 */
    private int[]            colorComponents               = { 3, 3, 3 };
/**
 * Internet Umrechnungsfaktor. Jenach verwendetem farbmodell. Wird automatisch gesetzt
 */
    private int              colorFaktor;
/**
 * Farbwert für den verwendeten Farbraum. 0: hsb 1: RGB
 */
    private int              color;

    private JAntiCaptcha owner;
    private File scriptFile;
    private String method;
 

    /**
     * @param owner
     * @param script
     */
    public JACScript(JAntiCaptcha owner, File script) {
  
       this.owner=owner;
       this.method=script.getParentFile().getName();
       this.scriptFile=script;
       this.parseScriptFile();
       this.executeParameterCommands();

    }


    /**
     * Diese Methode führt die zuvor eingelesenen JAC Script Befehle aus
     */
    private void executeParameterCommands() {
        if (jacCommands == null || jacCommands.size() == 0) {
            logger.info("KEINE JAC COMMANDS");
            return;
        }
        logger.info("Execute Script.jas Parameter scripts");
        try {
            for (int i = 0; i < jacCommands.size(); i++) {
                String[] cmd = jacCommands.elementAt(i);

                if (cmd[0].equals("parameter")) {
                    if (cmd[1].equalsIgnoreCase("lettersearchlimitvalue"))
                        this.setLetterSearchLimitValue(Double.parseDouble(cmd[2]));
                    else if (cmd[1].equalsIgnoreCase("trainonlyunknown"))
                        this.setTrainOnlyUnknown(cmd[2].equals("true"));
                    else if (cmd[1].equalsIgnoreCase("scanvariance"))
                        this.setScanVariance(Integer.parseInt(cmd[2]));
                    else if (cmd[1].equalsIgnoreCase("bordervariance"))
                        this.setBorderVariance(Integer.parseInt(cmd[2]));          
                    else if (cmd[1].equalsIgnoreCase("simplifyfaktor")) {
                        this.setSimplifyFaktor(Integer.parseInt(cmd[2]));
                    }
                    else if (cmd[1].equalsIgnoreCase("gapwidthpeak"))
                        this.setGapWidthPeak(Integer.parseInt(cmd[2]));
                    else if (cmd[1].equalsIgnoreCase("gapwidthaverage"))
                        this.setGapWidthAverage(Integer.parseInt(cmd[2]));
                    else if (cmd[1].equalsIgnoreCase("ColorType"))
                        this.setColorType(cmd[2]);
                    else if (cmd[1].equalsIgnoreCase("gapandaveragelogic"))
                        this.setGapAndAverageLogic(cmd[2].equals("true"));
                    else if (cmd[1].equalsIgnoreCase("gapdetectionaveragecontrast"))
                        this.setGapDetectionAverageContrast(Double.parseDouble(cmd[2]));
                    else if (cmd[1].equalsIgnoreCase("gapdetectionpeakcontrast"))
                        this.setGapDetectionPeakContrast(Double.parseDouble(cmd[2]));
                    else if (cmd[1].equalsIgnoreCase("useaveragegapdetection"))
                        this.setUseAverageGapDetection(cmd[2].equals("true"));
                    else if (cmd[1].equalsIgnoreCase("usepeakgapdetection"))
                        this.setUsePeakGapdetection(cmd[2].equals("true"));
                    else if (cmd[1].equalsIgnoreCase("minimumletterwidth"))
                        this.setMinimumLetterWidth(Integer.parseInt(cmd[2]));     
                    else if (cmd[1].equalsIgnoreCase("leftAngle"))
                        this.setLeftAngle(Integer.parseInt(cmd[2])); 
                    else if (cmd[1].equalsIgnoreCase("rightAngle"))
                        this.setRightAngle(Integer.parseInt(cmd[2])); 
                    else if (cmd[1].equalsIgnoreCase("relativecontrast"))
                        this.setRelativeContrast(Double.parseDouble(cmd[2]));
                    else if (cmd[1].equalsIgnoreCase("backgroundsamplecleancontrast"))
                        this.setBackgroundSampleCleanContrast(Double.parseDouble(cmd[2]));
                    else if (cmd[1].equalsIgnoreCase("blackpercent"))
                        this.setBlackPercent(Double.parseDouble(cmd[2]));
                    else if (cmd[1].equalsIgnoreCase("objectColorContrast"))
                        this.setObjectColorContrast(Double.parseDouble(cmd[2]));
                    else if (cmd[1].equalsIgnoreCase("objectDetectionContrast"))
                        this.setObjectDetectionContrast(Double.parseDouble(cmd[2]));
                    else if (cmd[1].equalsIgnoreCase("minimumObjectArea"))
                        this.setMinimumObjectArea(Integer.parseInt(cmd[2]));
                    else if (cmd[1].equalsIgnoreCase("useObjectDetection"))
                        this.setUseObjectDetection(cmd[2].equals("true"));
                    else if (cmd[1].equalsIgnoreCase("gaps")) {
                        
                        cmd[2] = cmd[2].substring(1, cmd[2].length() - 1);
                        String[] gaps = cmd[2].split("\\,");
                        int[] newGaps = new int[gaps.length];
                        for (int ii = 0; ii < gaps.length; ii++){
                            
                            newGaps[ii] = Integer.parseInt(gaps[ii]);
                        }
                        this.setGaps(newGaps);          
                      
                        
                        
                    } else {
                        logger.severe("Error in " + method + "/+script.jas : Parameter not valid: " + cmd[1] + " = " + cmd[2]);
                    }

                } else {
                    logger.severe("Syntax Error in " + method + "/+script.jas");
                }
            }
        } catch (Exception e) {
            logger.severe("Syntax Error in " + method + "/+script.jas");
        }

    }

    /**
     * Diese Methode führt die zuvor eingelesenen JAc Script Prepare Befehle aus
     * @param captcha 
     */
    public void executePrepareCommands(Captcha captcha) {
        if (captcha.isPrepared()) {
            // ISt schon prepared
            return;
        }       
       
        logger.info("Execute Script.jas Prepare scripts");
        captcha.setPrepared(true);
        String[] params;
        try {
            for (int i = 0; i < this.captchaPrepareCommands.size(); i++) {
                String[] cmd = captchaPrepareCommands.elementAt(i);
                logger.info("Execute Function: " + cmd[1] + "(" + cmd[2] + ")");
               
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
                    }else if (cmd[1].equalsIgnoreCase("cleanBackgroundByColor")) {
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
                    }else if (cmd[1].equalsIgnoreCase("removeSmallObjects")) {
                        captcha.removeSmallObjects(Double.parseDouble(params[0].trim()), Double.parseDouble(params[1].trim()), Integer.parseInt(params[2].trim()));
                        continue;
                    }else {
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
            
       
        logger.info("Execute Script.jas Letter Prepare scripts");
   
        String[] params;
        try {
            for (int i = 0; i < this.letterCommands.size(); i++) {
                String[] cmd = letterCommands.elementAt(i);
                logger.info("Execute Function: " + cmd[1] + "(" + cmd[2] + ")");
               
                if (cmd[0].equals("parameter")) {
                    logger.severe("Syntax Error in " + method + "/+script.jas");
                    // captchaPrepareCommands

                } else if (cmd[0].equals("function") && cmd[2] == null) {

                   
                } else if (cmd[0].equals("function") && (params = cmd[2].split("\\,")).length == 1) {

                 

                } else if (cmd[0].equals("function") && (params = cmd[2].split("\\,")).length == 2) {
                 
                   
                } else if (cmd[0].equals("function") && (params = cmd[2].split("\\,")).length == 3) {
                    if (cmd[1].equalsIgnoreCase("removeSmallObjects")) {
                       
                        letter.removeSmallObjects(Double.parseDouble(params[0].trim()), Double.parseDouble(params[1].trim()), Integer.parseInt(params[2].trim()));
                        continue;
                    }else {
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
    private void parseScriptFile() {
        logger.info("parsing Script.jas");
       
        String script = UTILITIES.getLocalFile(scriptFile);
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
                logger.info(pcmd.toString());
              
                localLetterCommands.add(pcmd);
                
            } else {
                logger.severe(method + "/script.jas: Syntax error near line " + i + ": " + lines[i]);
            }
        }
        this.captchaPrepareCommands=localCaptchaPrepareCommands;
        this.jacCommands=localJacCommands;
        this.letterCommands=localLetterCommands;
     
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
     * @return the gapAndAverageLogic
     */
    public boolean getGapAndAverageLogic() {
        return gapAndAverageLogic;
    }

    /**
     * @param gapAndAverageLogic
     *            the gapAndAverageLogic to set
     */
    public void setGapAndAverageLogic(boolean gapAndAverageLogic) {
        logger.info("SET PARAMETER: [gapAndAverageLogic] = " + gapAndAverageLogic);
        this.gapAndAverageLogic = gapAndAverageLogic;
    }

    /**
     * @return the gapDetectionAverageContrast
     */
    public double getGapDetectionAverageContrast() {
        return gapDetectionAverageContrast;
    }

    /**
     * @param gapDetectionAverageContrast
     *            the gapDetectionAverageContrast to set
     */
    public void setGapDetectionAverageContrast(double gapDetectionAverageContrast) {
        logger.info("SET PARAMETER: [gapDetectionAverageContrast] = " + gapDetectionAverageContrast);
        this.gapDetectionAverageContrast = gapDetectionAverageContrast;
    }

    /**
     * @return the gapDetectionPeakContrast
     */
    public double getGapDetectionPeakContrast() {
        return gapDetectionPeakContrast;
    }

    /**
     * @param gapDetectionPeakContrast
     *            the gapDetectionPeakContrast to set
     */
    public void setGapDetectionPeakContrast(double gapDetectionPeakContrast) {
        logger.info("SET PARAMETER: [gapDetectionPeakContrast] = " + gapDetectionPeakContrast);
        this.gapDetectionPeakContrast = gapDetectionPeakContrast;
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
        logger.info("SET PARAMETER: [gaps] = "+gaps.toString());
        
        this.gaps = gaps;
    }

    /**
     * @return the gapWidthAverage
     */
    public int getGapWidthAverage() {
        return gapWidthAverage;
    }

    /**
     * @param gapWidthAverage
     *            the gapWidthAverage to set
     */
    public void setGapWidthAverage(int gapWidthAverage) {
        logger.info("SET PARAMETER: [gapWidthAverage] = " + gapWidthAverage);
        this.gapWidthAverage = gapWidthAverage;
    }

    /**
     * @return the gapWidthPeak
     */
    public int getGapWidthPeak() {
        return gapWidthPeak;
    }

    /**
     * @param gapWidthPeak
     *            the gapWidthPeak to set
     */
    public void setGapWidthPeak(int gapWidthPeak) {
        logger.info("SET PARAMETER: [gapWidthPeak] = " + gapWidthPeak);
        this.gapWidthPeak = gapWidthPeak;
    }

   



    /**
     * @return the letterSearchLimitValue
     */
    public double getLetterSearchLimitValue() {
        return letterSearchLimitValue;
    }

    /**
     * @param letterSearchLimitValue
     *            the letterSearchLimitValue to set
     */
    public void setLetterSearchLimitValue(double letterSearchLimitValue) {
        logger.info("SET PARAMETER: [letterSearchLimitValue] = " + letterSearchLimitValue);
        this.letterSearchLimitValue = letterSearchLimitValue;
    }

    /**
     * @return the minimumLetterWidth
     */
    public int getMinimumLetterWidth() {
        return minimumLetterWidth;
    }

    /**
     * @param minimumLetterWidth
     *            the minimumLetterWidth to set
     */
    public void setMinimumLetterWidth(int minimumLetterWidth) {
        logger.info("SET PARAMETER: [minimumLetterWidth] = " + minimumLetterWidth);
        this.minimumLetterWidth = minimumLetterWidth;
    }

    /**
     * @return the useAverageGapDetection
     */
    public boolean isUseAverageGapDetection() {
        return useAverageGapDetection;
    }

    /**
     * @param useAverageGapDetection
     *            the useAverageGapDetection to set
     */
    public void setUseAverageGapDetection(boolean useAverageGapDetection) {
        logger.info("SET PARAMETER: [useAverageGapDetection] = " + useAverageGapDetection);
        this.useAverageGapDetection = useAverageGapDetection;
    }

    /**
     * @return the usePeakGapdetection
     */
    public boolean isUsePeakGapdetection() {

        return usePeakGapdetection;
    }

    /**
     * @param usePeakGapdetection
     *            the usePeakGapdetection to set
     */
    public void setUsePeakGapdetection(boolean usePeakGapdetection) {
        logger.info("SET PARAMETER: [usePeakGapdetection] = " + usePeakGapdetection);
        this.usePeakGapdetection = usePeakGapdetection;
    }

    /**
     * @return the backgroundSampleCleanContrast
     */
    public double getBackgroundSampleCleanContrast() {
        return backgroundSampleCleanContrast;
    }

    /**
     * @param backgroundSampleCleanContrast
     *            the backgroundSampleCleanContrast to set
     */
    public void setBackgroundSampleCleanContrast(double backgroundSampleCleanContrast) {
        logger.info("SET PARAMETER: [backgroundSampleCleanContrast] = " + backgroundSampleCleanContrast);
        this.backgroundSampleCleanContrast = backgroundSampleCleanContrast;
    }

    /**
     * @return the blackPercent
     */
    public double getBlackPercent() {
        return blackPercent;
    }

    /**
     * @param blackPercent
     *            the blackPercent to set
     */
    public void setBlackPercent(double blackPercent) {
        logger.info("SET PARAMETER: [blackPercent] = " + blackPercent);
        this.blackPercent = blackPercent;
    }

 

    /**
     * @return the relativeContrast
     */
    public double getRelativeContrast() {
        return relativeContrast;
    }

    /**
     * @param relativeContrast
     *            the relativeContrast to set
     */
    public void setRelativeContrast(double relativeContrast) {
        logger.info("SET PARAMETER: [relativeContrast] = " + relativeContrast);
        this.relativeContrast = relativeContrast;
    }

    /**
     * @return the simplifyFaktor
     */
    public int getSimplifyFaktor() {
        return simplifyFaktor;
    }

    /**
     * @param simplifyFaktor
     *            the simplifyFaktor to set
     */
    public void setSimplifyFaktor(int simplifyFaktor) {
        logger.info("SET PARAMETER: [simplifyFaktor] = " + simplifyFaktor);
        this.simplifyFaktor = simplifyFaktor;
    }

    /**
     * @return the borderVariance
     */
    public int getBorderVariance() {
        return borderVariance;
    }

    /**
     * @param borderVariance
     *            the borderVariance to set
     */
    public void setBorderVariance(int borderVariance) {
        logger.info("SET PARAMETER: [borderVariance] = " + borderVariance);
        this.borderVariance = borderVariance;
    }

    /**
     * @return the scanVariance
     */
    public int getScanVariance() {
        return scanVariance;
    }

    /**
     * @param scanVariance
     *            the scanVariance to set
     */
    public void setScanVariance(int scanVariance) {
        logger.info("SET PARAMETER: [scanVariance] = " + scanVariance);
        this.scanVariance = scanVariance;
    }

    /**
     * @return the trainOnlyUnknown
     */
    public boolean isTrainOnlyUnknown() {
        return trainOnlyUnknown;
    }

    /**
     * @param trainOnlyUnknown
     *            the trainOnlyUnknown to set
     */
    public void setTrainOnlyUnknown(boolean trainOnlyUnknown) {
        this.trainOnlyUnknown = trainOnlyUnknown;
    }

    /**
     * @return the ColorType
     */
    public String getColorType() {
        return ColorType;
    }
    
  
    /**
     * @param type
     *            the ColorType to set
     */
    public void setColorType(String type) {
        ColorType = type;
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

    /**
     * @return the minimumObjectArea
     */
    public int getMinimumObjectArea() {
        return minimumObjectArea;
    }

    /**
     * @param minimumObjectArea the minimumObjectArea to set
     */
    public void setMinimumObjectArea(int minimumObjectArea) {
        logger.info("SET PARAMETER: [minimumObjectArea] = " + minimumObjectArea);
        this.minimumObjectArea = minimumObjectArea;
    }

    /**
     * @return the objectColorContrast
     */
    public double getObjectColorContrast() {
        return objectColorContrast;
    }

    /**
     * @param objectColorContrast the objectColorContrast to set
     */
    public void setObjectColorContrast(double objectColorContrast) {
        logger.info("SET PARAMETER: [objectColorContrast] = " + objectColorContrast);
        this.objectColorContrast = objectColorContrast;
    }

    /**
     * @return the objectDetectionContrast
     */
    public double getObjectDetectionContrast() {
        return objectDetectionContrast;
    }

    /**
     * @param objectDetectionContrast the objectDetectionContrast to set
     */
    public void setObjectDetectionContrast(double objectDetectionContrast) {
        logger.info("SET PARAMETER: [objectDetectionContrast] = " + objectDetectionContrast);
        this.objectDetectionContrast = objectDetectionContrast;
    }

    /**
     * @return the useObjectDetection
     */
    public boolean isUseObjectDetection() {
       
        return useObjectDetection;
    }

    /**
     * @param useObjectDetection the useObjectDetection to set
     */
    public void setUseObjectDetection(boolean useObjectDetection) {
        logger.info("SET PARAMETER: [useObjectDetection] = " + useObjectDetection);
        this.useObjectDetection = useObjectDetection;
    }


    /**
     * @return the leftAngle
     */
    public int getLeftAngle() {
        return leftAngle;
    }


    /**
     * @param leftAngle the leftAngle to set
     */
    public void setLeftAngle(int leftAngle) {
        this.leftAngle = leftAngle;
    }


    /**
     * @return the rightAngle
     */
    public int getRightAngle() {
        return rightAngle;
    }


    /**
     * @param rightAngle the rightAngle to set
     */
    public void setRightAngle(int rightAngle) {
        this.rightAngle = rightAngle;
    }

}