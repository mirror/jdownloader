package jd.captcha.easy;

import java.awt.Image;
import java.io.File;
import java.util.Vector;

import jd.nutils.io.JDIO;

import jd.captcha.pixelgrid.Captcha;
import jd.captcha.utils.Utilities;
import jd.controlling.JDLogger;

/**
 * Verwaltet die hintergrundbilder und sorgt dafür das das richtige entfernt
 * wird
 * 
 * @author dwd
 * 
 */
public class BackGroundImageManager {
    /**
     * Liste mit Hintergrundbildern
     */
    private Vector<BackGroundImage> backgroundList = null;
    protected EasyFile methode;
    private Captcha captchaImage;

    /**
     * Verwaltet die hintergrundbilder und sorgt dafür das das richtige entfernt
     * wird das übergebene Captcha wird gereinigt
     * 
     * @param captcha
     */
    public BackGroundImageManager(Captcha captcha) {
        this.captchaImage = captcha;
        methode = new EasyFile(captchaImage.owner.getResourceFile("jacinfo.xml").getParentFile());
        load();
    }
    /**
     * gibt ein um den Zoomfaktor Scalliertes Image zurück
     * 
     * @param zoom
     * @return Image
     */
    public Image getScaledCaptchaImage(int zoom) {
        return captchaImage.getImage().getScaledInstance(captchaImage.getWidth() * zoom / 100, captchaImage.getHeight() * zoom / 100, Image.SCALE_DEFAULT);
    }

    public Vector<BackGroundImage> getBackgroundList() {
        return backgroundList;
    }

    public void setBackgroundList(Vector<BackGroundImage> backgroundList) {
        this.backgroundList = backgroundList;
    }

    public void add(BackGroundImage bgi) {
        backgroundList.add(bgi);
    }

    public Captcha getCaptchaImage() {
        return captchaImage;
    }

    public void setCaptchaImage(Captcha captchaImage) {
        this.captchaImage = captchaImage;
    }

    /**
     * gibt die Xmldatei zurück in der die Informationen der Huntergrundbilder
     * gespeichert sind
     * 
     * @return methodenpfad/bgimages.xml
     */
    private File getBgImagesXmlFile() {
        return new File(methode.getJacinfoXml().getParent(), "bgimages.xml");
    }

    /**
     * läd die bgimages.xml der methode in die backgroundList
     */
    @SuppressWarnings("unchecked")
    private void load() {
        if (backgroundList == null) {
            File file = getBgImagesXmlFile();
            if (file.exists())
                backgroundList = (Vector<BackGroundImage>) JDIO.loadObject(null, file, true);
            else
                backgroundList = new Vector<BackGroundImage>();
        }
    }

    /**
     * Speichert alle Hintergrundbilder in der bgimages.xml ab
     */
    public void save() {
        File file = getBgImagesXmlFile();
        file.getParentFile().mkdirs();
        JDIO.saveObject(null, backgroundList, file, null, null, true);
    }

    /**
     * Sucht das Hintergrundbild bei dem die größte Übereinstimmung vorhanden
     * ist und reinigt das Captcha damit
     */
    public void clearCaptchaAll() {
        Captcha best = null;
        int bestVal = -1;
        BackGroundImage bestBgi = null;
        for (BackGroundImage bgi : backgroundList) {
            int color = bgi.getColor();
            Image bImage = Utilities.loadImage(new File(methode.file, bgi.getBackgroundImage()));
            if (bImage.getWidth(null) != captchaImage.getWidth() || bImage.getHeight(null) != captchaImage.getHeight()) {
                if (Utilities.isLoggerActive()) {
                    JDLogger.getLogger().info("ERROR Maske und Bild passen nicht zusammmen");
                }
                continue;
            }
            Captcha captcha2 = captchaImage.owner.createCaptcha(bImage);
            int val = 0;
            for (int x = 0; x < captchaImage.getWidth(); x++) {
                for (int y = 0; y < captchaImage.getHeight(); y++) {
                    bgi.setColor(captcha2.getPixelValue(x, y));
                    if (bgi.getColorDifference(captchaImage.getPixelValue(x, y)) < bgi.getDistance()) val++;
                }
            }
            bgi.setColor(color);
            if (val > bestVal) {
                best = captcha2;
                bestVal = val;
                bestBgi = bgi;
            }
        }
        if (best != null) {
            bestBgi.clearCaptcha(captchaImage);
        }
    }

    public void remove(BackGroundImage dialogImage) {
        backgroundList.remove(dialogImage);
    }
}
