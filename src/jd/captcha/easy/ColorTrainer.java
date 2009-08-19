package jd.captcha.easy;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Iterator;
import java.util.Vector;
import jd.nutils.io.JDIO;
import jd.utils.locale.JDL;
import jd.nutils.Colors;
import jd.captcha.pixelgrid.Captcha;

public class ColorTrainer {
    /**
     * Farben mit denen der Hintergrund/Fordergrund gefärbt wird
     * foregroundColor1, backgroundColor1 = sicher zugeordnete Farben
     * foregroundColor2, backgroundColor2 = wahrscheinlich
     * Fordergrund/Hintergrund
     */
    public static final int foregroundColor1 = 0xff00ff, foregroundColor2 = 0xFF99FF, backgroundColor1 = 0x0000ff, backgroundColor2 = 0x00ffff;

    /**
     * bearbeitetes Captcha
     */
    public Captcha workingCaptcha;
    /**
     * Backup Captcha
     */
    public Captcha backUpCaptcha;
    /**
     * Ob im Fordergrund oder im hintergrund selektiert wird
     */
    public boolean foreground = true;
    /**
     * schnelle und performante Selektionsmöglichkeit ist aber exakt TODO muss
     * noch verbessert werden möglicherweise komplett entfernen
     */
    public boolean fastSelection = false;
    public boolean add = true;
    /**
     * Zoomfaktor mit dem das Captcha angezeigt wird
     */
    public int zoom = 400;
    /**
     * Orginal Captcha
     */
    public Captcha originalCaptcha;
    /**
     * Farbtoleranzwert
     */
    public int threshold = 25;
    /**
     * Dieses Bild zeigt die aktuelle Farbinformation an
     */
    public BufferedImage colorImage = new BufferedImage(28, 28, BufferedImage.TYPE_INT_RGB);
    /**
     * Liste der Farbpunkte die zum identifizieren von Hintergrund und
     * Fordergrund verwendet werden
     */
    public Vector<CPoint> colorPointList = new Vector<CPoint>();
    private Vector<CPoint> colorPointListBackUp = new Vector<CPoint>();
    public byte colorDifferenceMode = CPoint.LAB_DIFFERENCE;

    public void removePixelAbsolut(CPoint cp) {
        colorPointList.remove(cp);
        if (fastSelection) {
            for (int x = 0; x < workingCaptcha.getWidth(); x++) {
                for (int y = 0; y < workingCaptcha.getHeight(); y++) {
                    double dist = Colors.getColorDifference(originalCaptcha.getPixelValue(x, y), cp.getColor());
                    if (dist < cp.getDistance()) {
                        workingCaptcha.grid[x][y] = originalCaptcha.getPixelValue(x, y);
                    }

                }

            }
        } else {
            paintImage();
        }
    }

    public void addPixel(final CPoint p) {
        if (!colorPointList.contains(p)) {
            colorPointList.add(p);
            if (fastSelection) {
                for (int x = 0; x < workingCaptcha.getWidth(); x++) {
                    for (int y = 0; y < workingCaptcha.getHeight(); y++) {
                        workingCaptcha.grid[x][y] = workingCaptcha.getPixelValue(x, y);
                        if (p.getColorDifference(originalCaptcha.getPixelValue(x, y)) < p.getDistance()) {
                            workingCaptcha.grid[x][y] = p.isForeground() ? foregroundColor1 : backgroundColor1;
                        }

                    }

                }
            } else {
                paintImage();
            }

        }

    }

    private String getDigit(int i) {
        String ret = "";
        if (i < 10)
            ret = i + "&nbsp;&nbsp;&nbsp;&nbsp;";
        else if (i < 100)
            ret = i + "&nbsp;&nbsp;";
        else
            ret += i;
        return ret;
    }

    /**
     * Gibt einen String mit Informationen (rgb hsb usw.) über den pixel an der
     * Position x y an
     * 
     * @param x
     * @param y
     * @return
     */
    public String getStatusString(int xb, int yb) {
        Graphics2D graphics = colorImage.createGraphics();
        final int xc = xb * 100 / zoom;
        final int yc = yb * 100 / zoom;

        final Color c = new Color(originalCaptcha.getPixelValue(xc, yc));
        for (int y = 0; y < colorImage.getHeight(); y++) {

            for (int x = 0; x < colorImage.getWidth(); x++) {

                graphics.setColor(c);

                graphics.fillRect(x, y, 1, 1);

            }

        }
        for (int x = 0; x < colorImage.getWidth(); x++) {

            graphics.setColor(Color.black);

            graphics.fillRect(x, 0, 1, 1);

            graphics.fillRect(x, colorImage.getHeight(), 1, 1);

        }

        for (int y = 0; y < colorImage.getHeight(); y++) {

            graphics.setColor(Color.black);

            graphics.fillRect(0, y, 1, 1);

            graphics.fillRect(colorImage.getWidth(), y, 1, 1);

        }
        float[] hsb = Colors.rgb2hsb(c.getRed(), c.getGreen(), c.getBlue());

        return "<HTML><BODY>" + JDL.L("easycaptcha.color", "Color") + ":#" + Integer.toHexString(c.getRGB() & 0x00ffffff) + "<BR>\r\n" + xc + ":" + yc + "<BR>\r\n" + "<span style=\"color:#" + Integer.toHexString(new Color(c.getRed(), 0, 0).getRGB() & 0x00ffffff) + "\">R:" + getDigit(c.getRed()) + "</span><span style=\"color:#" + Integer.toHexString(new Color(0, c.getGreen(), 0).getRGB() & 0x00ffffff) + "\"> G:" + getDigit(c.getGreen()) + "</span><span style=\"color:#" + Integer.toHexString(new Color(0, 0, c.getBlue()).getRGB() & 0x00ffffff) + "\"> B:" + getDigit(c.getBlue()) + "</span><BR>\r\n" + "H:" + getDigit(Math.round(hsb[0] * 360)) + " S:" + getDigit(Math.round(hsb[1] * 100)) + " B:" + getDigit(Math.round(hsb[2] * 100)) + "\r\n</BODY></HTML>";
    }

    public CPoint getBestPixel(final CPoint pr) {
        int co = pr.getColor();
        double bestDist = Integer.MAX_VALUE;
        CPoint bestPX = null;
        for (Iterator<CPoint> iterator = colorPointList.iterator(); iterator.hasNext();) {
            CPoint p = (CPoint) iterator.next();
            double dist = 0;
            if (p.getDistance() == 0) {
                if (co == p.getColor()) {
                    bestPX = p;
                    break;
                }

            } else if ((dist = p.getColorDifference(co)) < p.getDistance()) {
                if (dist < bestDist) {
                    bestPX = p;
                    bestDist = dist;
                }
            }
        }
        return bestPX;
    }

    private void paintImage() {
        for (int x = 0; x < workingCaptcha.getWidth(); x++) {
            for (int y = 0; y < workingCaptcha.getHeight(); y++) {
                workingCaptcha.grid[x][y] = originalCaptcha.getPixelValue(x, y);
                double bestDist1 = Double.MAX_VALUE;
                CPoint cpBestDist1 = null;
                double bestDist2 = Double.MAX_VALUE;
                CPoint cpBestDist2 = null;
                for (CPoint cp : colorPointList) {
                    double dist = cp.getColorDifference(originalCaptcha.getPixelValue(x, y));
                    if (bestDist1 > dist) {
                        bestDist1 = dist;
                        cpBestDist1 = cp;
                    }
                    if (dist < cp.getDistance()) {
                        if (bestDist2 > dist) {
                            bestDist2 = 0;
                            cpBestDist2 = cp;
                        }
                    }
                }
                if (cpBestDist2 != null) {
                    workingCaptcha.grid[x][y] = cpBestDist2.isForeground() ? foregroundColor1 : backgroundColor1;
                } else if (cpBestDist1 != null) {
                    workingCaptcha.grid[x][y] = cpBestDist1.isForeground() ? foregroundColor2 : backgroundColor2;
                }

            }

        }
    }

    public void recreateCaptcha() {
        workingCaptcha = new Captcha(originalCaptcha.getWidth(), originalCaptcha.getHeight());

        workingCaptcha.grid = new int[originalCaptcha.getWidth()][originalCaptcha.getHeight()];

        paintImage();

    }

    /**
     * übergibt Einstellungen an einen anderen ColorTrainer
     * 
     * @param colorTrainer
     */
    public void copySettingsTo(ColorTrainer colorTrainer) {
        colorTrainer.fastSelection = fastSelection;
        colorTrainer.foreground = foreground;
        colorTrainer.add = add;
        colorTrainer.threshold = threshold;
        colorTrainer.colorDifferenceMode = colorDifferenceMode;
    }

    /**
     * Läd das letzte captcha
     */
    public void loadLastImage() {
        colorPointList = colorPointListBackUp;
        workingCaptcha = backUpCaptcha;
    }

    /**
     * legt ein backup vom aktuellen Captcha an
     */
    @SuppressWarnings("unchecked")
    public void backUP() {
        colorPointListBackUp = (Vector<CPoint>) colorPointList.clone();
        backUpCaptcha = new Captcha(workingCaptcha.getHeight(), workingCaptcha.getWidth());
        backUpCaptcha.grid = new int[workingCaptcha.getWidth()][workingCaptcha.getHeight()];
        for (int a = 0; a < workingCaptcha.grid.length; a++) {

            backUpCaptcha.grid[a] = workingCaptcha.grid[a].clone();
        }
    }

    public CPoint getCPointFromMouseEvent(MouseEvent e) {
        CPoint p = new CPoint(e.getX() * 100 / zoom, e.getY() * 100 / zoom, (Integer) threshold, originalCaptcha);
        p.setColorDistanceMode(colorDifferenceMode);
        p.setForeground(foreground);
        return p;
    }

    /**
     * gibt vom bearbeiteten Captcha ein um den Zoomfaktor Scalliertes Image
     * zurück
     * 
     * @param zoom
     * @return Image
     */
    public Image getScaledWorkingCaptchaImage() {
        return workingCaptcha.getImage().getScaledInstance(workingCaptcha.getWidth() * zoom / 100, workingCaptcha.getHeight() * zoom / 100, Image.SCALE_DEFAULT);
    }

    /**
     * gibt vom Original Captcha ein um den Zoomfaktor Scalliertes Image zurück
     * 
     * @param zoom
     * @return Image
     */
    public Image getScaledOriginalCaptchaImage() {
        return originalCaptcha.getImage().getScaledInstance(originalCaptcha.getWidth() * zoom / 100, originalCaptcha.getHeight() * zoom / 100, Image.SCALE_DEFAULT);
    }

    @SuppressWarnings("unchecked")
    public static Vector<CPoint> load(File file) {
        if (file.exists()) { return (Vector<CPoint>) JDIO.loadObject(null, file, true); }
        return new Vector<CPoint>();
    }

    public static void saveColors(Vector<CPoint> cc, File file) {
        file.getParentFile().mkdirs();
        JDIO.saveObject(null, cc, file, null, null, true);
    }

}
