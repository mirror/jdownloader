package jd.captcha.easy;

import java.awt.Image;
import java.io.File;
import java.util.Iterator;
import java.util.Vector;

import jd.captcha.pixelgrid.Captcha;
import jd.captcha.pixelgrid.PixelGrid;
import jd.captcha.utils.Utilities;
import jd.controlling.JDLogger;
import jd.nutils.io.JDIO;

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
    protected EasyMethodFile methode;
    private Captcha captchaImage;
    public int zoom;
    protected int[][] backupGrid;
    private String fileName="bgimages.xml";
    private void autoSetZoomFaktor() {
        if (captchaImage.getWidth() > 200 || captchaImage.getHeight() > 100)
            zoom = 100;
        else if (captchaImage.getWidth() > 100 || captchaImage.getHeight() > 50)
            zoom = 200;
        else
            zoom = 400;
    }

    /**
     * Verwaltet die hintergrundbilder und sorgt dafür das das richtige entfernt
     * wird bei einem randomCaptcha
     * 
     * @param originalCaptcha
     */
    public BackGroundImageManager(EasyMethodFile methode) {
        this(methode.getRandomCaptcha());
    }

    /**
     * Verwaltet die hintergrundbilder und sorgt dafür das das richtige entfernt
     * wird bei einem randomCaptcha
     * 
     * @param originalCaptcha
     */
    public BackGroundImageManager(String hoster) {
        this(new EasyMethodFile(hoster));
    }

    /**
     * Verwaltet die hintergrundbilder und sorgt dafür das das richtige entfernt
     * wird das übergebene Captcha wird gereinigt
     * 
     * @param captcha
     */
    public BackGroundImageManager(Captcha captcha) {
        this.captchaImage = captcha;
        backupGrid = PixelGrid.getGridCopy(captcha.getGrid());
        autoSetZoomFaktor();
        methode = new EasyMethodFile(captchaImage.owner.getResourceFile("jacinfo.xml").getParentFile());
        load();
    }
    
    
    /**
     * gibt ein um den Zoomfaktor Scalliertes Image zurück
     * 
     * @param zoom
     * @return Image
     */
    public Image getScaledCaptchaImage() {
        return captchaImage.getImage().getScaledInstance(captchaImage.getWidth() * zoom / 100, captchaImage.getHeight() * zoom / 100, Image.SCALE_DEFAULT);
    }

    /**
     * Liste der Hindergrundbilder
     * 
     * @return
     */
    public Vector<BackGroundImage> getBackgroundList() {
        return backgroundList;
    }

    /**
     * Liste der Hindergrundbilder
     */
    public void setBackgroundList(Vector<BackGroundImage> backgroundList) {
        this.backgroundList = backgroundList;
    }

    /**
     * Hintergrundbild hinzufügen
     * 
     * @param bgi
     */
    public void add(BackGroundImage bgi) {
        backgroundList.add(bgi);
    }

    /**
     * aktuell verwaltetes Captcha
     * 
     * @return
     */
    public Captcha getCaptchaImage() {
        return captchaImage;
    }

    /**
     * aktuell verwaltetes Captcha
     * 
     * @return
     */
    public void setCaptchaImage(Captcha captchaImage) {
        this.captchaImage = captchaImage;
    }
    public void setBackGroundImageListFileName(String name)
    {
        fileName=name;
        load();
    }
    /**
     * gibt die Xmldatei zurück in der die Informationen der Huntergrundbilder
     * gespeichert sind
     * 
     * @return methodenpfad/bgimages.xml
     */
    private File getBgImagesXmlFile() {
        return new File(methode.getJacinfoXml().getParent(), fileName);
    }

    /**
     * läd die bgimages.xml der methode in die backgroundList
     */
    @SuppressWarnings("unchecked")
    private void load() {
            File file = getBgImagesXmlFile();
            if (file.exists())
                backgroundList = (Vector<BackGroundImage>) JDIO.loadObject(null, file, true);
            else
                backgroundList = new Vector<BackGroundImage>();
    }

    /**
     * Speichert alle Hintergrundbilder in der bgimages.xml ab
     */
    public void save() {

        File file = getBgImagesXmlFile();
        file.getParentFile().mkdirs();
        for (Iterator<BackGroundImage> iter = backgroundList.iterator(); iter.hasNext();) {
            BackGroundImage bgi = iter.next();
            if (bgi == null || bgi.getBackgroundImage() == null || bgi.getBackgroundImage().matches("\\s*")) iter.remove();
        }
        if (backgroundList.size() == 0)
            file.delete();
        else
            JDIO.saveObject(null, backgroundList, file, null, null, true);
    }

    /**
     * Sucht das Hintergrundbild bei dem die größte Übereinstimmung vorhanden
     * ist und reinigt das Captcha damit
     */
    public void clearCaptchaAll() {
        clearCaptchaAll(backgroundList);
    }

    /**
     * reinigt das Captcha
     * 
     * @param preview
     */
    public void clearCaptchaPreview(BackGroundImage preview) {
        captchaImage.grid = PixelGrid.getGridCopy(backupGrid);
        preview.clearCaptcha(captchaImage);
    }

    public void resetCaptcha() {
        captchaImage.grid = PixelGrid.getGridCopy(backupGrid);
    }

    /**
     * Sucht das Hintergrundbild bei dem die größte Übereinstimmung vorhanden
     * ist und reinigt das Captcha damit
     * 
     * @param preview
     */
    public void clearCaptchaAll(final Vector<BackGroundImage> preview) {
        Captcha best = null;
        BackGroundImage bestBgi = null;
        int bestVal = -1;
        final Captcha[] bgic = new Captcha[preview.size()];
        Thread[] cths = new Thread[bgic.length];
        for (int i = 0; i < cths.length; i++) {
            final int c = i;
            Thread th = new Thread(new Runnable() {
                public void run() {
                    try {
                        BackGroundImage bgi = preview.get(c);
                        Image bImage = bgi.getImage(methode);
                        if (bImage == null || bImage.getWidth(null) != captchaImage.getWidth() || bImage.getHeight(null) != captchaImage.getHeight()) {
                            if (Utilities.isLoggerActive()) {
                                JDLogger.getLogger().info("ERROR Maske und Bild passen nicht zusammmen");
                            }
                            synchronized (this) {
                                this.notify();
                            }
                            bgic[c] = null;
                            return;
                        }
                        bgic[c] = captchaImage.owner.createCaptcha(bImage);
                    } catch (Exception e) {
                        bgic[c] = null;
                    }
                    synchronized (this) {
                        this.notify();
                    }

                }
            });
            cths[i] = th;
            th.start();
        }
        for (int i = 0; i < cths.length; i++) {
            BackGroundImage bgi = preview.get(i);
            while (cths[i].isAlive()) {
                synchronized (cths[i]) {
                    try {
                        cths[i].wait(3000);
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            }
            Captcha captcha2 = bgic[i];
            if (captcha2 == null) continue;
            int color = bgi.getColor();
            int val = 0;
            outer: for (int x = 0; x < captchaImage.getWidth(); x++) {
                for (int y = 0; y < captchaImage.getHeight(); y++) {
                    bgi.setColor(captcha2.getPixelValue(x, y));
                    if (x == (captchaImage.getWidth() / 3) && val < (bestVal / 4)) break outer;
                    if (bgi.getColorDifference(backupGrid[x][y]) < bgi.getDistance()) val++;
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
            resetCaptcha();
            bestBgi.clearCaptcha(captchaImage);
        }
    }

    /**
     * löscht das Hintergrundbild aus der liste (Captcha wird nicht neu erstell)
     * 
     * @param dialogImage
     */
    public void remove(BackGroundImage dialogImage) {
        backgroundList.remove(dialogImage);
    }
}
