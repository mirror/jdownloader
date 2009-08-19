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
    public Captcha captchaImage, lastCaptcha;
    public boolean foreground = true, fastSelection = false, add = true;
    public int zoom = 400;
    public Captcha captcha;
    
    public int tollerance = 25;
    public BufferedImage colorImage = new BufferedImage(28, 28, BufferedImage.TYPE_INT_RGB);
    public int foregroundColor1 = 0xff00ff, foregroundColor2 = 0xFF99FF, backgroundColor1 = 0x0000ff, backgroundColor2 = 0x00ffff;
    public Vector<CPoint> ret = new Vector<CPoint>(), lastRet = new Vector<CPoint>();
    public byte mode = CPoint.LAB_DIFFERENCE;
    public void removePixelAbsolut(CPoint cp) {
        ret.remove(cp);
        if (fastSelection) {
            for (int x = 0; x < captchaImage.getWidth(); x++) {
                for (int y = 0; y < captchaImage.getHeight(); y++) {
                    double dist = Colors.getColorDifference(captcha.getPixelValue(x, y), cp.getColor());
                    if (dist < cp.getDistance()) {
                        captchaImage.grid[x][y] = captcha.getPixelValue(x, y);
                    }

                }

            }
        } else {
            paintImage();
        }
    }

    public void addPixel(final CPoint p) {
        if (!ret.contains(p)) {
            ret.add(p);
            if (fastSelection) {
                for (int x = 0; x < captchaImage.getWidth(); x++) {
                    for (int y = 0; y < captchaImage.getHeight(); y++) {
                        captchaImage.grid[x][y] = captchaImage.getPixelValue(x, y);
                        if (p.getColorDifference(captcha.getPixelValue(x, y)) < p.getDistance()) {
                            captchaImage.grid[x][y] = p.isForeground() ? foregroundColor1 : backgroundColor1;
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

    public String getStatusString(int xb, int yb) {
        Graphics2D graphics = colorImage.createGraphics();
        final int xc = xb * 100 / zoom;
        final int yc = yb * 100 / zoom;

        final Color c = new Color(captcha.getPixelValue(xc, yc));
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
        for (Iterator<CPoint> iterator = ret.iterator(); iterator.hasNext();) {
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
        for (int x = 0; x < captchaImage.getWidth(); x++) {
            for (int y = 0; y < captchaImage.getHeight(); y++) {
                captchaImage.grid[x][y] = captcha.getPixelValue(x, y);
                double bestDist1 = Double.MAX_VALUE;
                CPoint cpBestDist1 = null;
                double bestDist2 = Double.MAX_VALUE;
                CPoint cpBestDist2 = null;
                for (CPoint cp : ret) {
                    double dist = cp.getColorDifference(captcha.getPixelValue(x, y));
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
                    captchaImage.grid[x][y] = cpBestDist2.isForeground() ? foregroundColor1 : backgroundColor1;
                } else if (cpBestDist1 != null) {
                    captchaImage.grid[x][y] = cpBestDist1.isForeground() ? foregroundColor2 : backgroundColor2;
                }

            }

        }
    }

    public void recreateCaptcha() {
        captchaImage = new Captcha(captcha.getWidth(), captcha.getHeight());

        captchaImage.grid = new int[captcha.getWidth()][captcha.getHeight()];

        paintImage();

    }

    /**
     * Läd das letzte captcha
     */
    public void loadLastImage() {
        ret = lastRet;
        captchaImage = lastCaptcha;
    }

    /**
     * legt ein backup vom aktuellen Captcha an
     */
    @SuppressWarnings("unchecked")
    public void backUP() {
        lastRet = (Vector<CPoint>) ret.clone();
        lastCaptcha = new Captcha(captchaImage.getHeight(), captchaImage.getWidth());
        lastCaptcha.grid = new int[captchaImage.getWidth()][captchaImage.getHeight()];
        for (int a = 0; a < captchaImage.grid.length; a++) {

            lastCaptcha.grid[a] = captchaImage.grid[a].clone();
        }
    }
    public CPoint getCPointFromMouseEvent(MouseEvent e) {
        CPoint p = new CPoint(e.getX() * 100 / zoom, e.getY() * 100 / zoom, (Integer) tollerance, captcha);
        p.setColorDistanceMode(mode);
        p.setForeground(foreground);
        return p;
    }
    /**
     * gibt vom Captcha ein um den Zoomfaktor Scalliertes Image zurück
     * 
     * @param zoom
     * @return Image
     */
    public Image getScaledCaptchaImage() {
        return captchaImage.getImage().getScaledInstance(captchaImage.getWidth() * zoom / 100, captchaImage.getHeight() * zoom / 100, Image.SCALE_DEFAULT);
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
