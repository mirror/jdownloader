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

package jd.gui.skins.simple.components;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.logging.Logger;

import javax.imageio.ImageIO;
import javax.swing.JComponent;

import jd.http.Encoding;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;

/**
 * Die ChartAPI greift auf die Google Chart API zurück Sie funktioniert nur mit
 * einer intakten Internet-Verbindung!
 * 
 * @author gluewurm
 * 
 */
public abstract class ChartAPI extends JComponent {

    private static final long serialVersionUID = -3619951791325759665L;

    class PictureLoader extends Thread {
        private String path;

        public PictureLoader(String path) {
            this.path = path;
        }

        public void run() {
            BufferedImage image = null;
            try {
                URL url = new URL(path);
                image = ImageIO.read(url);
            } catch (IOException ex) {
                logger.finest("Can not read : " + path);
            }
            if (image != null) setImage(image);
        }
    }

    private final String serverAdress = "chart.apis.google.com";
    private Logger logger = JDUtilities.getLogger();
    private HashMap<String, ChartAPI_Entity> collData = new HashMap<String, ChartAPI_Entity>();
    private int width;
    private int height;
    private Color backgroundColor;
    private BufferedImage image;
    private PictureLoader loader;
    private String caption;

    public ChartAPI(String caption, int width, int height, Color backgroundColor) {
        this.caption = caption;
        this.width = width;
        this.height = height;
        this.backgroundColor = backgroundColor;
        setPreferredSize(new Dimension(width, height));
    }

    public void clear() {
        collData.clear();
    }

    public void addEntity(ChartAPI_Entity input) {
        collData.put(input.getCaption(), input);
    }

    public void removeEntity(ChartAPI_Entity input) {
        collData.remove(input.getCaption());
    }

    public void removeEntity(String caption) {
        collData.remove(caption);
    }

    public ChartAPI_Entity getEntity(String caption) {
        return collData.get(caption);
    }

    public HashMap<String, ChartAPI_Entity> getHashMap() {
        return collData;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public Color getBackgroundColor() {
        return backgroundColor;
    }

    public void setBackgroundColor(Color backgroundColor) {
        this.backgroundColor = backgroundColor;
    }

    public String getServerAdress() {
        return serverAdress;
    }

    public String createCaptionString() {
        String data = "";
        for (ChartAPI_Entity tmp : collData.values()) {
            data += Encoding.urlEncode(tmp.getCaption()) + "|";
        }
        return data;
    }

    public long getMaxValue() {
        long max = 0;
        for (ChartAPI_Entity tmp : collData.values()) {
            if (Long.valueOf(tmp.getData()) > max) max = Long.valueOf(tmp.getData());
        }
        return max;
    }

    public String getRelativeValue(String input) {
        double calc = Double.valueOf(input) / Double.valueOf(getMaxValue()) * 100;
        if (calc < 0.01 && calc > 0.0)
            calc = 0.01;
        else if (calc < 0) calc = 0;
        String ret = String.valueOf(calc);
        if(ret.length() > 16) ret = ret.substring(0, 16);
        return ret;
    }

    public void downloadImage(String path) {
        loader = new PictureLoader(path);
        loader.start();
    }

    public void setImage(BufferedImage image) {
        this.image = image;
        Dimension d = new Dimension(image.getWidth(), image.getHeight());
        setPreferredSize(d);
        revalidate();
        repaint();
    }

    public void paintComponent(Graphics g) {
        g.setFont(new Font("Arial", Font.BOLD, 12));
        if (image != null) {
            int x = (getWidth() - image.getWidth()) / 2;
            int y = (getHeight() - image.getHeight()) / 2;
            g.drawImage(image, x, y, this);
            g.drawString(caption, 0, 10);
            return;
        }

        g.drawImage(image, 0, 0, null);
        g.drawString(JDLocale.LF("plugins.config.premium.chartapi.caption.error", "%s loading or not available", caption), 0, 10);
    }

    public abstract String createDataString();

    public abstract String getUrl();
}
