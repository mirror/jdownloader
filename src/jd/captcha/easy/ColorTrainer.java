//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
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

package jd.captcha.easy;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Iterator;
import java.util.Vector;

import jd.captcha.pixelgrid.Captcha;
import jd.captcha.translate.T;
import jd.nutils.Colors;
import jd.nutils.io.JDIO;

import org.jdownloader.controlling.FileCreationManager;

public class ColorTrainer {
    /**
     * Farben mit denen der Hintergrund/Fordergrund gefärbt wird foregroundColor1, backgroundColor1 = sicher zugeordnete Farben
     * foregroundColor2, backgroundColor2 = wahrscheinlich Fordergrund/Hintergrund
     */
    public static final int foregroundColor1     = 0xff00ff, foregroundColor2 = 0xFF99FF, backgroundColor1 = 0x0000ff, backgroundColor2 = 0x00ffff;

    /**
     * bearbeitetes Captcha
     */
    public Captcha          workingCaptcha;
    /**
     * Backup Captcha
     */
    public Captcha          backUpCaptcha;
    /**
     * Ob im Fordergrund oder im hintergrund selektiert wird
     */
    public boolean          foreground           = true;
    /**
     * schnelle und performante Selektionsmöglichkeit noch nicht fertig und deswegen vorerst deaktiviert
     */
    public boolean          fastSelection        = false;
    public boolean          add                  = true;
    /**
     * Zoomfaktor mit dem das Captcha angezeigt wird
     */
    public int              zoom                 = 400;
    /**
     * Orginal Captcha
     */
    public Captcha          originalCaptcha;
    /**
     * Farbtoleranzwert
     */
    public int              threshold            = 25;
    /**
     * Dieses Bild zeigt die aktuelle Farbinformation an
     */
    public BufferedImage    colorImage           = new BufferedImage(28, 28, BufferedImage.TYPE_INT_RGB);
    /**
     * Liste der Farbpunkte die zum identifizieren von Hintergrund und Fordergrund verwendet werden
     */
    public Vector<CPoint>   colorPointList       = new Vector<CPoint>();
    private Vector<CPoint>  colorPointListBackUp = new Vector<CPoint>();
    /**
     * Farbmodus wird in der Gui über eine Combobox gesetzt
     */
    public byte             colorDifferenceMode  = CPoint.LAB_DIFFERENCE;

    private void autoSetZoomFaktor() {
        if (originalCaptcha.getWidth() > 200 || originalCaptcha.getHeight() > 100)
            zoom = 100;
        else if (originalCaptcha.getWidth() > 100 || originalCaptcha.getHeight() > 50)
            zoom = 200;
        else
            zoom = 400;
    }

    /**
     * löscht einen cPoint aus der Liste und erstellt das WorkingCaptcha neu
     * 
     * @param CPoint
     */
    public void removeCPoint(CPoint cPoint) {
        colorPointList.remove(cPoint);
        if (fastSelection) {
            for (int x = 0; x < workingCaptcha.getWidth(); x++) {
                for (int y = 0; y < workingCaptcha.getHeight(); y++) {
                    double dist = Colors.getColorDifference(originalCaptcha.getPixelValue(x, y), cPoint.getColor());
                    if (dist < cPoint.getDistance()) {
                        workingCaptcha.grid[x][y] = originalCaptcha.getPixelValue(x, y);
                    }

                }

            }
        } else {
            recreateWorkingCaptcha();
        }
    }

    /**
     * fügt einen CPoint zur Liste und erstellt das WorkingCaptcha neu
     * 
     * @param cPoint
     */
    public void addCPoint(final CPoint cPoint) {
        if (!colorPointList.contains(cPoint)) {
            colorPointList.add(cPoint);
            if (fastSelection) {
                for (int x = 0; x < workingCaptcha.getWidth(); x++) {
                    for (int y = 0; y < workingCaptcha.getHeight(); y++) {
                        workingCaptcha.grid[x][y] = workingCaptcha.getPixelValue(x, y);
                        if (cPoint.getColorDifference(originalCaptcha.getPixelValue(x, y)) < cPoint.getDistance()) {
                            workingCaptcha.grid[x][y] = cPoint.isForeground() ? foregroundColor1 : backgroundColor1;
                        }

                    }

                }
            } else {
                recreateWorkingCaptcha();
            }

        }

    }

    /**
     * fügt bei Zahlen die kleiner sind als 100 Lehrzeichen hinzu
     * 
     * @param i
     * @return
     */
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
     * Gibt einen String mit Informationen (rgb hsb usw.) über den pixel an der Position x y an
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

        return "<HTML><BODY>" + T._.easycaptcha_color() + ":#" + Integer.toHexString(c.getRGB() & 0x00ffffff) + "<BR>\r\n" + xc + ":" + yc + "<BR>\r\n<span style=\"color:#" + Integer.toHexString(new Color(c.getRed(), 0, 0).getRGB() & 0x00ffffff) + "\">R:" + getDigit(c.getRed()) + "</span><span style=\"color:#" + Integer.toHexString(new Color(0, c.getGreen(), 0).getRGB() & 0x00ffffff) + "\"> G:" + getDigit(c.getGreen()) + "</span><span style=\"color:#" + Integer.toHexString(new Color(0, 0, c.getBlue()).getRGB() & 0x00ffffff) + "\"> B:" + getDigit(c.getBlue()) + "</span><BR>\r\nH:" + getDigit(Math.round(hsb[0] * 360)) + " S:" + getDigit(Math.round(hsb[1] * 100)) + " B:" + getDigit(Math.round(hsb[2] * 100)) + "\r\n</BODY></HTML>";
    }

    /**
     * Sucht den ColorPoint aus der colorPointList der die meiste Übereinstimmung mit der Farbe hat gibt null aus wenn die Farbe keinem
     * ColorPoint der Liste entspricht Wird beim löschen von Farbpunkten aus der Liste verwendet
     * 
     * @param color
     * @return matching CPoint
     */
    public CPoint searchCPoint(CPoint color) {
        return searchCPoint(color.getColor());
    }

    /**
     * Sucht den ColorPoint aus der colorPointList der die meiste Übereinstimmung mit der Farbe hat gibt null aus wenn die Farbe keinem
     * ColorPoint der Liste entspricht Wird beim löschen von Farbpunkten aus der Liste verwendet
     * 
     * @param color
     * @return matching CPoint
     */
    public CPoint searchCPoint(int color) {
        double bestDist = Integer.MAX_VALUE;
        CPoint bestPX = null;
        for (Iterator<CPoint> iterator = colorPointList.iterator(); iterator.hasNext();) {
            CPoint p = iterator.next();
            double dist = 0;
            if (p.getDistance() == 0) {
                if (color == p.getColor()) {
                    bestPX = p;
                    break;
                }

            } else if ((dist = p.getColorDifference(color)) < p.getDistance()) {
                if (dist < bestDist) {
                    bestPX = p;
                    bestDist = dist;
                }
            }
        }
        return bestPX;
    }

    /**
     * Zeichnet Fordergrund und Hintergrund auf dem workingCaptcha neu
     */
    private void recreateWorkingCaptcha() {
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

    /**
     * Erstellt das Captcha auf dem Fordergrund und Hintergrund gezeichnet wird
     */
    public void createWorkingCaptcha() {
        workingCaptcha = new Captcha(originalCaptcha.getWidth(), originalCaptcha.getHeight());
        workingCaptcha.grid = new int[originalCaptcha.getWidth()][originalCaptcha.getHeight()];
        recreateWorkingCaptcha();
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
    public void backUP() {
        if (colorPointList != null)
            colorPointListBackUp = new Vector<CPoint>(colorPointList);
        else
            colorPointListBackUp = null;
        backUpCaptcha = new Captcha(workingCaptcha.getHeight(), workingCaptcha.getWidth());
        backUpCaptcha.grid = new int[workingCaptcha.getWidth()][workingCaptcha.getHeight()];
        for (int a = 0; a < workingCaptcha.grid.length; a++) {

            backUpCaptcha.grid[a] = workingCaptcha.grid[a].clone();
        }
    }

    /**
     * Farbpunkt der bei den Koordinaten des MouseEvents liegt aus
     */
    public CPoint getCPointFromMouseEvent(MouseEvent e) {
        CPoint p = new CPoint(e.getX() * 100 / zoom, e.getY() * 100 / zoom, threshold, originalCaptcha);
        p.setColorDistanceMode(colorDifferenceMode);
        p.setForeground(foreground);
        return p;
    }

    /**
     * gibt vom bearbeiteten Captcha ein um den Zoomfaktor Scalliertes Image zurück
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
        autoSetZoomFaktor();
        return originalCaptcha.getImage().getScaledInstance(originalCaptcha.getWidth() * zoom / 100, originalCaptcha.getHeight() * zoom / 100, Image.SCALE_DEFAULT);
    }

    /**
     * läd einen Vector<CPoint> aus eine XML Datei (methodedir/CPoints.xml)
     * 
     * @param file
     * @return
     */
    @SuppressWarnings("unchecked")
    public static Vector<CPoint> load(File file) {
        if (file.exists()) return (Vector<CPoint>) JDIO.loadObject(file, true);
        return new Vector<CPoint>();
    }

    /**
     * Speichert einen Vector<CPoint> in eine XML Datei (methodedir/CPoints.xml)
     * 
     * @param cPoints
     * @param file
     */
    public static void saveColors(Vector<CPoint> cPoints, File file) {
        FileCreationManager.getInstance().mkdir(file.getParentFile());
        JDIO.saveObject(cPoints, file, true);
    }

}