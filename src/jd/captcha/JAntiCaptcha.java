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

package jd.captcha;

import java.awt.Color;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.IndexColorModel;
import java.awt.image.PixelGrabber;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Vector;
import java.util.logging.Logger;

import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextArea;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import jd.captcha.configuration.JACScript;
import jd.captcha.gui.BasicWindow;
import jd.captcha.gui.ImageComponent;
import jd.captcha.gui.ScrollPaneWindow;
import jd.captcha.pixelgrid.Captcha;
import jd.captcha.pixelgrid.Letter;
import jd.captcha.pixelgrid.PixelGrid;
import jd.captcha.pixelobject.PixelObject;
import jd.captcha.utils.UTILITIES;
import jd.utils.JDUtilities;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Diese Klasse stellt alle public Methoden zur captcha erkennung zur Verfügung.
 * Sie verküpft Letter und captcha Klassen. Gleichzeitig dient sie als
 * Parameter-Dump.
 * 
 * @author JD-Team
 */
public class JAntiCaptcha {

    /**
     * Static counter. Falls zu debug zecen mal global ein counter gebraucht
     * wird
     */
    public static int counter = 0;

    /**
     * Static counter. Falls zu debug zecen mal global ein counter gebraucht
     * wird
     */
    public static int counterB = 0;

    /**
     * Logger
     */
    private static Logger logger = UTILITIES.getLogger();

    /**
     * Gibt den Captchacode zurück
     * 
     * @param img
     * @param methodPath
     * @param methodname
     * @return Captchacode
     */
    public static String getCaptchaCode(Image img, String methodPath, String methodname) {
        JAntiCaptcha jac = new JAntiCaptcha(methodPath, methodname);
        // BasicWindow.showImage(img);
        Captcha cap = jac.createCaptcha(img);
        if (cap == null) {
            if (JAntiCaptcha.isLoggerActive()) logger.severe("Captcha Bild konnte nicht eingelesen werden");
            return "JACerror";
        }
        // BasicWindow.showImage(cap.getImageWithGaps(2));
        String ret = jac.checkCaptcha(cap);
        if (JAntiCaptcha.isLoggerActive()) logger.info("captcha text:" + ret);
        return ret;
    }

    /**
     * @param path
     * @return Gibt die Pfade zu allen methoden zurück
     */
    public static File[] getMethods(String path) {
        File dir = JDUtilities.getResourceFile(path);

        if (dir == null || !dir.exists()) {
            if (JAntiCaptcha.isLoggerActive()) logger.severe("Resource dir nicht gefunden: " + path);

        }

        File[] entries = dir.listFiles(new FileFilter() {
            public boolean accept(File pathname) {
                // if(JAntiCaptcha.isLoggerActive())logger.info(pathname.getName());
                if (pathname.isDirectory() && new File(pathname.getAbsoluteFile() + UTILITIES.FS + "jacinfo.xml").exists()&&!JDUtilities.getLocalFile(new File(pathname.getAbsoluteFile() + UTILITIES.FS + "jacinfo.xml")).contains("disabled")) {

                    return true;
                } else {
                    return false;
                }
            }

        });
        return entries;
    }

    /**
     * Gibt zurück ob die entsprechende Methode verfügbar ist.
     * 
     * @param methodsPath
     * 
     * @param methodName
     * @return true/false
     */
    public static boolean hasMethod(String methodsPath, String methodName) {
        boolean ret=JDUtilities.getResourceFile(methodsPath + "/" + methodName + "/script.jas").exists();
        if(!ret)return false;
        String info=JDUtilities.getLocalFile(JDUtilities.getResourceFile(methodsPath + "/" + methodName + "/jacinfo.xml"));
        if(info.contains("disabled"))return false;
        return true;
    }

    public static boolean isLoggerActive() {
        return JDUtilities.getRunType() == JDUtilities.RUNTYPE_LOCAL;
        // return
        // JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_JAC_LOG,
        // false);

    }

    /**
     * Testet die Angegebene Methode. dabei werden analysebilder erstellt.
     * 
     * @param file
     */
    public static void testMethod(File file) {
        int checkCaptchas = 20;
        String code;
        String inputCode;
        int totalLetters = 0;
        int correctLetters = 0;
        File captchaFile;
        Image img;
        String methodsPath = file.getParentFile().getAbsolutePath();
        String methodName = file.getName();
        File captchaDir = UTILITIES.getFullFile(new String[] { file.getAbsolutePath(), "captchas" });
        if (JAntiCaptcha.isLoggerActive()) logger.info("Test Method: " + methodName + " at " + methodsPath);
        JAntiCaptcha jac = new JAntiCaptcha(methodsPath, methodName);
        File[] entries = captchaDir.listFiles(new FileFilter() {
            public boolean accept(File pathname) {
                // if(JAntiCaptcha.isLoggerActive())logger.info(pathname.getName());
                if (pathname.getName().endsWith(".jpg") || pathname.getName().endsWith(".png") || pathname.getName().endsWith(".gif")) {

                    return true;
                } else {
                    return false;
                }
            }

        });
        ScrollPaneWindow w = new ScrollPaneWindow(jac);
        w.setTitle(" Test Captchas: " + file.getAbsolutePath());

        w.resizeWindow(100);
        if (JAntiCaptcha.isLoggerActive()) logger.info("Found Testcaptchas: " + entries.length);
        int testNum = Math.min(checkCaptchas, entries.length);
        if (JAntiCaptcha.isLoggerActive()) logger.info("Test " + testNum + " Captchas");
        int i = 0;
        for (i = 0; i < testNum; i++) {
            captchaFile = entries[(int) (Math.random() * entries.length)];

            logger.info("JJJJJJJJ" + captchaFile);
            img = UTILITIES.loadImage(captchaFile);
            w.setText(0, i, captchaFile.getName());
            w.setImage(1, i, img);

            w.repack();

            jac = new JAntiCaptcha(methodsPath, methodName);
            // BasicWindow.showImage(img);
            Captcha cap = jac.createCaptcha(img);
            if (cap == null) {
                if (JAntiCaptcha.isLoggerActive()) logger.severe("Captcha Bild konnte nicht eingelesen werden");
                continue;
            }

            w.setImage(2, i, cap.getImage());
            // BasicWindow.showImage(cap.getImageWithGaps(2));
            code = jac.checkCaptcha(cap);
            w.setImage(3, i, cap.getImage());

            w.setText(4, i, "JAC:" + code);

            w.repack();

            inputCode = UTILITIES.prompt("Bitte Captcha Code eingeben", code);

            w.setText(5, i, "User:" + inputCode);
            w.repack();
            if (code == null) code = "";
            if (inputCode == null) inputCode = "";
            code = code.toLowerCase();
            inputCode = inputCode.toLowerCase();
            for (int x = 0; x < inputCode.length(); x++) {
                totalLetters++;

                if (inputCode.length() == code.length() && inputCode.charAt(x) == code.charAt(x)) {
                    correctLetters++;
                }
            }
            if (JAntiCaptcha.isLoggerActive()) logger.info("Erkennung: " + correctLetters + "/" + totalLetters + " = " + UTILITIES.getPercent(correctLetters, totalLetters) + "%");
        }
        w.setText(0, i + 1, "Erkennung: " + UTILITIES.getPercent(correctLetters, totalLetters) + "%");
        w.setText(4, i + 1, "Richtig: " + correctLetters);
        w.setText(5, i + 1, "Falsch: " + (totalLetters - correctLetters));
        UTILITIES.showMessage("Erkennung: " + correctLetters + "/" + totalLetters + " = " + UTILITIES.getPercent(correctLetters, totalLetters) + "%");
    }

    /**
     * Führt einen testlauf mit den übergebenen methoden durch
     * 
     * @param methods
     */
    public static void testMethods(File[] methods) {
        for (int i = 0; i < methods.length; i++) {
            testMethod(methods[i]);
        }

    }

    /**
     * fenster die eigentlich nur zur entwicklung sind um Basic GUI Elemente zu
     * haben
     */
//    private BasicWindow bw1;

    private BasicWindow bw2;

    private BasicWindow bw3;

    @SuppressWarnings("unused")
    private BasicWindow bw4;

    private JFrame f;

    /**
     * Bildtyp. Falls dieser von jpg unterschiedlich ist muss zuerst konvertiert
     * werden
     */

    private String imageType;
    /**
     * jas Script Instanz. Sie verarbneitet das JACScript und speichert die
     * Parameter
     */
    public JACScript jas;
    /**
     * Vector mit den Buchstaben aus der MTHO File
     */
    public LinkedList<Letter> letterDB;

    private int[][] letterMap = null;

    /**
     * Anzahl der Buchstaben im Captcha. Wird aus der jacinfo.xml gelesen
     */
    private int letterNum;

    /**
     * Name des Authors der entsprechenden methode. Wird aus der jacinfo.xml
     * Datei geladen
     */
    private String methodAuthor;
    
    /**
     * ordnername der methode
     */
    private String methodDirName;

    /**
     * Methodenname. Wird aus der jacinfo.xml geladen
     */
    private String methodName;

    /**
     * Pfad zur Resulotfile. dort wird der Captchacode hineingeschrieben.
     * (standalone mode)
     */
    private String resultFile;

    private boolean showDebugGui = false;

    /**
     * Pfad zum SourceBild (Standalone). Wird aus der jacinfo.xml gelesen
     */
    private String sourceImage;

    private Vector<ScrollPaneWindow> spw = new Vector<ScrollPaneWindow>();

    private Captcha workingCaptcha;

    /**
     * @param methodsPath
     *            debug
     */

    public JAntiCaptcha(String methodsPath, String methodName) {
        methodsPath = UTILITIES.getFullPath(new String[] { methodsPath, methodName, "" });
        logger.info(methodsPath);
        this.methodDirName = methodName;

        getJACInfo();

        jas = new JACScript(this, methodDirName);
        loadMTHFile();
        if (JAntiCaptcha.isLoggerActive()) logger.fine("letter DB loaded: Buchstaben: " + letterDB.size());

    }

    /**
     * prüft den übergebenen Captcha und gibtd en Code als String zurück das
     * lettersarray des Catchas wird dabei bearbeitet. es werden decoedvalue,
     * avlityvalue und parent gesetzt WICHTIG: Nach dem Decoden eines Captcha
     * herrscht verwirrung. Es stehen unterschiedliche methoden zur verfügung um
     * an bestimmte Informationen zu kommen: captcha.getDecodedLetters() gibt
     * Die letter aus der datenbank zurück. Deren werte sind nicht fest. Auf den
     * Wert von getvalityvalue und getValityPercent kann man sich absolut nicht
     * verlassen. Einzig getDecodedValue() lässt sich zuverlässig auslesen
     * captcha.getLetters() gibt die Wirklichen Letter des captchas zurück. Hier
     * lassen sich alle wichtigen infos abfragen. z.B. ValityValue,
     * ValityPercent, Decodedvalue, etc. Wer immer das hier liest sollte auf
     * keinen fall den fehler machen und sich auf Wert aus dem getdecodedLetters
     * array verlassen
     * 
     * @param captcha
     *            Captcha instanz
     * @return CaptchaCode
     */
    @SuppressWarnings("unchecked")
    public String checkCaptcha(Captcha captcha) {

        this.workingCaptcha = captcha;
        // Führe prepare aus
        jas.executePrepareCommands(captcha);
        Letter[] letters = captcha.getLetters(getLetterNum());
        if (letters == null){
            captcha.setValityPercent(100.0);
            return null;
        }
        // LetterComperator[] newLetters = new LetterComperator[letters.length];
        String ret = "";
        double correct = 0;
        LetterComperator akt;

        if (letters == null) {
            captcha.setValityPercent(100.0);
            if (JAntiCaptcha.isLoggerActive()) logger.severe("Captcha konnte nicht erkannt werden!");
            return null;
        }
        // if (letters.length != this.getLetterNum()) {
        // captcha.setValityPercent(100.0);
        // if(JAntiCaptcha.isLoggerActive())logger.severe("Captcha konnte nicht
        // erkannt werden!2");
        // return null;
        // }

        // Scannen
        Vector<LetterComperator> newLettersVector = new Vector<LetterComperator>();
        for (int i = 0; i < letters.length; i++) {
            letters[i].id = i;
            if (letters[i].detected != null) {
                akt = letters[i].detected;
            } else {
                akt = getLetter(letters[i]);
            }
            akt.getA().id = i;

            newLettersVector.add(akt);

        }
        if (letters.length > getLetterNum()) {
            // sortieren
            Collections.sort(newLettersVector, new Comparator<LetterComperator>() {
                public int compare(LetterComperator obj1, LetterComperator obj2) {

                    if (obj1.getValityPercent() < obj2.getValityPercent()) return -1;
                    if (obj1.getValityPercent() > obj2.getValityPercent()) return 1;
                    return 0;
                }
            });

            // schlechte entfernen
            if (JAntiCaptcha.isLoggerActive()) logger.info(getLetterNum() + "");
            for (int i = newLettersVector.size() - 1; i >= getLetterNum(); i--) {
                newLettersVector.remove(i);
            }

            // Wieder in die richtige reihenfolge sortieren
            Collections.sort(newLettersVector, new Comparator<LetterComperator>() {
                public int compare(LetterComperator obj1, LetterComperator obj2) {

                    if (obj1.getA().id < obj2.getA().id) return -1;
                    if (obj1.getA().id > obj2.getA().id) return 1;
                    return 0;
                }
            });
        }

        if (getJas().getString("useLettercomparatorFilter") != null && getJas().getString("useLettercomparatorFilter").length() > 0) {
            String[] ref = getJas().getString("useLettercomparatorFilter").split("\\.");
            if (ref.length != 2) {
                captcha.setValityPercent(100.0);
                if (JAntiCaptcha.isLoggerActive()) logger.severe("useLettercomparatorFilter should have the format Class.Method");
                return null;
            }
            String cl = ref[0];
            String methodname = ref[1];

            Class newClass;
            try {
                newClass = Class.forName("jd.captcha.specials." + cl);

                Class[] parameterTypes = new Class[] { newLettersVector.getClass(), this.getClass() };
                Method method = newClass.getMethod(methodname, parameterTypes);
                Object[] arguments = new Object[] { newLettersVector, this };
                Object instance = null;
                method.invoke(instance, arguments);

            } catch (Exception e) {
                if (JAntiCaptcha.isLoggerActive()) logger.severe("Fehler in useLettercomparatorFilter:" + e.getLocalizedMessage() + " / " + getJas().getString("useLettercomparatorFilter"));
                e.printStackTrace();
            }

        }
        for (int i = 0; i < newLettersVector.size(); i++) {
            akt = newLettersVector.get(i);

            if (akt == null || akt.getValityPercent() >= 100.0) {
                ret += "-";
                correct += 100.0;
            } else {
                ret += akt.getDecodedValue();

                akt.getA().id = i;
                correct += akt.getValityPercent();

            }
            // if(JAntiCaptcha.isLoggerActive())logger.finer("Validty: " +
            // correct);
        }
        if (newLettersVector.size() == 0) {
            captcha.setValityPercent(100.0);

            return null;
        }
        captcha.setLetterComperators(newLettersVector.toArray(new LetterComperator[] {}));

        if (JAntiCaptcha.isLoggerActive()) logger.finer("Vality: " + ((int) (correct / newLettersVector.size())));
        captcha.setValityPercent(correct / (double) newLettersVector.size());
if(ret==null){
    captcha.setValityPercent(100.0);
}
        return ret;
    }

    /**
     * Gibt den erkannten CaptchaText zurück
     * 
     * @param captchafile
     *            Pfad zum Bild
     * @return CaptchaCode
     */
    public String checkCaptcha(File captchafile) {
        if (JAntiCaptcha.isLoggerActive()) logger.finer("check " + captchafile);
        Image captchaImage = UTILITIES.loadImage(captchafile);
        Captcha captcha = createCaptcha(captchaImage);
        captcha.setCaptchaFile(captchafile);
        // captcha.printCaptcha();
        return checkCaptcha(captcha);
    }

    /**
     * Factory Methode zur Captcha erstellung
     * 
     * @param captchaImage
     *            Image instanz
     * @return captcha
     */
    public Captcha createCaptcha(Image captchaImage) {
        if (captchaImage.getWidth(null) <= 0 || captchaImage.getHeight(null) <= 0) {
            if (JAntiCaptcha.isLoggerActive()) logger.severe("Image Dimensionen zu klein. Image hat keinen Inahlt. Pfad/Url prüfen!");
            return null;
        }

        Captcha ret = Captcha.getCaptcha(captchaImage, this);
        if (ret == null) return null;
        ret.setOwner(this);
        return ret;
    }

    /**
     * Aus gründen der geschwindigkeit wird die MTH XMl in einen vector
     * umgewandelt
     */
    private void createLetterDBFormMTH(Document mth) {
        letterDB = new LinkedList<Letter>();
        long start1 = UTILITIES.getTimer();
        try {

            if (mth == null || mth.getFirstChild() == null) return;
            NodeList nl = mth.getFirstChild().getChildNodes();
            Letter tmp;
            for (int i = 0; i < nl.getLength(); i++) {
                // Get child node
                Node childNode = nl.item(i);
                if (childNode.getNodeName().equals("letter")) {
                    NamedNodeMap att = childNode.getAttributes();

                    tmp = new Letter();
                    tmp.setOwner(this);
                    String id = UTILITIES.getAttribute(childNode, "id");
                    if (!tmp.setTextGrid(childNode.getTextContent())){
                        
                        logger.severe("Error in Letters DB line: "+i+":"+childNode.getTextContent()+" id:"+id);
                        continue;
                    }
                    ;
               
                    if (id != null) {
                        tmp.id = Integer.parseInt(id);
                    }
                    tmp.setSourcehash(att.getNamedItem("captchaHash").getNodeValue());
                    tmp.setDecodedValue(att.getNamedItem("value").getNodeValue());
                    tmp.setBadDetections(Integer.parseInt(UTILITIES.getAttribute(childNode, "bad")));
                    tmp.setGoodDetections(Integer.parseInt(UTILITIES.getAttribute(childNode, "good")));
                    letterDB.add(tmp);
                } else if (childNode.getNodeName().equals("map")) {
                    if (JAntiCaptcha.isLoggerActive()) logger.fine("Parse LetterMap");
                    long start2 = UTILITIES.getTimer();
                    String[] map = childNode.getTextContent().split("\\|");
                    letterMap = new int[map.length][map.length];
                    for (int x = 0; x < map.length; x++) {
                        String[] row = map[x].split("\\,");
                        for (int y = 0; y < map.length; y++) {
                            letterMap[x][y] = Integer.parseInt(row[y]);
                        }

                    }
                    if (JAntiCaptcha.isLoggerActive()) logger.fine("LetterMap Parsing time: " + (UTILITIES.getTimer() - start2));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            if (JAntiCaptcha.isLoggerActive()) logger.severe("Fehler bein lesen der MTH Datei!!. Methode kann nicht funktionieren!");

        }
        if (JAntiCaptcha.isLoggerActive()) logger.fine("Mth Parsing time: " + (UTILITIES.getTimer() - start1));
    }

    /**
     * Diese methode trainiert einen captcha
     * 
     * @param captchafile
     *            File zum Bild
     * @param letterNum
     *            Anzahl der Buchstaben im captcha
     * @return int -1: Übersprungen Sonst: anzahl der richtig erkanten Letter
     */

    private Document createXMLFromLetterDB() {
        Document xml = UTILITIES.parseXmlString("<jDownloader></jDownloader>", false);
        if (letterMap != null) {
            Element element = xml.createElement("map");
            xml.getFirstChild().appendChild(element);
            element.appendChild(xml.createTextNode(this.getLetterMapString()));
        }

        Letter letter;
        Iterator<Letter> iter = letterDB.iterator();
        int i = 0;
        while (iter.hasNext()) {
            letter = (Letter) iter.next();
            Element element = xml.createElement("letter");
            xml.getFirstChild().appendChild(element);
            element.appendChild(xml.createTextNode(letter.getPixelString()));
            element.setAttribute("id", i++ + "");
            element.setAttribute("value", letter.getDecodedValue());
            element.setAttribute("captchaHash", letter.getSourcehash());
            element.setAttribute("good", letter.getGoodDetections() + "");
            element.setAttribute("bad", letter.getBadDetections() + "");

        }
        return xml;

    }

    // /**
    // * TestFunktion - Annimierte Gifs verarbeiten
    // *
    // * @param path
    // */
    // public void mergeGif(File path) {
    // getJas().setColorType("G");
    // GifDecoder d = new GifDecoder();
    // d.read(path.getAbsolutePath());
    // int n = d.getFrameCount();
    //
    // if(JAntiCaptcha.isLoggerActive())logger.fine("Found Frames: " + n);
    // int width = (int) d.getFrameSize().getWidth();
    // int height = (int) d.getFrameSize().getHeight();
    // Captcha merged = new Captcha(width, height);
    // merged.setOwner(this);
    // Captcha tmp;
    //
    // for (int i = 0; i < n; i++) {
    // BufferedImage frame = d.getFrame(i);
    // tmp = new Captcha(width, height);
    // tmp.setOwner(this);
    // PixelGrabber pg = new PixelGrabber(frame, 0, 0, width, height, false);
    // try {
    // pg.grabPixels();
    // } catch (Exception e) {
    // e.printStackTrace();
    // }
    // ColorModel cm = pg.getColorModel();
    // tmp.setColorModel(cm);
    //
    // if (!(cm instanceof IndexColorModel)) {
    // // not an indexed file (ie: not a gif file)
    // tmp.setPixel((int[]) pg.getPixels());
    // } else {
    //
    // tmp.setPixel((byte[]) pg.getPixels());
    // }
    // merged.concat(tmp);
    //
    // }
    //
    // merged.crop(6, 12, 6, 12);
    // // merged.removeSmallObjects(0.3, 0.3);
    // // merged.invert();
    // merged.toBlackAndWhite(0.4);
    // merged.removeSmallObjects(0.3, 0.3, 20);
    // merged.reduceBlackNoise(4, 0.45);
    // merged.toBlackAndWhite(1);
    // // merged.reduceBlackNoise(6, 1.6);
    // // merged.reduceBlackNoise(6, 1.6);
    // // getJas().setBackgroundSampleCleanContrast(0.1);
    // // merged.cleanBackgroundBySample(4, 4, 7, 7);
    //
    // BasicWindow.showImage(merged.getImage(4), 160, 60);
    //
    // }

    private void destroyScrollPaneWindows() {
        while (spw.size() > 0) {
            spw.remove(0).destroy();
        }
    }

    /**
     * Zeigt die Momentane Library an. buchstaben können gelöscht werden
     */
    public void displayLibrary() {
        Letter tmp;
        if (letterDB == null || letterDB.size() == 0) return;
        // final BasicWindow w = BasicWindow.getWindow("Library: " +
        // letterDB.size() + " Datensätze", 400, 300);
        final ScrollPaneWindow w = new ScrollPaneWindow(this);
        w.setTitle("Library: " + letterDB.size() + " Datensätze");
        w.resizeWindow(100);
        sortLetterDB();
        w.setLocationByScreenPercent(5, 5);
        final JAntiCaptcha jac = this;
        final Letter[] list = new Letter[letterDB.size()];
        int x = 0;
        int y = 0;
        int i = 0;
        ListIterator<Letter> iter = letterDB.listIterator(letterDB.size());
        while (iter.hasPrevious()) {
            tmp = (Letter) iter.previous();
            list[i] = tmp;

            w.setText(x, y, tmp.getId() + ": " + tmp.getDecodedValue() + "(" + tmp.getGoodDetections() + "/" + tmp.getBadDetections() + ") Size: " + tmp.toPixelObject(0.85).getSize());
            w.setImage(x + 1, y, tmp.getImage((int) Math.ceil(jas.getDouble("simplifyFaktor"))));
            w.setComponent(x + 3, y, new JButton(new AbstractAction("remove " + i++) {
                private static final long serialVersionUID = 1L;
                public void actionPerformed(ActionEvent evt) {
                    jac.removeLetterFromLibrary(list[Integer.parseInt(((JButton) evt.getSource()).getText().substring(7))]);

                    w.destroy();

                    jac.saveMTHFile();
                    jac.displayLibrary();
                }
            }));

            y++;
            // if (y > 20) {
            // y = 0;
            // x += 6;
            // }
        }
        w.refreshUI();
    }

    /**
     * Exportiert die aktelle Datenbank als PONG einzelbilder
     */
    public void exportDB() {
        File path = UTILITIES.directoryChooser("");
        File file;
        BufferedImage img;
        int i = 0;
        Iterator<Letter> iter = letterDB.iterator();
        while (iter.hasNext()) {
            Letter letter = (Letter) iter.next();
            img = (BufferedImage) letter.getFullImage();
            file = new File(path + "/letterDB_" + this.getMethodName() + "/" + i++ + "_" + letter.getDecodedValue() + ".png");
            file.mkdirs();
            try {
                if (JAntiCaptcha.isLoggerActive()) logger.info("Write Db: " + file);
                ImageIO.write(img, "png", file);
            } catch (IOException e) {

                e.printStackTrace();
            }
        }
    }

    private String getCodeFromFileName(String name, String captchaHash) {
        // captcha_share.gulli.com_codeph2.gif

        String[] matches = UTILITIES.getMatches(name, "captcha_°_code°.°");
        if (matches != null && matches.length > 0) return matches[1];

        return null;
    }

    /**
     * Liest den captchaornder aus
     * 
     * @param path
     * @return File Array
     */
    private File[] getImages(String path) {
        File dir = new File(path);

        if (dir == null || !dir.exists()) {
            if (JAntiCaptcha.isLoggerActive()) logger.severe("Image dir nicht gefunden " + path);

        }
        logger.info(dir + "");
        File[] entries = dir.listFiles(new FileFilter() {
            public boolean accept(File pathname) {
                if (JAntiCaptcha.isLoggerActive()) logger.info(pathname.getName());
                if (pathname.getName().endsWith(".jpg") || pathname.getName().endsWith(".png") || pathname.getName().endsWith(".gif")) {

                    return true;
                } else {
                    return false;
                }
            }

        });
        return entries;

    }

    /**
     * @return the imageType
     */
    public String getImageType() {
        return imageType;
    }

    /*
     * Die Methode parst die jacinfo.xml
     */
    private void getJACInfo() {

        Document doc;
        File f = JDUtilities.getResourceFile("jd/captcha/methods/" + this.methodDirName + "/" + "jacinfo.xml");

        doc = UTILITIES.parseXmlString(JDUtilities.getLocalFile(f), false);
        if (doc == null) {
            if (JAntiCaptcha.isLoggerActive()) logger.severe("" + "jacinfo.xml" + " is missing2");
            return;
        }

        NodeList nl = doc.getFirstChild().getChildNodes();
        for (int i = 0; i < nl.getLength(); i++) {
            // Get child node
            Node childNode = nl.item(i);

            if (childNode.getNodeName().equals("method")) {

                this.setMethodAuthor(UTILITIES.getAttribute(childNode, "author"));
                this.setMethodName(UTILITIES.getAttribute(childNode, "name"));

            }
            if (childNode.getNodeName().equals("format")) {
                try {
                    this.setLetterNum(Integer.parseInt(UTILITIES.getAttribute(childNode, "letterNum")));
                } catch (Exception e) {
                }
                try {
                    this.setLetterMinNum(Integer.parseInt(UTILITIES.getAttribute(childNode, "minLetterNum")));
                } catch (Exception e) {
                }
                try {
                    this.setLetterMaxNum(Integer.parseInt(UTILITIES.getAttribute(childNode, "maxLetterNum")));
                } catch (Exception e) {
                }

                this.setImageType(UTILITIES.getAttribute(childNode, "type"));

            }
            if (childNode.getNodeName().equals("source")) {

                this.setSourceImage(UTILITIES.getAttribute(childNode, "file"));

            }
            if (childNode.getNodeName().equals("result")) {

                this.setResultFile(UTILITIES.getAttribute(childNode, "file"));

            }

        }

    }

    /**
     * @return JACscript Instanz
     */
    public JACScript getJas() {
        return jas;
    }

    /**
     * Vergleicht a und b und gibt eine vergleichszahl zurück. a und b werden
     * gegeneinander verschoben und b wird über die parameter gedreht. Praktisch
     * heißt das, dass derjenige TReffer als gut eingestuft wird, bei dem der
     * Datenbank datensatz möglichst optimal überdeckt wird.
     * 
     * @param a
     *            Original Letter
     * @param b
     *            Vergleichsletter
     * @return int 0(super)-0xffffff (ganz übel)
     */
    @SuppressWarnings("unchecked")
    public LetterComperator getLetter(Letter letter) {
        if (jas.getDouble("quickScanValityLimit") <= 0) {
            logger.info("quickscan disabled");
            return this.getLetterExtended(letter);

        }

        logger.info("Work on Letter:" + letter);
//        long startTime = UTILITIES.getTimer();
        LetterComperator res = null;
        double lastPercent = 100.0;
        int bvX, bvY;
        try {

            if (letterDB == null) {
                if (JAntiCaptcha.isLoggerActive()) logger.severe("letterDB nicht vorhanden");
                return null;
            }

            Letter tmp;

            LetterComperator lc;
            ScrollPaneWindow w = null;
            if (this.isShowDebugGui()) {
                w = new ScrollPaneWindow(this);

                w.setTitle(" Letter " + letter.id);
            }
            bvX = jas.getInteger("borderVarianceX");
            bvY = jas.getInteger("borderVarianceY");
            int line = 0;
            lc = new LetterComperator(letter, null);
            lc.setScanVariance(0, 0);
            lc.setOwner(this);
            res = lc;
            Iterator<Letter> iter = letterDB.iterator();
            int tt = 0;
            logger.info("Do quickscan");
            Method preValueFilterMethod = null;
            Class[] preValueFilterParameterTypes = null;
            Object[] preValueFilterArguments = new Object[] { null, this };
            if (jas.getString("preValueFilter").length() > 0) {
                String[] ref = jas.getString("preValueFilter").split("\\.");
                if (ref.length != 2) {
                    if (JAntiCaptcha.isLoggerActive()) logger.severe("preValueFilter should have the format Class.Method");
                    return null;
                }
                String cl = ref[0];
                String methodname = ref[1];
                Class newClass;
                try {
                    newClass = Class.forName("jd.captcha.specials." + cl);
                    preValueFilterParameterTypes = new Class[] { LetterComperator.class, this.getClass() };
                    preValueFilterMethod = newClass.getMethod(methodname, preValueFilterParameterTypes);

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            Method postValueFilterMethod = null;
            Class[] postValueFilterParameterTypes = null;
            Object[] postValueFilterArguments = new Object[] { null, this };
            if (jas.getString("postValueFilter").length() > 0) {
                String[] ref = jas.getString("postValueFilter").split("\\.");
                if (ref.length != 2) {
                    if (JAntiCaptcha.isLoggerActive()) logger.severe("postValueFilter should have the format Class.Method");
                    return null;
                }
                String cl = ref[0];
                String methodname = ref[1];
                Class newClass;
                try {
                    newClass = Class.forName("jd.captcha.specials." + cl);
                    postValueFilterParameterTypes = new Class[] { LetterComperator.class, this.getClass() };
                    postValueFilterMethod = newClass.getMethod(methodname, postValueFilterParameterTypes);

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            while (iter.hasNext()) {
                tmp = (Letter) iter.next();

                if (Math.abs(tmp.getHeight() - letter.getHeight()) > bvY || Math.abs(tmp.getWidth() - letter.getWidth()) > bvX) {
                    continue;
                }

                lc = new LetterComperator(letter, tmp);
                lc.setScanVariance(0, 0);
                lc.setOwner(this);

                if (preValueFilterMethod != null) {
                    preValueFilterArguments[0] = tmp;
                    preValueFilterArguments[1] = lc;
                    if (!((Boolean) preValueFilterMethod.invoke(null, preValueFilterArguments))) continue;

                }
                lc.run();
                tt++;
                if (this.isShowDebugGui()) {
                    w.setText(0, line, "0° Quick " + (tt));
                    w.setImage(1, line, lc.getA().getImage(2));
                    w.setText(2, line, lc.getA().getDim());
                    w.setImage(3, line, lc.getB().getImage(2));
                    w.setText(4, line, lc.getB().getDim());
                    w.setImage(5, line, lc.getIntersectionLetter().getImage(2));
                    w.setText(6, line, lc.getIntersectionLetter().getDim());
                    w.setText(7, line, lc);
                    line++;
                }
                postValueFilterArguments[0] = lc;
                if (postValueFilterMethod == null || (Boolean) postValueFilterMethod.invoke(null, postValueFilterArguments)) {
                    if (res == null || lc.getValityPercent() < res.getValityPercent()) {
                        if (res != null && res.getValityPercent() < lastPercent) {
                            lastPercent = res.getValityPercent();
                        }
                        res = lc;
                        if (jas.getDouble("LetterSearchLimitPerfectPercent") >= lc.getValityPercent()) {
                            if (JAntiCaptcha.isLoggerActive()) logger.finer(" Perfect Match: " + res.getB().getDecodedValue() + res.getValityPercent() + " good:" + tmp.getGoodDetections() + " bad: " + tmp.getBadDetections() + " - " + res);
                            res.setDetectionType(LetterComperator.QUICKSCANPERFECTMATCH);
                            res.setReliability(lastPercent - res.getValityPercent());
                            return res;
                        }
                        // if(JAntiCaptcha.isLoggerActive())logger.finer("dim "
                        // +
                        // lc.getA().getDim() + "|" + lc.getB().getDim() + " New
                        // Best value: " + lc.getDecodedValue() + " "
                        // +lc.getValityPercent() + " good:" +
                        // tmp.getGoodDetections() + " bad: " +
                        // tmp.getBadDetections() + " - " + lc);
                    } else if (res != null) {
                        if (lc.getValityPercent() < lastPercent) {
                            lastPercent = lc.getValityPercent();
                        }
                    }
                }
            }

        } catch (Exception e) {

            e.printStackTrace();
        }
        if (res != null && res.getB() != null) {
            if (JAntiCaptcha.isLoggerActive()) logger.finer(" Normal Match: " + res.getB().getDecodedValue() + " " + res.getValityPercent() + " good:" + res.getB().getGoodDetections() + " bad: " + res.getB().getBadDetections());
            // if (JAntiCaptcha.isLoggerActive()) logger.fine("Letter erkannt
            // in: " + (UTILITIES.getTimer() - startTime) + " ms");
            res.setReliability(lastPercent - res.getValityPercent());
            if (res.getReliability() >= jas.getDouble("quickScanReliabilityLimit") && res.getValityPercent() < jas.getDouble("quickScanValityLimit")) {
                res.setDetectionType(LetterComperator.QUICKSCANMATCH);
                logger.info("Qickscan found " + res.getValityPercent() + "<" + jas.getDouble("quickScanValityLimit"));
                return res;
            } else {
                if (JAntiCaptcha.isLoggerActive()) logger.warning("Letter nicht ausreichend erkannt. Try Extended " + res.getReliability() + " - " + jas.getDouble("quickScanReliabilityLimit") + " /" + res.getValityPercent() + "-" + jas.getDouble("quickScanValityLimit"));
                return getLetterExtended(letter);
            }
        } else {
            if (JAntiCaptcha.isLoggerActive()) logger.warning("Letter nicht erkannt. Try Extended");
            return getLetterExtended(letter);
        }

    }

    /**
     * Sucht in der MTH ANch dem besten übereinstimmenem letter
     * 
     * @param letter
     *            (refferenz)
     * @return Letter. Beste Übereinstimmung
     */
    @SuppressWarnings("unchecked")
    private LetterComperator getLetterExtended(Letter letter) {
//        long startTime = UTILITIES.getTimer();
        LetterComperator res = null;
        logger.info("Extended SCAN");
        double lastPercent = 100.0;
        JTextArea tf = null;
        try {

            if (letterDB == null) {
                if (JAntiCaptcha.isLoggerActive()) logger.severe("letterDB nicht vorhanden");
                return null;
            }

            Letter tmp;
            int leftAngle = jas.getInteger("scanAngleLeft");
            int rightAngle = jas.getInteger("scanAngleRight");
            if (leftAngle > rightAngle) {
                int temp = leftAngle;
                leftAngle = rightAngle;
                rightAngle = temp;
                if (JAntiCaptcha.isLoggerActive()) logger.warning("param.scanAngleLeft>paramscanAngleRight");
            }
            int steps = Math.max(1, jas.getInteger("scanAngleSteps"));
            boolean turnDB = jas.getBoolean("turnDB");
            int angle;
            Letter orgLetter = letter;
            LetterComperator lc;

            ScrollPaneWindow w = null;
            if (this.isShowDebugGui()) {
                w = new ScrollPaneWindow(this);

                w.setTitle(" Letter " + letter.id);
            }
            int line = 0;
            lc = new LetterComperator(letter, null);
            lc.setOwner(this);
            res = lc;

            Method preValueFilterMethod = null;
            Class[] preValueFilterParameterTypes = null;
            Object[] preValueFilterArguments = new Object[] { null, this };
            if (jas.getString("preValueFilter").length() > 0) {
                String[] ref = jas.getString("preValueFilter").split("\\.");
                if (ref.length != 2) {
                    if (JAntiCaptcha.isLoggerActive()) logger.severe("preValueFilter should have the format Class.Method");
                    return null;
                }
                String cl = ref[0];
                String methodname = ref[1];
                Class newClass;
                try {
                    newClass = Class.forName("jd.captcha.specials." + cl);
                    preValueFilterParameterTypes = new Class[] { LetterComperator.class, this.getClass() };
                    preValueFilterMethod = newClass.getMethod(methodname, preValueFilterParameterTypes);

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            Method postValueFilterMethod = null;
            Class[] postValueFilterParameterTypes = null;
            Object[] postValueFilterArguments = new Object[] { null, this };
            if (jas.getString("postValueFilter").length() > 0) {
                String[] ref = jas.getString("postValueFilter").split("\\.");
                if (ref.length != 2) {
                    if (JAntiCaptcha.isLoggerActive()) logger.severe("postValueFilter should have the format Class.Method");
                    return null;
                }
                String cl = ref[0];
                String methodname = ref[1];
                Class newClass;
                try {
                    newClass = Class.forName("jd.captcha.specials." + cl);
                    postValueFilterParameterTypes = new Class[] { LetterComperator.class, this.getClass() };
                    postValueFilterMethod = newClass.getMethod(methodname, postValueFilterParameterTypes);

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            for (angle = UTILITIES.getJumperStart(leftAngle, rightAngle); UTILITIES.checkJumper(angle, leftAngle, rightAngle); angle = UTILITIES.nextJump(angle, leftAngle, rightAngle, steps)) {

                if (turnDB) {
                    letter = orgLetter;
                } else {
                    letter = orgLetter.turn(angle);
                }
                // if(JAntiCaptcha.isLoggerActive())logger.finer(" Angle " +
                // angle + " : " + letter.getDim());

                Iterator<Letter> iter = letterDB.iterator();
                int tt = 0;
                while (iter.hasNext()) {
                    if (turnDB) {
                        tmp = ((Letter) iter.next()).turn(angle);

                    } else {
                        tmp = ((Letter) iter.next());
                    }

                    if (Math.abs(tmp.getHeight() - letter.getHeight()) > jas.getInteger("borderVarianceY") || Math.abs(tmp.getWidth() - letter.getWidth()) > jas.getInteger("borderVarianceX")) {
                        continue;
                    }

                    lc = new LetterComperator(letter, tmp);
                    lc.setOwner(this);

                    if (preValueFilterMethod != null) {
                        preValueFilterArguments[0] = lc;
                        preValueFilterArguments[1] = this;
                        if (!((Boolean) preValueFilterMethod.invoke(null, preValueFilterArguments))) continue;

                    }
                    lc.run();
                    // if(JAntiCaptcha.isLoggerActive())logger.info("Duration:
                    // "+(UTILITIES.getTimer()-timer) +"
                    // Loops: "+lc.loopCounter);
                    tt++;

                    if (this.isShowDebugGui()) {
                        w.setText(0, line, angle + "° " + (tt));
                        w.setImage(1, line, lc.getA().getImage(2));
                        w.setText(2, line, lc.getA().getDim());
                        w.setImage(3, line, lc.getB().getImage(2));
                        w.setText(4, line, lc.getB().getDim());
                        w.setImage(5, line, lc.getIntersectionLetter().getImage(2));
                        w.setText(6, line, lc.getIntersectionLetter().getDim());

                        w.setComponent(7, line, tf = new JTextArea());
                        tf.setText(lc.toString());
                        if (lc.getPreValityPercent() > jas.getInteger("preScanFilter") && jas.getInteger("preScanFilter") > 0) tf.setBackground(Color.LIGHT_GRAY);
                        line++;
                    }
                    postValueFilterArguments[0] = lc;
                    if (postValueFilterMethod == null || (Boolean) postValueFilterMethod.invoke(null, postValueFilterArguments)) {

                        if (res == null || lc.getValityPercent() < res.getValityPercent()) {
                            if (res != null && res.getValityPercent() < lastPercent) {
                                lastPercent = res.getValityPercent();
                            }
                            res = lc;

                            if (jas.getDouble("LetterSearchLimitPerfectPercent") >= lc.getValityPercent()) {
                                res.setDetectionType(LetterComperator.PERFECTMATCH);
                                res.setReliability(lastPercent - res.getValityPercent());
                                if (JAntiCaptcha.isLoggerActive()) logger.finer(" Perfect Match: " + res.getB().getDecodedValue() + " " + res.getValityPercent() + " good:" + tmp.getGoodDetections() + " bad: " + tmp.getBadDetections() + " - " + res);
                                if (this.isShowDebugGui()) tf.setBackground(Color.GREEN);
                                return res;
                            }
                            if (this.isShowDebugGui()) tf.setBackground(Color.BLUE);
                            if (JAntiCaptcha.isLoggerActive()) logger.finer("Angle " + angle + "dim " + lc.getA().getDim() + "|" + lc.getB().getDim() + " New Best value: " + lc.getDecodedValue() + " " + lc.getValityPercent() + " good:" + tmp.getGoodDetections() + " bad: " + tmp.getBadDetections() + " - " + lc);

                        } else if (res != null) {
                            // if (JAntiCaptcha.isLoggerActive()&&
                            // lc.getDecodedValue().equalsIgnoreCase("G"))
                            // logger.finer("Angle " + angle + "dim " +
                            // lc.getA().getDim() + "|" + lc.getB().getDim() + "
                            // value: " + lc.getDecodedValue() + " " +
                            // lc.getValityPercent() + " good:" +
                            // tmp.getGoodDetections() + " bad: " +
                            // tmp.getBadDetections() + " - " + lc);

                            if (lc.getValityPercent() < lastPercent) {
                                lastPercent = lc.getValityPercent();
                            }
                        }
                    }

                }
                // if(JAntiCaptcha.isLoggerActive())logger.info("Full Angle scan
                // in
                // "+(UTILITIES.getTimer()-startTime2));
            }
            // w.refreshUI();
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (res != null && res.getB() != null) {
            if (JAntiCaptcha.isLoggerActive()) logger.finer(" Normal Match: " + res.getB().getDecodedValue() + " " + res.getValityPercent() + " good:" + res.getB().getGoodDetections() + " bad: " + res.getB().getBadDetections());

            res.setReliability(lastPercent - res.getValityPercent());
        } else {
            if (getJas().getInteger("preScanEmergencyFilter") > getJas().getInteger("preScanFilter")) {
                logger.warning("nicht erkannt. Verwende erweiterte Emergencydatenbank");
                int psf = getJas().getInteger("preScanFilter");
                getJas().set("preScanFilter", getJas().getInteger("preScanEmergencyFilter"));
                LetterComperator ret = this.getLetterExtended(letter);
                getJas().set("preScanFilter", psf);
                return ret;

            }
            if (JAntiCaptcha.isLoggerActive()) logger.severe("Letter entgültig nicht erkannt");
            if (this.isShowDebugGui() && tf != null) tf.setBackground(Color.RED);

        }

        return res;

    }

    /**
     * @return gibt die Lettermap als String zurück
     */
    private String getLetterMapString() {
        StringBuffer ret = new StringBuffer();
        int i = 0;
        for (int x = 0; x < letterMap.length; x++) {
            ret.append("|");
            i++;
            for (int y = 0; y < letterMap[0].length; y++) {

                ret.append(letterMap[x][y]);
                i++;
                ret.append(",");
                i++;
            }
            ret.deleteCharAt(ret.length() - 1);
            if (JAntiCaptcha.isLoggerActive()) logger.fine("Db String: " + ((x * 100) / letterDB.size()) + "%");
        }
        ret.deleteCharAt(0);
        return ret.toString();

    }

    /**
     * @return the letterNum
     */
    public int getLetterNum() {
        return letterNum;
    }

    /**
     * @return the methodAuthor
     */
    public String getMethodAuthor() {
        return methodAuthor;
    }

    /**
     * @return the method
     */
    public String getMethodDirName() {
        return methodDirName;
    }

    /**
     * @return the methodName
     */
    public String getMethodName() {
        return methodName;
    }

    /**
     * Gibt ein FileOebject zu einem resourcstring zurück
     * 
     * @param arg
     * @return File zu arg
     */
    public File getResourceFile(String arg) {
        return JDUtilities.getResourceFile("jd/captcha/methods/" + this.methodDirName + "/" + arg);
    }

    /**
     * @return the resultFile
     */
    public String getResultFile() {
        return resultFile;
    }

    /**
     * @return the sourceImage
     */
    public String getSourceImage() {
        return sourceImage;
    }

    public Captcha getWorkingCaptcha() {
        return workingCaptcha;
    }

    public void importDB() {
        importDB(UTILITIES.directoryChooser(""));
    }

    /**
     * Importiert pNG einzelbilder aus einem ordner und erstellt daraus eine
     * neue db
     */
    public void importDB(File path) {
        letterDB = new LinkedList<Letter>();
        this.getResourceFile("letters.mth").delete();
        Image image;
        Letter letter;
        File[] images = getImages(path.getAbsolutePath());
        for (int i = 0; i < images.length; i++) {
            image = UTILITIES.loadImage(images[i]);
            int width = image.getWidth(null);
            int height = image.getHeight(null);
            if (width <= 0 || height <= 0) {
                if (JAntiCaptcha.isLoggerActive()) logger.severe("ERROR: Image nicht korrekt.");
            }
            PixelGrabber pg = new PixelGrabber(image, 0, 0, width, height, false);
            try {
                pg.grabPixels();
            } catch (Exception e) {
                e.printStackTrace();
            }
            letter = new Letter();

            letter.setOwner(this);

            ColorModel cm = pg.getColorModel();

            if (!(cm instanceof IndexColorModel)) {
                // not an indexed file (ie: not a gif file)

                int[] pixel = (int[]) pg.getPixels();
                int[][] newGrid = new int[width][height];
                int px = 0;
                for (int y = 0; y < height; y++) {
                    for (int x = 0; x < width; x++) {
                        while (pixel[px] < 0)
                            pixel[px] += 0xffffff + 1;
                        // if(JAntiCaptcha.isLoggerActive())logger.info("-
                        // "+pixel[px]);
                        newGrid[x][y] = pixel[px++] < 100 ? 0 : PixelGrid.getMaxPixelValue(this);

                    }
                }
                letter.setGrid(newGrid);
                // letter = letter.align(-40, +40);
                // PixelGrid.fillLetter(letter);
                letter.setSourcehash(UTILITIES.getLocalHash(images[i]));
                letter.setDecodedValue(images[i].getName().split("\\_")[1].split("\\.")[0]);

                letter.clean();

                letterDB.add(letter);

                // letter.resizetoHeight(25);

            } else {

                if (JAntiCaptcha.isLoggerActive()) logger.severe("Bildformat von ImportDB nicht unterstützt");
            }

            // BasicWindow.showImage(ret.getImage());

        }
        this.sortLetterDB();
        this.saveMTHFile();
    }

    /**
     * Prüft ob der übergeben hash in der MTH file ist
     * 
     * @param captchaHash
     * @return true/false
     */
    private boolean isCaptchaInMTH(String captchaHash) {
        if (letterDB == null) return false;
        Iterator<Letter> iter = letterDB.iterator();
        while (iter.hasNext()) {
            if (((Letter) iter.next()).getSourcehash().equals(captchaHash)) return true;
        }

        return false;

    }

    /**
     * @return the showDebugGui
     */
    public boolean isShowDebugGui() {
        return showDebugGui;
    }

    /**
     * MTH File wird geladen und verarbeitet
     */
    private void loadMTHFile() {
        File f = JDUtilities.getResourceFile("jd/captcha/methods/" + this.methodDirName + "/" + "letters.mth");
        String str = "<jDownloader></jDownloader>";
        if (f.exists()) str = JDUtilities.getLocalFile(f);
        Document mth = UTILITIES.parseXmlString(str, false);
        logger.info("Get file: " + f);
        if (mth == null) {
            if (JAntiCaptcha.isLoggerActive()) logger.severe("MTH FILE NOT AVAILABLE.");

        }

        createLetterDBFormMTH(mth);
        // sortLetterDB();

    }

    /**
     * Entfernt Buchstaben mit einem schlechetb Bad/Good verhältniss
     */
    public void removeBadLetters() {
        Letter tmp;
        if (JAntiCaptcha.isLoggerActive()) logger.info("aktuelle DB Size: " + letterDB.size());
        ListIterator<Letter> iter = letterDB.listIterator(letterDB.size());
        while (iter.hasPrevious()) {
            tmp = (Letter) iter.previous();
            if ((tmp.getGoodDetections() == 0 && tmp.getBadDetections() > 0) || ((double) tmp.getBadDetections() / (double) tmp.getGoodDetections()) >= jas.getDouble("findBadLettersRatio")) {
                if (JAntiCaptcha.isLoggerActive()) logger.info("bad Letter entfernt: " + tmp.getDecodedValue() + " (" + tmp.getBadDetections() + "/" + tmp.getGoodDetections() + ")");
                iter.remove();
            }

        }
        if (JAntiCaptcha.isLoggerActive()) logger.info("neue DB Size: " + letterDB.size());

        sortLetterDB();
        saveMTHFile();

    }

    protected void removeLetterFromLibrary(Letter letter) {

        logger.info("Remove" + letter + " : " + letterDB.remove(letter));

    }

    /**
     * Führt diverse Tests durch. file dient als testcaptcha
     * 
     * @param file
     */
    public void runTestMode(File file) {
        Image img = UTILITIES.loadImage(file);
        Captcha captcha = createCaptcha(img);

        BasicWindow.showImage(captcha.getImage(2), "Original bild");
        captcha.testColor();
        BasicWindow.showImage(captcha.getImage(2), "Farbtester. Bild sollte Identisch sein");

        BasicWindow.showImage(captcha.getImage(2));
        // jas.setBackgroundSampleCleanContrast(0.15);
        // captcha.crop(80, 0, 0, 14);
        captcha.cleanBackgroundByColor(14408167);
        // captcha.reduceWhiteNoise(1);
        // captcha.toBlackAndWhite(0.9);

        BasicWindow.showImage(captcha.getImage(2));

        Vector<PixelObject> letters = captcha.getBiggestObjects(4, 200, 0.7, 0.8);
        for (int i = 0; i < letters.size(); i++) {
            PixelObject obj = letters.elementAt(i);
            // BasicWindow.showImage(obj.toLetter().getImage(3));

            Letter l = obj.toLetter();
            l.removeSmallObjects(0.3, 0.5);

            l = l.align(0.5, -45, +45);

            // BasicWindow.showImage(l.getImage(3));
            l.reduceWhiteNoise(2);
            l.toBlackAndWhite(0.6);
            BasicWindow.showImage(l.getImage(1));

        }

        // captcha.crop(13,8,33,8);
        // BasicWindow.showImage(captcha.getImage(3));
        // captcha.blurIt(5);

        //
        // executePrepareCommands(captcha);

        // BasicWindow.showImage(captcha.getImage(3),"With prepare Code");

    }

    /**
     * Speichert die MTH File
     */
    public void saveMTHFile() {
        try {
            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");

            // initialize StreamResult with File object to save to file
            StreamResult result = new StreamResult(new StringWriter());
            DOMSource source = new DOMSource(createXMLFromLetterDB());

            transformer.transform(source, result);

            String xmlString = result.getWriter().toString();

            if (!UTILITIES.writeLocalFile(this.getResourceFile("letters.mth"), xmlString)) {
                if (JAntiCaptcha.isLoggerActive()) logger.severe("MTHO file Konnte nicht gespeichert werden");
            }

        } catch (TransformerException e) {
            e.printStackTrace();
        }

    }

    /**
     * @param imageType
     *            the imageType to set
     */
    public void setImageType(String imageType) {
        if (JAntiCaptcha.isLoggerActive()) logger.finer("SET PARAMETER: [imageType] = " + imageType);
        this.imageType = imageType;
    }

    private void setLetterMaxNum(int parseInt) {

    }

    private void setLetterMinNum(int parseInt) {

    }

    /**
     * @param letterNum
     *            the letterNum to set
     */
    public void setLetterNum(int letterNum) {
        if (JAntiCaptcha.isLoggerActive()) logger.finer("SET PARAMETER: [letterNum] = " + letterNum);
        this.letterNum = letterNum;
    }

    /**
     * @param methodPath
     * @param methodName
     */
    public void setMethod(String methodPath, String methodName) {
        if (JAntiCaptcha.isLoggerActive()) logger.finer("SET PARAMETER: [method] = " + methodDirName);

        this.methodDirName = methodName;

    }

    /**
     * @param methodAuthor
     *            the methodAuthor to set
     */
    public void setMethodAuthor(String methodAuthor) {
        if (JAntiCaptcha.isLoggerActive()) logger.finer("SET PARAMETER: [methodAuthor] = " + methodAuthor);
        this.methodAuthor = methodAuthor;
    }

    /**
     * @param methodName
     *            the methodName to set
     */
    public void setMethodName(String methodName) {
        if (JAntiCaptcha.isLoggerActive()) logger.finer("SET PARAMETER: [methodName] = " + methodName);
        this.methodName = methodName;
    }

    /**
     * @param resultFile
     *            the resultFile to set
     */
    public void setResultFile(String resultFile) {
        if (JAntiCaptcha.isLoggerActive()) logger.finer("SET PARAMETER: [resultFile] = " + resultFile);
        this.resultFile = resultFile;
    }

    /**
     * @param showDebugGui
     *            the showDebugGui to set
     */
    public void setShowDebugGui(boolean showDebugGui) {
        this.showDebugGui = showDebugGui;
    }

    /**
     * @param sourceImage
     *            the sourceImage to set
     */
    public void setSourceImage(String sourceImage) {
        if (JAntiCaptcha.isLoggerActive()) logger.finer("SET PARAMETER: [sourceImage] = " + sourceImage);
        this.sourceImage = sourceImage;
    }

    public void setWorkingCaptcha(Captcha workingCaptcha) {
        this.workingCaptcha = workingCaptcha;
    }

    /**
     * Debug Methode. Zeigt den Captcha in verschiedenen bearbeitungsstadien an
     * 
     * @param captchafile
     */
    public void showPreparedCaptcha(final File captchafile) {

        if (!captchafile.exists()) {
            if (JAntiCaptcha.isLoggerActive()) logger.severe(captchafile.getAbsolutePath() + " existiert nicht");
            return;
        }

        Image captchaImage;
        // if (!this.getImageType().equalsIgnoreCase("jpg")) {
        // captchafile=UTILITIES.toJPG(captchafile);
        // captchaImage = UTILITIES.loadImage(captchafile);
        // if(JAntiCaptcha.isLoggerActive())logger.info("Bild umgewandelt:
        // "+captchafile.getAbsolutePath());
        // captchafile.delete();
        // } else {
        captchaImage = UTILITIES.loadImage(captchafile);
        // }
        BasicWindow.showImage(captchaImage);
        Captcha captcha = createCaptcha(captchaImage);
    
        logger.info("CAPTCHA :_" + checkCaptcha(captcha));
        if (bw3 != null) {
            bw3.dispose();
        }
        bw3 = BasicWindow.showImage(captchaImage, "Captchas");
        bw3.add(new JLabel("ORIGINAL"), UTILITIES.getGBC(2, 0, 2, 2));
        bw3.setLocationByScreenPercent(50, 70);

        bw3.add(new ImageComponent(captcha.getImage(1)), UTILITIES.getGBC(0, 2, 2, 2));
        bw3.add(new JLabel("Farbraum Anpassung"), UTILITIES.getGBC(2, 2, 2, 2));
        jas.executePrepareCommands(captcha);

        bw3.add(new ImageComponent(captcha.getImage(1)), UTILITIES.getGBC(0, 4, 2, 2));
        bw3.add(new JLabel("Prepare Code ausgeführt"), UTILITIES.getGBC(2, 4, 2, 2));

        // Hole die letters aus dem neuen captcha

        // UTILITIES.wait(40000);
        // prüfe auf Erfolgreiche Lettererkennung

        // Decoden. checkCaptcha verwendet dabei die gecachte Erkennung der
        // letters

        Letter[] letters = captcha.getLetters(letterNum);
        if (letters == null) {

            if (JAntiCaptcha.isLoggerActive()) logger.severe("2. Lettererkennung ist fehlgeschlagen!");

            return;

        }

        bw3.add(new ImageComponent(captcha.getImageWithGaps(1)), UTILITIES.getGBC(0, 6, 2, 2));
        bw3.add(new JLabel("Buchstaben freistellen"), UTILITIES.getGBC(2, 6, 2, 2));
        bw3.refreshUI();
        if (bw2 != null) {
            bw2.destroy();
        }
        bw2 = new BasicWindow();
        bw2.setTitle("Freigestellte Buchstaben");
        bw2.setLayout(new GridBagLayout());
        bw2.setSize(300, 300);
        if (JAntiCaptcha.isLoggerActive()) logger.info("display freistellen");
        bw2.setAlwaysOnTop(true);
        bw2.setLocationByScreenPercent(50, 5);
        bw2.add(new JLabel("Aus Captcha:"), UTILITIES.getGBC(0, 0, 2, 2));
        // Letter test=letters[2].getLinedLetter();

        // BasicWindow.showImage(test.getImage(10));
        for (int i = 0; i < letters.length; i++) {

            bw2.add(new ImageComponent(letters[i].getImage((int) Math.ceil(jas.getDouble("simplifyFaktor")))), UTILITIES.getGBC(i * 2 + 2, 0, 2, 2));

        }
        bw2.setVisible(true);
        bw2.pack();
        bw2.setSize(300, bw2.getSize().height);

        LetterComperator[] lcs = captcha.getLetterComperators();
        for (int i = 0; i < lcs.length; i++) {
            if (lcs[i] == null) continue;

            bw2.add(new JLabel("Aus Datenbank:"), UTILITIES.getGBC(0, 6, 2, 2));
            Letter dif = lcs[i].getDifference();
            dif.removeSmallObjects(0.8, 0.8, 5);
            dif.clean();
            if (lcs[i].getB() != null) {
                bw2.add(new ImageComponent(lcs[i].getB().getImage((int) Math.ceil(jas.getDouble("simplifyFaktor")))), UTILITIES.getGBC(i * 2 + 2, 6, 2, 1));

                bw2.add(new ImageComponent(dif.getImage((int) Math.ceil(jas.getDouble("simplifyFaktor")))), UTILITIES.getGBC(i * 2 + 2, 7, 2, 1));

            } else {
                bw2.add(new JLabel("B unknown"), UTILITIES.getGBC(i * 2 + 2, 6, 2, 2));

            }

            // String methodsPath = UTILITIES.getFullPath(new String[] {
            // JDUtilities.getJDHomeDirectoryFromEnvironment().getAbsolutePath(),
            // "jd", "captcha", "methods" });
            // String hoster = "rscat.com";
            // JAntiCaptcha jac = new JAntiCaptcha(methodsPath, hoster);
            // // jac.setShowDebugGui(true);
            // // LetterComperator.CREATEINTERSECTIONLETTER = true;
            // LetterComperator l = jac.getLetter(dif);
            bw2.add(new JLabel("Wert:"), UTILITIES.getGBC(0, 8, 2, 2));
            // bw2.add(new JLabel(lcs[i].getDecodedValue() + " : " +
            // l.getDecodedValue() + " - " + l.getValityPercent()),
            // UTILITIES.getGBC(i * 2 + 2, 8, 2, 2));
            bw2.add(new JLabel("Proz.:"), UTILITIES.getGBC(0, 10, 2, 2));
            bw2.add(new JLabel(lcs[i].getValityPercent() + "%"), UTILITIES.getGBC(i * 2 + 2, 10, 2, 2));

        }
        JButton bt = new JButton("Train");
        
        bw2.add(bt,UTILITIES.getGBC(0, 12, 2, 2));
        bw2.pack();
        bw2.repack();
        
        bt.addActionListener(new ActionListener(){

            public void actionPerformed(ActionEvent e) {
               
                JAntiCaptcha.this.trainCaptcha(captchafile, 4);
                
            }
            
        });
    }

    /**
     * Sortiert die letterDB Nach den bad Detections. Der Sortieralgo gehört
     * dringend überarbeitet!!! Diese Sortieren hilft die GUten Letter zuerst zu
     * prüfen.
     * 
     * @TODO Sortoer ALGO ändern. zu langsam!!
     */
    private void sortLetterDB() {

        // LinkedList<Letter> ret = new LinkedList<Letter>();
        // Iterator<Letter> iter = letterDB.iterator();

        Collections.sort(letterDB, new Comparator<Letter>() {
            public int compare(Letter a, Letter b) {
                return a.getDecodedValue().compareToIgnoreCase(b.getDecodedValue()) * -1;

            }

        });

    }

    /**
     * Diese methode wird aufgerufen um alle captchas im Ordner
     * methods/Methodname/captchas zu trainieren
     * 
     * @param path
     */
    public void trainAllCaptchas(String path) {

        int successFull = 0;
        int total = 0;
        logger.info("TRain " + path);
        File[] images = getImages(path);
        if (images == null) return;
        int newLetters;
        for (int i = 0; i < images.length; i++) {
            if (JAntiCaptcha.isLoggerActive()) logger.fine(images[i].toString());
            int letternum = getLetterNum();
            newLetters = trainCaptcha(images[i], letternum);

            if (JAntiCaptcha.isLoggerActive()) logger.fine("Erkannt: " + newLetters + "/" + getLetterNum());
            if (newLetters > 0) {
                successFull += newLetters;
                total += getLetterNum();
                if (JAntiCaptcha.isLoggerActive()) logger.info("Erkennungsrate: " + ((100 * successFull / total)));
            }
        }

    }

    int trainCaptcha(File captchafile, int letterNum) {

        if (!captchafile.exists()) {
            if (JAntiCaptcha.isLoggerActive()) logger.severe(captchafile.getAbsolutePath() + " existiert nicht");
            return -1;
        }
        if (this.isShowDebugGui()) {
            destroyScrollPaneWindows();
        }
        // Lade das Bild
        Image captchaImage = UTILITIES.loadImage(captchafile);
        // Erstelle hashwert
        String captchaHash = UTILITIES.getLocalHash(captchafile);

        // Prüfe ob dieser captcha schon aufgenommen wurde und überspringe ihn
        // falls ja
        if (isCaptchaInMTH(captchaHash)) {
            if (JAntiCaptcha.isLoggerActive()) logger.fine("Captcha schon aufgenommen" + captchafile);
            return -1;
        }
        // captcha erstellen
        Captcha captcha = createCaptcha(captchaImage);

        String code = null;
        String guess = "";
        // Zeige das OriginalBild
        if (f != null) {
            f.dispose();
        }
        f = new JFrame();
        f.setVisible(true);
        f.setLocation(500, 10);
        f.setLayout(new GridBagLayout());
        f.add(new JLabel("original captcha: " + captchafile.getName()), UTILITIES.getGBC(0, 0, 10, 1));

        f.add(new ImageComponent(captcha.getImage()), UTILITIES.getGBC(0, 1, 10, 1));

        f.setSize(1400, 800);

        f.pack();

        // Führe das Prepare aus
        // jas.executePrepareCommands(captcha);
        // Hole die letters aus dem neuen captcha
        guess = checkCaptcha(captcha);
        Letter[] letters = captcha.getLetters(letterNum);
        // UTILITIES.wait(40000);
        // prüfe auf Erfolgreiche Lettererkennung
        // if (letters == null || letters.length != this.getLetterNum()) {
        // File file = getResourceFile("detectionErrors1/" +
        // (UTILITIES.getTimer()) + "_" + captchafile.getName());
        // file.getParentFile().mkdirs();
        // captchafile.renameTo(file);
        // if(JAntiCaptcha.isLoggerActive())logger.severe("2. Lettererkennung
        // ist fehlgeschlagen!");
        // return -1;
        // }
        if (letters == null) {
            File file = getResourceFile("detectionErrors5/" + (UTILITIES.getTimer()) + "_" + captchafile.getName());
            file.getParentFile().mkdirs();
            captchafile.renameTo(file);
            if (JAntiCaptcha.isLoggerActive()) logger.severe("Letter detection error");
            return -1;
        }
        for (int i = 0; i < letters.length; i++) {
            // if (letters[i] == null || letters[i].getWidth() < 2 ||
            // letters[i].getHeight() < 2) {
            // File file = getResourceFile("detectionErrors5/" +
            // (UTILITIES.getTimer()) + "_" + captchafile.getName());
            // file.getParentFile().mkdirs();
            // captchafile.renameTo(file);
            // if (JAntiCaptcha.isLoggerActive()) logger.severe("Letter
            // detection error");
            // return -1;
            // }
        }

        // Zeige das After-prepare Bild an

        f.add(new JLabel("Letter Detection"), UTILITIES.getGBC(0, 3, 10, 1));

        f.add(new ImageComponent(captcha.getImageWithGaps(1)), UTILITIES.getGBC(0, 4, 10, 1));

        f.add(new JLabel("Seperated"), UTILITIES.getGBC(0, 5, 10, 1));

        for (int i = 0; i < letters.length; i++) {
            f.add(new ImageComponent(letters[i].getImage((int) Math.ceil(jas.getDouble("simplifyFaktor")))), UTILITIES.getGBC(i + 1, 6, 1, 1));

        }
        f.pack();
        // Decoden. checkCaptcha verwendet dabei die gecachte Erkennung der
        // letters

        LetterComperator[] lcs = captcha.getLetterComperators();
        if (lcs == null) {
            File file = getResourceFile("detectionErrors5/" + (UTILITIES.getTimer()) + "_" + captchafile.getName());
            file.getParentFile().mkdirs();
            captchafile.renameTo(file);
            if (JAntiCaptcha.isLoggerActive()) logger.severe("Letter detection error");
            return -1;
        }
        if (lcs.length != letters.length) {
            logger.severe("ACHTUNG. lcs: " + lcs.length + " - letters: " + letters.length);
        }
        if (guess != null /* && guess.length() == getLetterNum() */) {

            for (int i = 0; i < lcs.length; i++) {
                if (lcs[i] != null && lcs[i].getB() != null) {
                    f.add(new ImageComponent(lcs[i].getB().getImage((int) Math.ceil(jas.getDouble("simplifyFaktor")))), UTILITIES.getGBC(i + 1, 8, 1, 1));

                } else {
                    f.add(new JLabel(""), UTILITIES.getGBC(i + 1, 8, 1, 1));

                }
                // bw3.setImage(i + 1, 3, lcs[i].getB().getImage((int)
                // Math.ceil(jas.getDouble("simplifyFaktor"))));

                if (lcs[i] != null && lcs[i].getB() != null) {
                    f.add(new JLabel("" + lcs[i].getDecodedValue()), UTILITIES.getGBC(i + 1, 9, 1, 1));

                    // bw3.setText(i + 1, 4, lcs[i].getDecodedValue());
                } else {
                    f.add(new JLabel(""), UTILITIES.getGBC(i + 1, 9, 1, 1));

                }
                if (lcs[i] != null && lcs[i].getB() != null) {
                    f.add(new JLabel("" + ((double) Math.round(10 * lcs[i].getValityPercent()) / 10.0)), UTILITIES.getGBC(i + 1, 10, 1, 1));

                } else {
                    f.add(new JLabel(""), UTILITIES.getGBC(i + 1, 10, 1, 1));

                }
            }
            f.pack();
        } else {
            if (JAntiCaptcha.isLoggerActive()) logger.warning("Erkennung fehlgeschlagen");
        }
        f.add(new JLabel("prepared captcha"), UTILITIES.getGBC(0, 11, 10, 1));

        f.add(new ImageComponent(captcha.getImage()), UTILITIES.getGBC(0, 12, 10, 1));
        f.pack();
        if (JAntiCaptcha.isLoggerActive()) logger.info("Decoded Captcha: " + guess + " Vality: " + captcha.getValityPercent());
        if (captcha.getValityPercent() >= 0 || true) {

            // if (guess == null) {
            // File file = getResourceFile("detectionErrors2/" +
            // (UTILITIES.getTimer()) + "_" + captchafile.getName());
            // file.getParentFile().mkdirs();
            // captchafile.renameTo(file);
            // if(JAntiCaptcha.isLoggerActive())logger.severe("Letter erkennung
            // fehlgeschlagen");
            // return -1;
            //
            // }
            if (getCodeFromFileName(captchafile.getName(), captchaHash) == null) {
                code = UTILITIES.prompt("Bitte Captcha Code eingeben (Press enter to confirm " + guess, guess);
                if (code != null && code.equals(guess))
                    code = "";
                else if (code == null) {
                    boolean doIt = JOptionPane.showConfirmDialog(new JFrame(), "Ja (yes) = beenden (close) \t Nein (no) = nächstes Captcha (next captcha)") == JOptionPane.OK_OPTION;
                    if (doIt) {
                        System.exit(0);
                    }
                }

            } else {
                code = getCodeFromFileName(captchafile.getName(), captchaHash);
                if (JAntiCaptcha.isLoggerActive()) logger.warning("captcha code für " + captchaHash + " verwendet: " + code);

            }

        } else {
            if (JAntiCaptcha.isLoggerActive()) logger.info("100% ERkennung.. automatisch übernommen");
            // code = guess;
        }
        if (code == null) {
            File file = getResourceFile("detectionErrors3/" + (UTILITIES.getTimer()) + "_" + captchafile.getName());
            file.getParentFile().mkdirs();
            captchafile.renameTo(file);
            if (JAntiCaptcha.isLoggerActive()) logger.severe("Captcha Input error");
            return -1;
        }
        if (code.length() == 0) {
            code = guess;
        }
        if (code.length() != letters.length) {
            File file = getResourceFile("detectionErrors4/" + (UTILITIES.getTimer()) + "_" + captchafile.getName());
            file.getParentFile().mkdirs();
            captchafile.renameTo(file);
            if (JAntiCaptcha.isLoggerActive()) logger.severe("Captcha Input error3");
            return -1;
        }
        if (code.indexOf("-") < 0) {
            String[] oldName = captchafile.getName().split("\\.");
            String ext = oldName[oldName.length - 1];
            String newName = captchafile.getParentFile().getAbsolutePath() + UTILITIES.FS + "captcha_" + this.getMethodDirName() + "_code" + code + "." + ext;
            captchafile.renameTo(new File(newName));
        }
        int ret = 0;
        for (int i = 0; i < letters.length; i++) {
            if (!code.substring(i, i + 1).equals("-")) {
                if (guess != null && code.length() > i && guess.length() > i && code.substring(i, i + 1).equals(guess.substring(i, i + 1))) {
                    ret++;
                    if (lcs[i] != null) {
                        lcs[i].getB().markGood();
                    }

                    if (!jas.getBoolean("TrainOnlyUnknown")) {
                        letters[i].setOwner(this);
                        // letters[i].setTextGrid(letters[i].getPixelString());
                        letters[i].setSourcehash(captchaHash);
                        letters[i].setDecodedValue(code.substring(i, i + 1));
                        letterDB.add(letters[i]);
                        f.add(new JLabel("OK+"), UTILITIES.getGBC(i + 1, 13, 1, 1));

                    } else {
                        f.add(new JLabel("OK-"), UTILITIES.getGBC(i + 1, 13, 1, 1));
                    }
                    f.pack();
                } else {
                    if (JAntiCaptcha.isLoggerActive()) logger.info(letterDB + " - ");
                    if (lcs != null && lcs[i] != null && letterDB.size() > 30 && lcs[i] != null && lcs[i].getB() != null) {
                        lcs[i].getB().markBad();
                    }
                    letters[i].setOwner(this);
                    // letters[i].setTextGrid(letters[i].getPixelString());
                    letters[i].setSourcehash(captchaHash);
                    letters[i].setDecodedValue(code.substring(i, i + 1));

                    letterDB.add(letters[i]);

                    f.add(new JLabel("NO +"), UTILITIES.getGBC(i + 1, 13, 1, 1));
                    f.pack();
                }
            } else {
                f.add(new JLabel("-"), UTILITIES.getGBC(i + 1, 13, 1, 1));
                f.pack();
            }
            // mth.appendChild(element);
        }
        sortLetterDB();
        saveMTHFile();
        return ret;
    }

    //  
    // private static String[] getMethods() {

    //
    // }
}