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

package jd.captcha;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.ListIterator;
import java.util.Vector;
import java.util.logging.Level;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import jd.captcha.configuration.JACScript;
import jd.captcha.gui.BasicWindow;
import jd.captcha.gui.ImageComponent;
import jd.captcha.gui.ScrollPaneWindow;
import jd.captcha.pixelgrid.Captcha;
import jd.captcha.pixelgrid.Letter;
import jd.captcha.utils.Utilities;
import jd.gui.userio.DummyFrame;
import jd.http.Browser;
import jd.nutils.Executer;
import jd.nutils.JDHash;
import jd.nutils.io.JDIO;
import jd.utils.JDUtilities;

import org.appwork.utils.Files;
import org.appwork.utils.Regex;
import org.appwork.utils.ImageProvider.ImageProvider;
import org.appwork.utils.logging2.LogSource;
import org.appwork.utils.swing.WindowManager;
import org.appwork.utils.swing.WindowManager.FrameState;
import org.jdownloader.logging.LogController;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Diese Klasse stellt alle public Methoden zur captcha Erkennung zur Verfügung. Sie verküpft Letter und captcha Klassen. Gleichzeitig dient
 * sie als Parameter-Dump.
 * 
 * @author JD-Team
 */
public class JAntiCaptcha {

    /**
     * Testet die Angegebene Methode. Dabei werden analysebilder erstellt.
     * 
     * @param file
     * @param methodName
     *            TODO
     * @throws InterruptedException
     */
    public static void testMethod(File file, String methodName) throws InterruptedException {
        LogSource logger = LogController.CL();
        int checkCaptchas = 20;
        String code;
        String inputCode;
        int totalLetters = 0;
        int correctLetters = 0;
        File captchaFile;
        Image img;
        if (methodName == null) {
            methodName = file.getName();
        }
        File captchaDir = new File(file.getAbsolutePath() + "/captchas");

        logger.info("Test Method: " + methodName);

        new JAntiCaptcha(methodName);
        File[] entries = captchaDir.listFiles(new FileFilter() {
            public boolean accept(File pathname) {
                // if(Utilities.isLoggerActive())logger.info(pathname.getName(
                // ));
                if (pathname.getName().endsWith(".jpg") || pathname.getName().endsWith(".png") || pathname.getName().endsWith(".gif")) {

                    return true;
                } else {
                    return false;
                }
            }

        });
        ScrollPaneWindow w = new ScrollPaneWindow();
        w.setTitle(" Test Captchas: " + file.getAbsolutePath());

        w.resizeWindow(100);

        logger.info("Found Testcaptchas: " + entries.length);

        int testNum = Math.min(checkCaptchas, entries.length);

        logger.info("Test " + testNum + " Captchas");

        int i = 0;
        for (i = 0; i < testNum; i++) {
            captchaFile = entries[(int) (Math.random() * entries.length)];

            logger.info("JJJJJJJJ" + captchaFile);
            img = Utilities.loadImage(captchaFile);
            w.setText(0, i, captchaFile.getName());
            w.setImage(1, i, img);

            w.repack();

            JAntiCaptcha jac = new JAntiCaptcha(methodName);
            // BasicWindow.showImage(img);
            Captcha cap = jac.createCaptcha(img);
            if (cap == null) {

                logger.severe("Captcha Bild konnte nicht eingelesen werden");

                continue;
            }

            w.setImage(2, i, cap.getImage());
            // BasicWindow.showImage(cap.getImageWithGaps(2));
            code = jac.checkCaptcha(captchaFile, cap);
            w.setImage(3, i, cap.getImage());

            w.setText(4, i, "JAC:" + code);

            w.repack();

            inputCode = JOptionPane.showInputDialog(w, "Bitte Captcha Code eingeben", code);

            w.setText(5, i, "User:" + inputCode);
            w.repack();
            if (code == null) {
                code = "";
            }
            if (inputCode == null) {
                inputCode = "";
            }
            code = code.toLowerCase();
            inputCode = inputCode.toLowerCase();
            for (int x = 0; x < inputCode.length(); x++) {
                totalLetters++;

                if (inputCode.length() == code.length() && inputCode.charAt(x) == code.charAt(x)) {
                    correctLetters++;
                }
            }

            logger.info("Erkennung: " + correctLetters + "/" + totalLetters + " = " + Utilities.getPercent(correctLetters, totalLetters) + "%");

        }
        w.setText(0, i + 1, "Erkennung: " + Utilities.getPercent(correctLetters, totalLetters) + "%");
        w.setText(4, i + 1, "Richtig: " + correctLetters);
        w.setText(5, i + 1, "Falsch: " + (totalLetters - correctLetters));
        JOptionPane.showMessageDialog(new JFrame(), "Erkennung: " + correctLetters + "/" + totalLetters + " = " + Utilities.getPercent(correctLetters, totalLetters) + "%");
    }

    /**
     * Führt einen Testlauf mit den übergebenen Methoden durch
     * 
     * @param methods
     * @throws InterruptedException
     */
    public static void testMethods(File[] methods) throws InterruptedException {
        for (File element : methods) {
            JAntiCaptcha.testMethod(element, null);
        }

    }

    /**
     * Fenster die eigentlich nur zur Entwicklung sind um Basic GUI Elemente zu haben
     */
    private BasicWindow              bw2;

    private BasicWindow              bw3;

    private JDialog                  f;

    /**
     * Bildtyp. Falls dieser von jpg unterschiedlich ist, muss zuerst konvertiert werden.
     */
    private String                   imageType;

    /**
     * jas Script Instanz. Sie verarbneitet das JACScript und speichert die Parameter
     */
    public JACScript                 jas;
    /**
     * Vector mit den Buchstaben aus der MTHO File
     */
    public java.util.List<Letter>    letterDB;

    private int[][]                  letterMap    = null;

    /**
     * Anzahl der Buchstaben im Captcha. Wird aus der jacinfo.xml gelesen
     */
    private int                      letterNum;

    /**
     * ordnername der methode
     */
    private String                   methodDirName;

    private boolean                  showDebugGui = false;

    private Vector<ScrollPaneWindow> spw          = new Vector<ScrollPaneWindow>();

    private Captcha                  workingCaptcha;

    private boolean                  extern;

    public boolean isExtern() {
        return extern;
    }

    private String                      command;

    private String                      dstFile;

    private String                      srcFile;

    private Image                       sourceImage;

    private final LogSource             logger;

    private final JACMethod             method;

    public static final HashSet<String> SYNCMAP = new HashSet<String>();

    public JAntiCaptcha(String methodName) {
        logger = LogController.CL();
        logger.setLevel(Level.OFF);
        method = JACMethod.forServiceName(methodName);
        if (method == null) {
            logger.severe("no such method found! " + methodName);
            return;
        }
        methodDirName = method.getFileName();
        getJACInfo();
        jas = new JACScript(this, methodDirName);
        long time = System.currentTimeMillis();
        loadMTHFile();
        time = System.currentTimeMillis() - time;
        logger.fine("LoadTime: " + time);
        logger.fine("letter DB loaded: Buchstaben: " + letterDB.size());
    }

    /**
     * prüft den übergebenen Captcha und gibt den Code als String zurück. Das lettersarray des Catchas wird dabei bearbeitet. Es werden
     * decoedvalue, avlityvalue und parent gesetzt WICHTIG: Nach dem Decoden eines Captcha herrscht Verwirrung. Es stehen unterschiedliche
     * Methoden zur Verfügung um an bestimmte Informationen zu kommen: captcha.getDecodedLetters() gibt Die letter aus der datenbank zurück.
     * Deren werte sind nicht fest. Auf den Wert von getvalityvalue und getValityPercent kann man sich absolut nicht verlassen. Einzig
     * getDecodedValue() lässt sich zuverlässig auslesen captcha.getLetters() gibt die Wirklichen Letter des captchas zurück. Hier lassen
     * sich alle wichtigen Infos abfragen. z.B. ValityValue, ValityPercent, Decodedvalue, etc. Wer immer das hier liest sollte auf keinen
     * fall den fehler machen und sich auf Wert aus dem getdecodedLetters array verlassen
     * 
     * @param captcha
     *            Captcha instanz
     * @return CaptchaCode
     * @throws InterruptedException
     */
    public String checkCaptcha(File file, Captcha captcha) throws InterruptedException {
        if (extern) return callExtern();
        workingCaptcha = captcha;
        // Führe prepare aus
        jas.executePrepareCommands(file, captcha);
        Letter[] letters = captcha.getLetters(getLetterNum());
        if (letters == null) {
            captcha.setValityPercent(100.0);

            logger.severe("Captcha konnte nicht erkannt werden!");

            return null;
        }
        String ret = "";
        double correct = 0;
        LetterComperator akt;

        // Scannen
        Vector<LetterComperator> newLettersVector = new Vector<LetterComperator>();
        for (int i = 0; i < letters.length; i++) {
            letters[i].setId(i);
            if (letters[i].detected != null) {
                akt = letters[i].detected;
            } else {
                akt = getLetter(letters[i]);
            }
            akt.getA().setId(i);

            newLettersVector.add(akt);

        }
        if (letters.length > getLetterNum()) {
            // sortieren
            Collections.sort(newLettersVector, new Comparator<LetterComperator>() {
                public int compare(LetterComperator obj1, LetterComperator obj2) {

                    if (obj1.getValityPercent() < obj2.getValityPercent()) { return -1; }
                    if (obj1.getValityPercent() > obj2.getValityPercent()) { return 1; }
                    return 0;
                }
            });

            // schlechte entfernen

            logger.info(getLetterNum() + "");

            if (!jas.getBoolean("autoLetterNum")) {
                for (int i = newLettersVector.size() - 1; i >= getLetterNum(); i--) {
                    newLettersVector.remove(i);
                }
            }
            // Wieder in die richtige reihenfolge sortieren
            Collections.sort(newLettersVector, new Comparator<LetterComperator>() {
                public int compare(LetterComperator obj1, LetterComperator obj2) {

                    if (obj1.getA().getId() < obj2.getA().getId()) { return -1; }
                    if (obj1.getA().getId() > obj2.getA().getId()) { return 1; }
                    return 0;
                }
            });
        }

        if (getJas().getString("useLettercomparatorFilter") != null && getJas().getString("useLettercomparatorFilter").length() > 0) {
            String[] ref = getJas().getString("useLettercomparatorFilter").split("\\.");
            if (ref.length != 2) {
                captcha.setValityPercent(100.0);

                logger.severe("useLettercomparatorFilter should have the format Class.Method");

                return null;
            }
            String cl = ref[0];
            String methodname = ref[1];

            Class<?> newClass;
            try {
                newClass = Class.forName("jd.captcha.specials." + cl);

                Class<?>[] parameterTypes = new Class[] { newLettersVector.getClass(), this.getClass() };
                Method method = newClass.getMethod(methodname, parameterTypes);
                Object[] arguments = new Object[] { newLettersVector, this };
                Object instance = null;
                method.invoke(instance, arguments);

            } catch (Exception e) {

                logger.severe("Fehler in useLettercomparatorFilter:" + e.getLocalizedMessage() + " / " + getJas().getString("useLettercomparatorFilter"));

                logger.log(e);
            }

        }
        for (int i = 0; i < newLettersVector.size(); i++) {
            akt = newLettersVector.get(i);

            if (akt == null || akt.getValityPercent() >= 100.0) {
                ret += "-";
                correct += 100.0;
            } else {
                ret += akt.getDecodedValue();

                akt.getA().setId(i);
                correct += akt.getValityPercent();

            }
            // if(Utilities.isLoggerActive())logger.finer("Validty: " +
            // correct);
        }
        if (newLettersVector.size() == 0) {
            captcha.setValityPercent(100.0);

            return null;
        }
        captcha.setLetterComperators(newLettersVector.toArray(new LetterComperator[] {}));

        logger.finer("Vality: " + (int) (correct / newLettersVector.size()));

        captcha.setValityPercent(correct / newLettersVector.size());
        return ret;
    }

    /**
     * Exportiert die aktelle Datenbank als PNG einzelbilder
     */
    public void exportDB() {
        File path = Utilities.directoryChooser();

        File file;
        BufferedImage img;
        int i = 0;
        for (Letter letter : letterDB) {

            img = letter.getFullImage();
            file = new File(path + "/letterDB/" + i++ + "_" + letter.getDecodedValue() + ".png");
            file.mkdirs();
            FileOutputStream fos = null;
            try {
                logger.info("Write Db: " + file);
                fos = new FileOutputStream(file);
                ImageIO.write(img, "png", fos);
            } catch (IOException e) {
                logger.log(e);
            } finally {
                try {
                    fos.close();
                } catch (final Throwable e) {
                }
            }
        }
    }

    private BufferedImage toBufferedImage(Image i) {
        if (i instanceof BufferedImage) { return (BufferedImage) i; }
        Image img;
        img = new ImageIcon(i).getImage();
        BufferedImage b;
        b = new BufferedImage(img.getWidth(null), img.getHeight(null), BufferedImage.TYPE_INT_ARGB);
        Graphics g = b.createGraphics();
        g.drawImage(img, 0, 0, null);
        g.dispose();
        return b;
    }

    private String callExtern() {
        /* wait till locked */
        logger.info("acquire lock: " + methodDirName);
        while (true) {
            synchronized (SYNCMAP) {
                if (SYNCMAP.add(methodDirName) == true) {
                    logger.info("locked: " + methodDirName);
                    break;
                }
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                logger.log(e);
                return null;
            }
        }
        try {
            FileOutputStream fos = null;
            try {
                File file = JDUtilities.getResourceFile(this.srcFile);
                file.getParentFile().mkdirs();
                String ext = Files.getExtension(file.getName());
                fos = new FileOutputStream(file);
                ImageIO.write(toBufferedImage(this.sourceImage), ext, fos);
            } catch (Exception e) {
                logger.log(e);
                return null;
            } finally {
                try {
                    fos.close();
                } catch (final Throwable e) {
                }
            }
            logger.info("start: " + command);
            Executer exec = new Executer(JDUtilities.getResourceFile(this.command).getAbsolutePath());
            exec.setRunin(JDUtilities.getResourceFile(this.command).getParent());
            exec.setWaitTimeout(300);
            exec.start();
            exec.waitTimeout();
            try {
                exec.interrupt();
            } catch (final Throwable e) {
            }
            // String ret = exec.getOutputStream() + " \r\n " +
            // exec.getErrorStream();

            String res = JDIO.readFileToString(JDUtilities.getResourceFile(this.dstFile));
            logger.info("returned with: " + res);
            if (res == null) return null;
            return res.trim();
        } finally {
            /* remove lock */
            synchronized (SYNCMAP) {
                logger.info("release lock: " + methodDirName);
                SYNCMAP.remove(methodDirName);
            }
        }
    }

    /**
     * Gibt den erkannten CaptchaText zurück
     * 
     * @param captchafile
     *            Pfad zum Bild
     * @return CaptchaCode
     * @throws InterruptedException
     */
    public String checkCaptcha(File captchafile) throws InterruptedException {
        logger.finer("check " + captchafile);
        Image captchaImage = Utilities.loadImage(captchafile);
        Captcha captcha = createCaptcha(captchaImage);
        if (captcha != null) captcha.setCaptchaFile(captchafile);
        // captcha.printCaptcha();
        return checkCaptcha(captchafile, captcha);
    }

    /**
     * Factory Methode zur Captcha erstellung
     * 
     * @param captchaImage
     *            Image instanz
     * @return captcha
     * @throws InterruptedException
     */
    public Captcha createCaptcha(Image captchaImage) throws InterruptedException {
        this.sourceImage = captchaImage;
        if (extern) return null;
        if (captchaImage.getWidth(null) <= 0 || captchaImage.getHeight(null) <= 0) {

            logger.severe("Image Dimensionen zu klein. Image hat keinen Inahlt. Pfad/Url prüfen!");

            return null;
        }
        Captcha ret = Captcha.getCaptcha(captchaImage, this);
        if (ret == null) { return null; }
        ret.setOwner(this);
        return ret;
    }

    /**
     * Aus gründen der geschwindigkeit wird die MTH XMl in einen vector umgewandelt
     */
    private void createLetterDBFormMTH(Document mth) {
        letterDB = new ArrayList<Letter>();
        long start1 = System.currentTimeMillis();
        try {

            if (mth == null || mth.getFirstChild() == null) { return; }
            NodeList nl = mth.getFirstChild().getChildNodes();
            Letter tmp;
            for (int i = 0; i < nl.getLength(); i++) {
                // Get child node
                Node childNode = nl.item(i);
                if (childNode.getNodeName().equals("letter")) {
                    NamedNodeMap att = childNode.getAttributes();

                    tmp = new Letter();
                    tmp.setOwner(this);
                    String id = JDUtilities.getAttribute(childNode, "id");
                    if (!tmp.setTextGrid(childNode.getTextContent())) {

                        logger.severe("Error in Letters DB line: " + i + ":" + childNode.getTextContent() + " id:" + id);
                        continue;
                    }

                    if (id != null) {
                        tmp.setId(Integer.parseInt(id));
                    }
                    tmp.setSourcehash(att.getNamedItem("captchaHash").getNodeValue());
                    tmp.setDecodedValue(att.getNamedItem("value").getNodeValue());
                    tmp.setBadDetections(Integer.parseInt(JDUtilities.getAttribute(childNode, "bad")));
                    tmp.setGoodDetections(Integer.parseInt(JDUtilities.getAttribute(childNode, "good")));
                    letterDB.add(tmp);
                } else if (childNode.getNodeName().equals("map")) {

                    logger.fine("Parse LetterMap");

                    long start2 = System.currentTimeMillis();
                    String[] map = childNode.getTextContent().split("\\|");
                    letterMap = new int[map.length][map.length];
                    for (int x = 0; x < map.length; x++) {
                        String[] row = map[x].split("\\,");
                        for (int y = 0; y < map.length; y++) {
                            letterMap[x][y] = Integer.parseInt(row[y]);
                        }

                    }

                    logger.fine("LetterMap Parsing time: " + (System.currentTimeMillis() - start2));

                }
            }
        } catch (Exception e) {
            logger.log(e);

            logger.severe("Fehler bein lesen der MTH Datei!!. Methode kann nicht funktionieren!");

        }

        logger.fine("Mth Parsing time: " + (System.currentTimeMillis() - start1));

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
        Document xml = JDUtilities.parseXmlString("<jDownloader></jDownloader>", false);
        if (letterMap != null) {
            Element element = xml.createElement("map");
            xml.getFirstChild().appendChild(element);
            element.appendChild(xml.createTextNode(getLetterMapString()));
        }

        int i = 0;
        for (Letter letter : letterDB) {
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

    private void destroyScrollPaneWindows() {
        while (spw.size() > 0) {
            spw.remove(0).destroy();
        }
    }

    /**
     * Zeigt die Momentane Library an. Buchstaben können gelöscht werden
     */
    public void displayLibrary() {
        if (letterDB == null || letterDB.size() == 0) { return; }
        // final BasicWindow w = BasicWindow.getWindow("Library: " +
        // letterDB.size() + " Datensätze", 400, 300);
        final JFrame w = new JFrame();
        // w.setLayout(new GridBagLayout());
        sortLetterDB();
        JPanel p = new JPanel(new GridLayout(letterDB.size() + 1, 3));
        w.add(new JScrollPane(p));

        final Letter[] list = new Letter[letterDB.size()];

        int y = 0;
        int i = 0;
        ListIterator<Letter> iter = letterDB.listIterator(letterDB.size());
        final java.util.List<Integer> rem = new ArrayList<Integer>();
        while (iter.hasPrevious()) {
            final Letter tmp = iter.previous();
            list[i] = tmp;

            JLabel lbl = null;
            if ((tmp.getGoodDetections() == 0 && tmp.getBadDetections() > 3) || ((double) tmp.getBadDetections() / (double) tmp.getGoodDetections() >= 3)) {
                lbl = new JLabel("<html><p><font color=\"#ff0000\" " + "size=\"3\">" + tmp.getId() + ": " + tmp.getDecodedValue() + "(" + tmp.getGoodDetections() + "/" + tmp.getBadDetections() + ") Size: " + tmp.toPixelObject(0.85).getSize() + "</font> </p>" + "</html>");
            } else {
                lbl = new JLabel(tmp.getId() + ": " + tmp.getDecodedValue() + "(" + tmp.getGoodDetections() + "/" + tmp.getBadDetections() + ") Size: " + tmp.toPixelObject(0.85).getSize());
            }

            ImageComponent img = new ImageComponent(tmp.getImage());

            final JCheckBox bt = new JCheckBox("DELETE");
            final int ii = i;
            bt.addActionListener(new ActionListener() {
                public Integer id = ii;

                public void actionPerformed(ActionEvent arg) {
                    JCheckBox src = ((JCheckBox) arg.getSource());
                    if (src.getText().equals("DELETE")) {
                        rem.add(id);
                    } else {
                        rem.remove(id);
                    }
                }

            });
            p.add(lbl);
            p.add(img);
            p.add(bt);
            i++;
            y++;
            // if (y > 20) {
            // y = 0;
            // x += 6;
            // }
        }
        JButton b = new JButton("Invoke");
        p.add(b);
        b.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                // System.out.println(rem + "");
                java.util.List<Letter> list = new ArrayList<Letter>();
                int s = letterDB.size();
                for (Integer i : rem) {
                    try {
                        Letter let = letterDB.get(s - 1 - i);
                        list.add(let);

                    } catch (Exception ew) {
                        logger.log(ew);
                    }
                }
                for (Letter letter : list) {
                    removeLetterFromLibrary(letter);
                }
                saveMTHFile();
                displayLibrary();
            }
        });
        w.pack();
        WindowManager.getInstance().setVisible(w, true,FrameState.OS_DEFAULT);
    }

    public String getCodeFromFileName(String name) {
        return new Regex(name, "captcha_(.*?)_code(.*?)\\.(.*?)").getMatch(1);
    }

    /**
     * Liest den captchaornder aus
     * 
     * @param path
     * @return File Array
     */
    public File[] getImages(String path) {
        File dir = new File(path);

        if (dir == null || !dir.exists()) {

            logger.severe("Image dir nicht gefunden " + path);

        }
        logger.info(dir + "");
        File[] entries = dir.listFiles(new FileFilter() {
            public boolean accept(File pathname) {

                logger.info(pathname.getName());

                if (pathname.getName().endsWith(".bmp") || pathname.getName().endsWith(".jpg") || pathname.getName().endsWith(".png") || pathname.getName().endsWith(".gif")) {

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

    /**
     * Die Methode parsed die jacinfo.xml
     */
    private void getJACInfo() {
        File f = getResourceFile("jacinfo.xml");
        if (!f.exists()) {

            logger.severe("jacinfo.xml is missing2");

            return;
        }
        Document doc = JDUtilities.parseXmlString(JDIO.readFileToString(f), false);
        if (doc == null) {

            logger.severe("jacinfo.xml is missing2");

            return;
        }

        NodeList nl = doc.getFirstChild().getChildNodes();
        for (int i = 0; i < nl.getLength(); i++) {
            // Get child node
            Node childNode = nl.item(i);

            if (childNode.getNodeName().equals("method")) {
                try {
                    this.extern = JDUtilities.getAttribute(childNode, "type").equalsIgnoreCase("extern");
                } catch (Exception e) {
                }
            } else if (childNode.getNodeName().equals("command")) {

                this.srcFile = JDUtilities.getAttribute(childNode, "src");
                this.dstFile = JDUtilities.getAttribute(childNode, "dst");
                this.command = JDUtilities.getAttribute(childNode, "cmd");

            } else if (childNode.getNodeName().equals("format")) {
                try {
                    setLetterNum(Integer.parseInt(JDUtilities.getAttribute(childNode, "letterNum")));
                } catch (Exception e) {
                }

                setImageType(JDUtilities.getAttribute(childNode, "type"));
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
     * Vergleicht a und b und gibt eine Vergleichszahl zurück. a und b werden gegeneinander verschoben und b wird über die Parameter
     * gedreht. Praktisch heißt das, dass derjenige Treffer als gut eingestuft wird, bei dem der Datenbank Datensatz möglichst optimal
     * überdeckt wird.
     * 
     * @param a
     *            Original Letter
     * @param B
     *            Vergleichsletter
     * @return int 0(super)-0xffffff (ganz übel)
     * @throws InterruptedException
     */
    public LetterComperator getLetter(Letter letter) throws InterruptedException {
        if (jas.getDouble("quickScanValityLimit") <= 0) {
            logger.info("quickscan disabled");
            return getLetterExtended(letter);

        }

        logger.info("Work on Letter:" + letter);
        // long startTime = Utilities.getTimer();
        LetterComperator res = null;
        double lastPercent = 100.0;
        int bvX, bvY;
        try {

            if (letterDB == null) {

                logger.severe("letterDB nicht vorhanden");

                return null;
            }

            LetterComperator lc;
            ScrollPaneWindow w = null;
            if (isShowDebugGui()) {
                w = new ScrollPaneWindow();

                w.setTitle(" Letter " + letter.getId());
            }
            bvX = jas.getInteger("borderVarianceX");
            bvY = jas.getInteger("borderVarianceY");
            int line = 0;
            lc = new LetterComperator(letter, null);
            lc.setScanVariance(0, 0);
            lc.setOwner(this);
            res = lc;
            int tt = 0;
            logger.info("Do quickscan");
            Method preValueFilterMethod = null;
            Class<?>[] preValueFilterParameterTypes = null;
            Object[] preValueFilterArguments = new Object[] { null, this };
            if (jas.getString("preValueFilter").length() > 0) {
                String[] ref = jas.getString("preValueFilter").split("\\.");
                if (ref.length != 2) {

                    logger.severe("preValueFilter should have the format Class.Method");

                    return null;
                }
                String cl = ref[0];
                String methodname = ref[1];
                Class<?> newClass;
                try {
                    newClass = Class.forName("jd.captcha.specials." + cl);
                    preValueFilterParameterTypes = new Class[] { LetterComperator.class, this.getClass() };
                    preValueFilterMethod = newClass.getMethod(methodname, preValueFilterParameterTypes);

                } catch (Exception e) {
                    logger.log(e);
                }
            }
            Method postValueFilterMethod = null;
            Class<?>[] postValueFilterParameterTypes = null;
            Object[] postValueFilterArguments = new Object[] { null, this };
            if (jas.getString("postValueFilter").length() > 0) {
                String[] ref = jas.getString("postValueFilter").split("\\.");
                if (ref.length != 2) {

                    logger.severe("postValueFilter should have the format Class.Method");

                    return null;
                }
                String cl = ref[0];
                String methodname = ref[1];
                Class<?> newClass;
                try {
                    newClass = Class.forName("jd.captcha.specials." + cl);
                    postValueFilterParameterTypes = new Class[] { LetterComperator.class, this.getClass() };
                    postValueFilterMethod = newClass.getMethod(methodname, postValueFilterParameterTypes);

                } catch (Exception e) {
                    logger.log(e);
                }
            }
            for (Letter tmp : letterDB) {
                if (Thread.currentThread().isInterrupted()) throw new InterruptedException();

                if (Math.abs(tmp.getHeight() - letter.getHeight()) > bvY || Math.abs(tmp.getWidth() - letter.getWidth()) > bvX) {
                    continue;
                }

                lc = new LetterComperator(letter, tmp);
                // commented out only experimental
                // lc.setScanVariance(0, 0);
                lc.setOwner(this);

                if (preValueFilterMethod != null) {
                    preValueFilterArguments[0] = tmp;
                    preValueFilterArguments[1] = lc;
                    if (!((Boolean) preValueFilterMethod.invoke(null, preValueFilterArguments))) {
                        continue;
                    }

                }
                lc.run();
                tt++;
                if (isShowDebugGui()) {
                    w.setText(0, line, "0° Quick " + tt);
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

                            logger.finer(" Perfect Match: " + res.getB().getDecodedValue() + res.getValityPercent() + " good:" + tmp.getGoodDetections() + " bad: " + tmp.getBadDetections() + " - " + res);

                            res.setDetectionType(LetterComperator.QUICKSCANPERFECTMATCH);
                            res.setReliability(lastPercent - res.getValityPercent());
                            return res;
                        }
                        // if(Utilities.isLoggerActive())logger.finer("dim "
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
            logger.log(e);
        }
        if (res != null && res.getB() != null) {

            logger.finer(" Normal Match: " + res.getB().getDecodedValue() + " " + res.getValityPercent() + " good:" + res.getB().getGoodDetections() + " bad: " + res.getB().getBadDetections());

            // if (Utilities.isLoggerActive()) logger.fine("Letter erkannt
            // in: " + (Utilities.getTimer() - startTime) + " ms");
            res.setReliability(lastPercent - res.getValityPercent());
            if (res.getReliability() >= jas.getDouble("quickScanReliabilityLimit") && res.getValityPercent() < jas.getDouble("quickScanValityLimit")) {
                res.setDetectionType(LetterComperator.QUICKSCANMATCH);
                logger.info("Qickscan found " + res.getValityPercent() + "<" + jas.getDouble("quickScanValityLimit"));
                return res;
            } else {
                logger.warning("Letter nicht ausreichend erkannt. Try Extended " + res.getReliability() + " - " + jas.getDouble("quickScanReliabilityLimit") + " /" + res.getValityPercent() + "-" + jas.getDouble("quickScanValityLimit"));
                return getLetterExtended(letter);
            }
        } else {
            logger.warning("Letter nicht erkannt. Try Extended");
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
    private LetterComperator getLetterExtended(Letter letter) throws InterruptedException {
        // long startTime = Utilities.getTimer();
        LetterComperator res = null;
        logger.info("Extended SCAN");
        double lastPercent = 100.0;
        JTextArea tf = null;
        try {
            if (letterDB == null) {
                logger.severe("letterDB nicht vorhanden");
                return null;
            }
            Letter tmp;
            int leftAngle = jas.getInteger("scanAngleLeft");
            int rightAngle = jas.getInteger("scanAngleRight");
            if (leftAngle > rightAngle) {
                int temp = leftAngle;
                leftAngle = rightAngle;
                rightAngle = temp;

                logger.warning("param.scanAngleLeft>paramscanAngleRight");

            }
            int steps = Math.max(1, jas.getInteger("scanAngleSteps"));
            boolean turnDB = jas.getBoolean("turnDB");
            int angle;
            Letter orgLetter = letter;
            LetterComperator lc;

            ScrollPaneWindow w = null;
            if (isShowDebugGui()) {
                w = new ScrollPaneWindow();

                w.setTitle(" Letter " + letter.getId());
            }
            int line = 0;
            lc = new LetterComperator(letter, null);
            lc.setOwner(this);
            res = lc;

            Method preValueFilterMethod = null;
            Class<?>[] preValueFilterParameterTypes = null;
            Object[] preValueFilterArguments = new Object[] { null, this };
            if (jas.getString("preValueFilter").length() > 0) {
                String[] ref = jas.getString("preValueFilter").split("\\.");
                if (ref.length != 2) {

                    logger.severe("preValueFilter should have the format Class.Method");

                    return null;
                }
                String cl = ref[0];
                String methodname = ref[1];
                Class<?> newClass;
                try {
                    newClass = Class.forName("jd.captcha.specials." + cl);
                    preValueFilterParameterTypes = new Class[] { LetterComperator.class, this.getClass() };
                    preValueFilterMethod = newClass.getMethod(methodname, preValueFilterParameterTypes);

                } catch (Exception e) {
                    logger.log(e);
                }
            }
            Method postValueFilterMethod = null;
            Class<?>[] postValueFilterParameterTypes = null;
            Object[] postValueFilterArguments = new Object[] { null, this };
            if (jas.getString("postValueFilter").length() > 0) {
                String[] ref = jas.getString("postValueFilter").split("\\.");
                if (ref.length != 2) {

                    logger.severe("postValueFilter should have the format Class.Method");

                    return null;
                }
                String cl = ref[0];
                String methodname = ref[1];
                Class<?> newClass;
                try {
                    newClass = Class.forName("jd.captcha.specials." + cl);
                    postValueFilterParameterTypes = new Class[] { LetterComperator.class, this.getClass() };
                    postValueFilterMethod = newClass.getMethod(methodname, postValueFilterParameterTypes);

                } catch (Exception e) {
                    logger.log(e);
                }
            }
            for (angle = Utilities.getJumperStart(leftAngle, rightAngle); Utilities.checkJumper(angle, leftAngle, rightAngle); angle = Utilities.nextJump(angle, leftAngle, rightAngle, steps)) {

                if (turnDB) {
                    letter = orgLetter;
                } else {
                    letter = orgLetter.turn(angle);
                }
                // if(Utilities.isLoggerActive())logger.finer(" Angle " +
                // angle + " : " + letter.getDim());

                int tt = 0;
                for (Letter ltr : letterDB) {
                    if (Thread.currentThread().isInterrupted()) {
                        //
                        throw new InterruptedException();
                    }

                    if (turnDB) {
                        tmp = ltr.turn(angle);

                    } else {
                        tmp = ltr;
                    }

                    if (Math.abs(tmp.getHeight() - letter.getHeight()) > jas.getInteger("borderVarianceY") || Math.abs(tmp.getWidth() - letter.getWidth()) > jas.getInteger("borderVarianceX")) {
                        continue;
                    }

                    lc = new LetterComperator(letter, tmp);
                    lc.setOwner(this);

                    if (preValueFilterMethod != null) {
                        preValueFilterArguments[0] = lc;
                        preValueFilterArguments[1] = this;
                        if (!((Boolean) preValueFilterMethod.invoke(null, preValueFilterArguments))) {
                            continue;
                        }

                    }
                    lc.run();
                    // if(Utilities.isLoggerActive())logger.info("Duration:
                    // "+(Utilities.getTimer()-timer) +"
                    // Loops: "+lc.loopCounter);
                    tt++;

                    if (isShowDebugGui()) {
                        w.setText(0, line, angle + "° " + tt);
                        w.setImage(1, line, lc.getA().getImage(2));
                        w.setText(2, line, lc.getA().getDim());
                        w.setImage(3, line, lc.getB().getImage(2));
                        w.setText(4, line, lc.getB().getDim());
                        w.setImage(5, line, lc.getIntersectionLetter().getImage(2));
                        w.setText(6, line, lc.getIntersectionLetter().getDim());

                        w.setComponent(7, line, tf = new JTextArea());
                        tf.setText(lc.toString());
                        if (lc.getPreValityPercent() > jas.getInteger("preScanFilter") && jas.getInteger("preScanFilter") > 0) {
                            tf.setBackground(Color.LIGHT_GRAY);
                        }
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

                                logger.finer(" Perfect Match: " + res.getB().getDecodedValue() + " " + res.getValityPercent() + " good:" + tmp.getGoodDetections() + " bad: " + tmp.getBadDetections() + " - " + res);

                                if (isShowDebugGui()) {
                                    tf.setBackground(Color.GREEN);
                                }
                                return res;
                            }
                            if (isShowDebugGui()) {
                                tf.setBackground(Color.BLUE);
                            }

                            logger.finer("Angle " + angle + "dim " + lc.getA().getDim() + "|" + lc.getB().getDim() + " New Best value: " + lc.getDecodedValue() + " " + lc.getValityPercent() + " good:" + tmp.getGoodDetections() + " bad: " + tmp.getBadDetections() + " - " + lc);

                        } else if (res != null) {
                            // if (Utilities.isLoggerActive()&&
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
                // if(Utilities.isLoggerActive())logger.info("Full Angle scan
                // in
                // "+(Utilities.getTimer()-startTime2));
            }
            // w.refreshUI();
        } catch (InterruptedException e) {
            throw e;
        } catch (Exception e) {
            logger.log(e);
        }

        if (res != null && res.getB() != null) {

            logger.finer(" Normal Match: " + res.getB().getDecodedValue() + " " + res.getValityPercent() + " good:" + res.getB().getGoodDetections() + " bad: " + res.getB().getBadDetections());

            res.setReliability(lastPercent - res.getValityPercent());
        } else {
            if (getJas().getInteger("preScanEmergencyFilter") > getJas().getInteger("preScanFilter")) {
                logger.warning("nicht erkannt. Verwende erweiterte Emergencydatenbank");
                int psf = getJas().getInteger("preScanFilter");
                getJas().set("preScanFilter", getJas().getInteger("preScanEmergencyFilter"));
                LetterComperator ret = getLetterExtended(letter);
                getJas().set("preScanFilter", psf);
                return ret;

            }

            logger.severe("Letter entgültig nicht erkannt");

            if (isShowDebugGui() && tf != null) {
                tf.setBackground(Color.RED);
            }

        }

        return res;

    }

    /**
     * @return gibt die Lettermap als String zurück
     */
    private String getLetterMapString() {
        StringBuilder ret = new StringBuilder();
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

            logger.fine("Db String: " + x * 100 / letterDB.size() + "%");

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
     * @return the method
     */
    public String getMethodDirName() {
        return methodDirName;
    }

    /**
     * Gibt ein FileObject zu einem resourcestring zurück
     * 
     * @param arg
     * @return File zu arg
     */
    public File getResourceFile(String arg) {
        return JDUtilities.getResourceFile(JDUtilities.getJACMethodsDirectory() + methodDirName + "/" + arg);
    }

    public Captcha getWorkingCaptcha() {
        return workingCaptcha;
    }

    /**
     * Importiert PNG einzelbilder aus einem ordner und erstellt daraus eine neue db
     * 
     * @throws InterruptedException
     */
    public void importDB(File path) throws InterruptedException {
        String pattern = JOptionPane.showInputDialog("PATTERN", "\\d+_(.*?)\\.");
        if (JOptionPane.showConfirmDialog(null, "Delete old db?") == JOptionPane.OK_OPTION) letterDB = new ArrayList<Letter>();
        getResourceFile("letters.mth").delete();
        System.out.println("LETTERS BEFORE: " + letterDB.size());
        Image image;
        Letter letter;
        File[] images = getImages(path.getAbsolutePath());
        for (File element : images) {
            image = Utilities.loadImage(element);
            try {
                image = ImageProvider.read(element);
            } catch (IOException e) {
                logger.log(e);
            }
            System.out.println(element.getAbsolutePath());
            int width = image.getWidth(null);
            int height = image.getHeight(null);
            if (width <= 0 || height <= 0) {

                logger.severe("ERROR: Image nicht korrekt.");

            }

            Captcha cap = createCaptcha(image);

            letter = new Letter();
            letter.setOwner(this);
            letter.setGrid(cap.grid);
            letter.setSourcehash(JDHash.getMD5(element));

            String let = new Regex(element.getName(), pattern).getMatch(0);

            letter.setDecodedValue(let);
            letter.clean();
            letter.removeSmallObjects(0.3, 0.5, 10);
            letter.setDecodedValue(let);

            letterDB.add(letter);

        }
        System.out.println("LETTERS AFTER: " + letterDB.size());
        sortLetterDB();
        saveMTHFile();
    }

    /**
     * Prüft ob der übergeben hash in der MTH file ist
     * 
     * @param captchaHash
     * @return true/false
     */
    private boolean isCaptchaInMTH(String captchaHash) {
        if (letterDB == null) return false;
        for (Letter letter : letterDB) {
            if (letter.getSourcehash().equals(captchaHash)) return true;
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
     * 
     * @param f
     */
    public void loadMTHFile(File f) {
        String str = null;
        if (f.exists()) {
            str = JDIO.readFileToString(f);
        } else {
            str = "<jDownloader></jDownloader>";
        }
        Document mth = JDUtilities.parseXmlString(str, false);
        logger.info("Get file: " + f);
        if (mth == null) {
            logger.severe("MTH FILE NOT AVAILABLE.");
        }
        createLetterDBFormMTH(mth);
        // sortLetterDB();

    }

    /**
     * MTH File wird geladen und verarbeitet
     */
    public void loadMTHFile() {
        File f = getResourceFile("letters.mth");
        loadMTHFile(f);
        // sortLetterDB();
    }

    /**
     * Entfernt Buchstaben mit einem schlechetb Bad/Good verhältniss
     */
    public void removeBadLetters() {
        Letter tmp;

        logger.info("aktuelle DB Size: " + letterDB.size());

        ListIterator<Letter> iter = letterDB.listIterator(letterDB.size());
        while (iter.hasPrevious()) {
            tmp = iter.previous();
            if (tmp.getBadDetections() > tmp.getGoodDetections() + 2) {

                logger.info("bad Letter entfernt: " + tmp.getDecodedValue() + " (" + tmp.getBadDetections() + "/" + tmp.getGoodDetections() + ")");

                iter.remove();
            }

        }

        logger.info("neue DB Size: " + letterDB.size());

        sortLetterDB();
        saveMTHFile();

    }

    protected void removeLetterFromLibrary(Letter letter) {

        logger.info("Remove" + letter + " : " + letterDB.remove(letter));

    }

    /**
     * Speichert die MTH File
     */
    public synchronized void saveMTHFile() {
        String xmlString = JDUtilities.createXmlString(createXMLFromLetterDB());
        if (!JDIO.writeLocalFile(getResourceFile("letters.mth"), xmlString)) {
            logger.severe("MTHO file Konnte nicht gespeichert werden");
        }
    }

    /**
     * @param imageType
     *            the imageType to set
     */
    public void setImageType(String imageType) {

        logger.finer("SET PARAMETER: [imageType] = " + imageType);

        this.imageType = imageType;
    }

    /**
     * @param letterNum
     *            the letterNum to set
     */
    public void setLetterNum(int letterNum) {

        logger.finer("SET PARAMETER: [letterNum] = " + letterNum);

        this.letterNum = letterNum;
    }

    /**
     * @param showDebugGui
     *            the showDebugGui to set
     */
    public void setShowDebugGui(boolean showDebugGui) {
        this.showDebugGui = showDebugGui;
    }

    public void setWorkingCaptcha(Captcha workingCaptcha) {
        this.workingCaptcha = workingCaptcha;
    }

    /**
     * Debug Methode. Zeigt den Captcha in verschiedenen bearbeitungsstadien an
     * 
     * @param captchafile
     * @throws InterruptedException
     */
    public void showPreparedCaptcha(final File captchafile) throws InterruptedException {

        if (!captchafile.exists()) {

            logger.severe(captchafile.getAbsolutePath() + " existiert nicht");

            return;
        }

        Image captchaImage = Utilities.loadImage(captchafile);
        BasicWindow.showImage(captchaImage);
        Captcha captcha = createCaptcha(captchaImage);

        int skWidth = captcha.getWidth();
        int skHeight = captcha.getHeight();
        if (skHeight > 200 || skWidth > 200) {
            if (skHeight > skWidth) {
                skWidth = 200 * skWidth / skHeight;
                skHeight = 200;
            } else {
                skHeight = 200 * skHeight / skWidth;
                skWidth = 200;
            }
        }
        logger.info("CAPTCHA :_" + checkCaptcha(captchafile, captcha));
        if (bw3 != null) {
            bw3.dispose();
        }
        bw3 = BasicWindow.showImage(captchaImage.getScaledInstance(skWidth, skHeight, 1), "Captchas");
        bw3.add(new JLabel("ORIGINAL"), Utilities.getGBC(2, 0, 2, 2));
        bw3.setLocationByScreenPercent(50, 70);

        bw3.add(new ImageComponent(captcha.getImage(1).getScaledInstance(skWidth, skHeight, 1)), Utilities.getGBC(0, 2, 2, 2));
        bw3.add(new JLabel("Farbraum Anpassung"), Utilities.getGBC(2, 2, 2, 2));
        jas.executePrepareCommands(captchafile, captcha);

        bw3.add(new ImageComponent(captcha.getImage(1).getScaledInstance(skWidth, skHeight, 1)), Utilities.getGBC(0, 4, 2, 2));
        bw3.add(new JLabel("Prepare Code ausgeführt"), Utilities.getGBC(2, 4, 2, 2));

        // Hole die letters aus dem neuen captcha

        // Utilities.wait(40000);
        // prüfe auf Erfolgreiche Lettererkennung

        // Decoden. checkCaptcha verwendet dabei die gecachte Erkennung der
        // letters

        Letter[] letters = captcha.getLetters(letterNum);
        if (letters == null) {

            logger.severe("2. Lettererkennung ist fehlgeschlagen!");

            return;
        }

        bw3.add(new ImageComponent(captcha.getImageWithGaps(1).getScaledInstance(skWidth, skHeight, 1)), Utilities.getGBC(0, 6, 2, 2));
        bw3.add(new JLabel("Buchstaben freistellen"), Utilities.getGBC(2, 6, 2, 2));
        bw3.refreshUI();
        if (bw2 != null) {
            bw2.destroy();
        }
        bw2 = new BasicWindow();
        bw2.setTitle("Freigestellte Buchstaben");
        bw2.setLayout(new GridBagLayout());
        bw2.setSize(300, 300);

        logger.info("display freistellen");

        bw2.setAlwaysOnTop(true);
        bw2.setLocationByScreenPercent(50, 5);
        bw2.add(new JLabel("Aus Captcha:"), Utilities.getGBC(0, 0, 2, 2));

        for (int i = 0; i < letters.length; i++) {
            bw2.add(new ImageComponent(letters[i].getImage((int) Math.ceil(jas.getDouble("simplifyFaktor")))), Utilities.getGBC(i * 2 + 2, 0, 2, 2));
        }
        WindowManager.getInstance().setVisible(bw2, true,FrameState.OS_DEFAULT);
        bw2.pack();
        bw2.setSize(300, bw2.getSize().height);

        LetterComperator[] lcs = captcha.getLetterComperators();
        for (int i = 0; i < lcs.length; i++) {
            if (lcs[i] == null) continue;

            bw2.add(new JLabel("Aus Datenbank:"), Utilities.getGBC(0, 6, 2, 2));
            Letter dif = lcs[i].getDifference();
            dif.removeSmallObjects(0.8, 0.8, 5);
            dif.clean();
            if (lcs[i].getB() != null) {
                bw2.add(new ImageComponent(lcs[i].getB().getImage((int) Math.ceil(jas.getDouble("simplifyFaktor")))), Utilities.getGBC(i * 2 + 2, 6, 2, 1));
                bw2.add(new ImageComponent(dif.getImage((int) Math.ceil(jas.getDouble("simplifyFaktor")))), Utilities.getGBC(i * 2 + 2, 7, 2, 1));
            } else {
                bw2.add(new JLabel("B unknown"), Utilities.getGBC(i * 2 + 2, 6, 2, 2));
            }

            bw2.add(new JLabel("Wert:"), Utilities.getGBC(0, 8, 2, 2));
            bw2.add(new JLabel("Proz.:"), Utilities.getGBC(0, 10, 2, 2));
            bw2.add(new JLabel(lcs[i].getValityPercent() + "%"), Utilities.getGBC(i * 2 + 2, 10, 2, 2));

        }
        JButton bt = new JButton("Train");

        bw2.add(bt, Utilities.getGBC(0, 12, 2, 2));
        bw2.pack();
        bw2.repack();

        bt.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                try {
                    JAntiCaptcha.this.trainCaptcha(captchafile, 4);
                } catch (InterruptedException e1) {
                    e1.printStackTrace();
                }
            }

        });
    }

    /**
     * Sortiert die letterDB Nach den bad Detections. Der Sortieralgo gehört dringend überarbeitet!!! Diese Sortieren hilft die GUten Letter
     * zuerst zu prüfen.
     * 
     * @TODO Sortoer ALGO ändern. zu langsam!!
     */
    public void sortLetterDB() {
        Collections.sort(letterDB, new Comparator<Letter>() {
            public int compare(Letter a, Letter b) {
                try {
                    return a.getDecodedValue().compareToIgnoreCase(b.getDecodedValue()) * -1;
                } catch (Exception e) {
                    e.printStackTrace();
                    return 0;
                }
            }
        });
    }

    /**
     * Diese methode wird aufgerufen um alle captchas im Ordner methods/Methodname/captchas zu trainieren
     * 
     * @param path
     * @throws InterruptedException
     */
    public void trainAllCaptchas(String path) throws InterruptedException {
        int successFull = 0;
        int total = 0;
        logger.info("TRain " + path);
        File[] images = getImages(path);
        if (images == null) { return; }
        int newLetters;
        for (File element : images) {

            logger.fine(element.toString());

            int letternum = getLetterNum();
            newLetters = trainCaptcha(element, letternum);

            logger.fine("Erkannt: " + newLetters + "/" + getLetterNum());

            if (newLetters > 0) {
                successFull += newLetters;
                total += getLetterNum();

                logger.info("Erkennungsrate: " + 100 * successFull / total);

            } else if (newLetters == -2) {
                if (f != null) {
                    f.dispose();
                }
                break;
            }

        }

    }

    public int trainCaptcha(final File captchafile, int letterNum) throws InterruptedException {

        if (!captchafile.exists()) {

            logger.severe(captchafile.getAbsolutePath() + " existiert nicht");

            return -1;
        }
        if (isShowDebugGui()) {
            destroyScrollPaneWindows();
        }
        // Lade das Bild
        // Erstelle hashwert
        final String captchaHash = JDHash.getMD5(captchafile);

        // Prüfe ob dieser captcha schon aufgenommen wurde und überspringe ihn
        // falls ja
        if (isCaptchaInMTH(captchaHash)) {

            logger.fine("Captcha schon aufgenommen" + captchafile);

            return -1;
        }
        // captcha erstellen
        Image captchaImage = Utilities.loadImage(captchafile);

        final Captcha captcha = createCaptcha(captchaImage);
        BasicWindow.showImage(captcha.getImage(), "Captchas");
        int sk1Width = captcha.getWidth();
        int sk1Height = captcha.getHeight();
        if (sk1Height > 200 || sk1Width > 200) {
            if (sk1Height > sk1Width) {
                sk1Width = 200 * sk1Width / sk1Height;
                sk1Height = 200;
            } else {
                sk1Height = 200 * sk1Height / sk1Width;
                sk1Width = 200;
            }
        }
        final int skWidth = sk1Width;
        final int skHeight = sk1Height;
        String code = null;
        // Zeige das OriginalBild
        if (f != null) {
            f.dispose();
        }
        f = new JDialog(DummyFrame.getDialogParent());
        f.setLocation(500, 10);
        f.setLayout(new GridBagLayout());
        f.add(new JLabel("original captcha: " + captchafile.getName()), Utilities.getGBC(0, 0, 10, 1));

        f.add(new ImageComponent(captcha.getImage()), Utilities.getGBC(0, 1, 10, 1));

        f.setSize(1400, 800);
        f.pack();
        WindowManager.getInstance().setVisible(f, true,FrameState.OS_DEFAULT);

        // Führe das Prepare aus
        // jas.executePrepareCommands(captcha);
        // Hole die letters aus dem neuen captcha
        final String guess = checkCaptcha(captchafile, captcha);
        final Letter[] letters = captcha.getLetters(letterNum);
        if (letters == null) {
            File file = getResourceFile("detectionErrors5/" + System.currentTimeMillis() + "_" + captchafile.getName());
            file.getParentFile().mkdirs();
            captchafile.renameTo(file);

            logger.severe("Letter detection error");

            return -1;
        }

        class MyRunnable implements Runnable {
            private String code = null;
            private int    ret  = 0;

            public void run() {
                if (getCodeFromFileName(captchafile.getName()) == null) {
                    code = JOptionPane.showInputDialog("Bitte Captcha Code eingeben (Press enter to confirm " + guess, guess);
                    if (code != null && code.equals(guess)) {
                        code = "";
                    } else if (code == null) {
                        if (JOptionPane.showConfirmDialog(new JFrame(), "Ja (yes) = beenden (close) \t Nein (no) = nächstes Captcha (next captcha)") == JOptionPane.OK_OPTION) {
                            ret = -2;
                        }
                    }
                } else {
                    code = getCodeFromFileName(captchafile.getName());

                    logger.warning("captcha code für " + captchaHash + " verwendet: " + code);

                }
                synchronized (this) {
                    this.notify();
                }
            }
        }
        MyRunnable run = new MyRunnable();
        Thread inpThread = new Thread(run);
        inpThread.start();
        // Zeige das After-prepare Bild an
        f.add(new JLabel("Letter Detection"), Utilities.getGBC(0, 3, 10, 1));

        f.add(new ImageComponent(captcha.getImageWithGaps(1).getScaledInstance(skWidth, skHeight, 1)), Utilities.getGBC(0, 4, 10, 1));

        f.add(new JLabel("Seperated"), Utilities.getGBC(0, 5, 10, 1));
        for (int i = 0; i < letters.length; i++) {
            f.add(new ImageComponent(letters[i].getImage((int) Math.ceil(jas.getDouble("simplifyFaktor")))), Utilities.getGBC(i * 2 + 1, 6, 1, 1));
            JLabel jl = new JLabel("|");
            jl.setForeground(Color.RED);
            f.add(jl, Utilities.getGBC(i * 2 + 2, 6, 1, 1));
        }
        f.pack();

        // Decoden. checkCaptcha verwendet dabei die gecachte Erkennung der
        // letters

        final LetterComperator[] lcs = captcha.getLetterComperators();
        if (lcs == null) {
            File file = getResourceFile("detectionErrors5/" + System.currentTimeMillis() + "_" + captchafile.getName());
            file.getParentFile().mkdirs();
            captchafile.renameTo(file);

            logger.severe("Letter detection error");

            return -1;
        }
        if (lcs.length != letters.length) {
            logger.severe("ACHTUNG. lcs: " + lcs.length + " - letters: " + letters.length);
        }
        if (guess != null /* && guess.length() == getLetterNum() */) {

            f.add(new JLabel("Letter Detection"), Utilities.getGBC(0, 3, 10, 1));

            f.add(new ImageComponent(captcha.getImageWithGaps(1).getScaledInstance(skWidth, skHeight, 1)), Utilities.getGBC(0, 4, 10, 1));

            f.add(new JLabel("Seperated"), Utilities.getGBC(0, 5, 10, 1));
            for (int i = 0; i < letters.length; i++) {
                f.add(new ImageComponent(letters[i].getImage((int) Math.ceil(jas.getDouble("simplifyFaktor")))), Utilities.getGBC(i * 2 + 1, 6, 1, 1));
                JLabel jl = new JLabel("|");
                jl.setForeground(Color.RED);
                f.add(jl, Utilities.getGBC(i * 2 + 2, 6, 1, 1));
            }
            f.pack();

            for (int i = 0; i < lcs.length; i++) {
                if (lcs[i] != null && lcs[i].getB() != null) {
                    f.add(new ImageComponent(lcs[i].getB().getImage((int) Math.ceil(jas.getDouble("simplifyFaktor")))), Utilities.getGBC(i * 2 + 1, 8, 1, 1));
                } else {
                    f.add(new JLabel(""), Utilities.getGBC(i * 2 + 1, 8, 1, 1));
                }
                JLabel jl = new JLabel("|");
                jl.setForeground(Color.RED);
                f.add(jl, Utilities.getGBC(i * 2 + 2, 6, 1, 1));

                if (lcs[i] != null && lcs[i].getB() != null) {
                    f.add(new JLabel("" + lcs[i].getDecodedValue()), Utilities.getGBC(i * 2 + 1, 9, 1, 1));
                } else {
                    f.add(new JLabel(""), Utilities.getGBC(i * 2 + 1, 9, 1, 1));
                }
                if (lcs[i] != null && lcs[i].getB() != null) {
                    f.add(new JLabel("" + Math.round(10 * lcs[i].getValityPercent()) / 10.0), Utilities.getGBC(i * 2 + 1, 10, 1, 1));
                } else {
                    f.add(new JLabel(""), Utilities.getGBC(i * 2 + 1, 10, 1, 1));
                }
            }
            f.pack();
        } else {

            logger.warning("Erkennung fehlgeschlagen");

        }
        f.add(new JLabel("prepared captcha"), Utilities.getGBC(0, 11, 10, 1));

        f.add(new ImageComponent(captcha.getImage().getScaledInstance(skWidth, skHeight, 1)), Utilities.getGBC(0, 12, 10, 1));
        f.pack();

        logger.info("Decoded Captcha: " + guess + " Vality: " + captcha.getValityPercent());

        if (inpThread.isAlive()) {
            synchronized (run) {
                try {
                    run.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        if (run.ret == -2) return -2;
        code = run.code;
        if (code == null) {
            File file = getResourceFile("detectionErrors3/" + System.currentTimeMillis() + "_" + captchafile.getName());
            file.getParentFile().mkdirs();
            captchafile.renameTo(file);
            logger.severe("Captcha Input error");
            return -1;
        }
        if (code.length() == 0) {
            code = guess;
        }
        if (code.length() != letters.length) {
            File file = getResourceFile("detectionErrors4/" + System.currentTimeMillis() + "_" + captchafile.getName());
            file.getParentFile().mkdirs();
            captchafile.renameTo(file);
            logger.severe("Captcha Input error3");
            return -1;
        }
        if (code.indexOf("-") < 0) {
            String[] oldName = captchafile.getName().split("\\.");
            String ext = oldName[oldName.length - 1];
            String newName = captchafile.getParentFile().getAbsolutePath() + "/captcha_" + getMethodDirName() + "_code" + code + "." + ext;
            captchafile.renameTo(new File(newName));
        }
        int ret = 0;
        for (int j = 0; j < letters.length; j++) {
            final int i = j;

            if (!code.substring(i, i + 1).equals("-")) {
                if (guess != null && code.length() > i && guess.length() > i && code.substring(i, i + 1).equals(guess.substring(i, i + 1))) {
                    ret++;
                    if (lcs[i] != null) {
                        lcs[i].getB().markGood();
                    }
                    if (lcs[i].getValityPercent() > 50) {
                        letters[i].setOwner(this);
                        // letters[i].setTextGrid(letters[i].getPixelString());
                        letters[i].setSourcehash(captchaHash);
                        letters[i].setDecodedValue(code.substring(i, i + 1));

                        new Thread(new Runnable() {
                            public void run() {
                                final BasicWindow bws = BasicWindow.showImage(letters[i].getImage(2), "" + letters[i].getDecodedValue());
                                try {
                                    Thread.sleep(1000);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                                bws.dispose();
                            }
                        }).start();

                        letterDB.add(letters[i]);
                    }
                    if (!jas.getBoolean("TrainOnlyUnknown")) {
                        letters[i].setOwner(this);
                        // letters[i].setTextGrid(letters[i].getPixelString());
                        letters[i].setSourcehash(captchaHash);
                        letters[i].setDecodedValue(code.substring(i, i + 1));
                        letterDB.add(letters[i]);
                        f.add(new JLabel("OK+"), Utilities.getGBC(i + 1, 13, 1, 1));
                    } else {
                        f.add(new JLabel("OK-"), Utilities.getGBC(i + 1, 13, 1, 1));
                        f.pack();
                    }
                } else {
                    logger.info(letterDB + " - ");
                    if (lcs != null && lcs[i] != null && letterDB.size() > 30 && lcs[i] != null && lcs[i].getB() != null) {
                        lcs[i].getB().markBad();
                    }
                    letters[i].setOwner(this);
                    // letters[i].setTextGrid(letters[i].getPixelString());
                    letters[i].setSourcehash(captchaHash);
                    letters[i].setDecodedValue(code.substring(i, i + 1));

                    letterDB.add(letters[i]);
                    new Thread(new Runnable() {
                        public void run() {
                            final BasicWindow bws = BasicWindow.showImage(letters[i].getImage(2), "" + letters[i].getDecodedValue());
                            try {
                                Thread.sleep(1000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            bws.dispose();
                        }
                    }).start();
                    f.add(new JLabel("NO +"), Utilities.getGBC(i + 1, 13, 1, 1));
                    f.pack();
                }
            } else {
                f.add(new JLabel("-"), Utilities.getGBC(i + 1, 13, 1, 1));
                f.pack();
            }
            // mth.appendChild(element);
        }
        sortLetterDB();
        // das syncroniced kann das jetzt auch mit einem thread gemacht werden
        new Thread(new Runnable() {
            public void run() {
                saveMTHFile();
            }
        }).start();
        return ret;
    }

    public void cleanLibrary(double d) {
        java.util.List<Letter> newDB = new ArrayList<Letter>();
        main: for (Letter let : letterDB) {

            for (Letter n : newDB) {
                if (let.getDecodedValue().endsWith(n.getDecodedValue())) {
                    LetterComperator lc = new LetterComperator(let, n);

                    lc.setOwner(this);
                    lc.run();

                    n.getElementPixel();

                    if (lc.getValityPercent() <= d) {
                        BasicWindow.showImage(let.getImage(), " OK ");
                        // BasicWindow.showImage(n.getImage(), " FILTERED " +
                        // lc.getValityPercent());
                        if (n.getElementPixel() > let.getElementPixel()) {
                            newDB.remove(let);
                            break;
                        } else {
                            continue main;
                        }
                    }
                }
            }
            newDB.add(let);

        }
        letterDB = newDB;
        this.saveMTHFile();
    }

    public static String getCaptcha(String path, String host) {
        if (JACMethod.hasMethod(host)) {

            File file;
            if (path.contains("http://")) {
                try {
                    file = JDUtilities.getResourceFile("captchas/jac_captcha.img");
                    file.deleteOnExit();

                    Browser.download(file, new Browser().openGetConnection(path));
                } catch (IOException e) {
                    LogController.CL().log(e);
                    return "Could not download captcha image";
                }
            } else {
                file = new File(path);
                if (!file.exists()) return "File does not exist";
            }

            try {
                Image captchaImage = ImageProvider.read(file);
                JAntiCaptcha jac = new JAntiCaptcha(host);
                Captcha captcha = jac.createCaptcha(captchaImage);
                return jac.checkCaptcha(file, captcha);
            } catch (Exception e) {
                LogController.CL().log(e);
                return Arrays.toString(e.getStackTrace());
            }
        } else {
            return "jDownloader has no method for " + host;
        }
    }

}