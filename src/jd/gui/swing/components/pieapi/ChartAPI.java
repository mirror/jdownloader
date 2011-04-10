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

package jd.gui.swing.components.pieapi;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.logging.Logger;

import javax.imageio.ImageIO;
import javax.swing.JComponent;

import jd.controlling.JDLogger;
import jd.nutils.encoding.Encoding;

import org.jdownloader.gui.translate.T;

/**
 * Die ChartAPI greift auf die Google Chart API zurück. Sie funktioniert nur mit
 * einer intakten Internet-Verbindung!
 * 
 * @author gluewurm
 */
public abstract class ChartAPI extends JComponent {

    private static final long serialVersionUID = -3619951791325759665L;

    private class PictureLoader extends Thread {
        private String path;

        public PictureLoader(String path) {
            this.path = path;
        }

        @Override
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

    private Logger                          logger   = JDLogger.getLogger();
    private HashMap<String, ChartAPIEntity> collData = new HashMap<String, ChartAPIEntity>();
    private int                             width;
    private int                             height;
    protected Image                         image;
    private PictureLoader                   loader;

    public ChartAPI(int width, int height) {
        this.width = width;
        this.height = height;
        setPreferredSize(new Dimension(width, height));
    }

    public void clear() {
        collData.clear();
    }

    public void addEntity(ChartAPIEntity input) {
        collData.put(input.getCaption(), input);
    }

    public void removeEntity(ChartAPIEntity input) {
        collData.remove(input.getCaption());
    }

    public void removeEntity(String caption) {
        collData.remove(caption);
    }

    public ChartAPIEntity getEntity(String caption) {
        return collData.get(caption);
    }

    public HashMap<String, ChartAPIEntity> getHashMap() {
        return collData;
    }

    @Override
    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    @Override
    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public String createCaptionString() {
        StringBuilder data = new StringBuilder();
        for (ChartAPIEntity tmp : collData.values()) {
            if (data.length() > 0) data.append('|');
            data.append(Encoding.urlEncode(tmp.getCaption()));
        }
        return data.toString();
    }

    public long getMaxValue() {
        long max = 0;
        for (ChartAPIEntity tmp : collData.values()) {
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
        if (ret.length() > 16) ret = ret.substring(0, 16);
        return ret;
    }

    public void downloadImage(String path) {
        loader = new PictureLoader(path);
        loader.start();
    }

    public void setImage(Image image) {
        this.image = image;

        Dimension d = new Dimension(image.getWidth(null), image.getHeight(null));
        setPreferredSize(d);
        revalidate();
        repaint();
    }

    @Override
    public void paintComponent(Graphics g) {
        g.setFont(new Font("Arial", Font.BOLD, 12));
        if (image != null) {
            int x = (getWidth() - image.getWidth(null)) / 2;
            int y = (getHeight() - image.getHeight(null)) / 2;
            g.drawImage(image, x, y, this);
            return;
        }

        g.drawImage(image, 0, 0, null);
        g.drawString(T._.plugins_config_premium_chartapi_caption_error2(), 0, 10);
    }

    public abstract String createDataString();

    public abstract String getUrl();
}