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

package jd;

import java.awt.AWTException;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.Transparency;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JWindow;
import javax.swing.Timer;

import jd.gui.JDLookAndFeelManager;

public class SplashScreen implements ActionListener {

    private float duration = 500.0f;

    private BufferedImage image;

    private JLabel label;

    private final int speed = 1000 / 10;
    private long startTime = 0;

    private Timer timer;
    private JWindow window;

    // private BufferedImage screenshot;

    private ArrayList<SplashProgressImage> progressimages;

    private int x;

    private int y;

    private int h;

    private int w;

    private int imageCounter = 1;

    public void setNextImage() {
        imageCounter++;
    }

    public SplashScreen(Image image) throws IOException, AWTException {
        // final URL url =
        // this.getClass().getClassLoader().getResource(path);
        JDLookAndFeelManager.setUIManager();
        this.image = (BufferedImage) image;
        progressimages = new ArrayList<SplashProgressImage>();
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();

        Rectangle v = ge.getDefaultScreenDevice().getDefaultConfiguration().getBounds();
        int screenWidth = (int) v.getWidth();
        int screenHeight = (int) v.getHeight();
        x = (int) (screenWidth / 2 - image.getWidth(null) / 2);
        y = (int) (screenHeight / 2 - image.getHeight(null) / 2);
        w = image.getWidth(null);
        h = image.getHeight(null);
        // createScreenshot();
        initGui();
        startAnimation();
    }

    private void startAnimation() {
        timer = new Timer(speed, this);
        timer.setCoalesce(true);
        timer.start();
        startTime = System.currentTimeMillis();
    }

    private void initGui() {
        GraphicsConfiguration gc = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration();
        label = new JLabel();
        label.setIcon(drawImage(0.0f));

        window = new JWindow(gc);
        window.setAlwaysOnTop(true);
        window.setSize(image.getWidth(null), image.getHeight(null));
        Container content = window.getContentPane();
        content.add(label);
        window.pack();
        Rectangle b = gc.getBounds();
        window.setLocation(b.x + x, b.y + y);
        window.setVisible(true);

    }

    // private void createScreenshot() throws AWTException {
    // final Robot robot = new Robot();
    // final Rectangle rectangle = new Rectangle(x, y, w, h);
    // screenshot = robot.createScreenCapture(rectangle);
    // }

    public void actionPerformed(ActionEvent e) {
        float percent = Math.min(1.0f, (System.currentTimeMillis() - startTime) / duration);
        label.setIcon(drawImage(percent));
        label.repaint();
    }

    /**
     * Draws Background, then draws image over it
     * 
     * @param alphaValue
     * @throws AWTException
     */
    private ImageIcon drawImage(float alphaValue) {
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice gd = ge.getDefaultScreenDevice();
        GraphicsConfiguration gc = gd.getDefaultConfiguration();
        BufferedImage res = gc.createCompatibleImage(w, h, Transparency.BITMASK);
        Graphics2D g2d = res.createGraphics();
        // RenderingHints qualityHints = new
        // RenderingHints(RenderingHints.KEY_ANTIALIASING,
        // RenderingHints.VALUE_ANTIALIAS_ON);
        // qualityHints.put(RenderingHints.KEY_RENDERING,
        // RenderingHints.VALUE_RENDER_QUALITY);
        // g2d.setRenderingHints(qualityHints);

        // g2d.drawImage(screenshot, 0, 0, null);
        g2d.setColor(Color.WHITE);
        g2d.fillRect(1, 1, w - 2, h - 2);
        g2d.setColor(Color.BLACK.brighter());
        g2d.drawRect(0, 0, w - 1, h - 1);

        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alphaValue));
        g2d.drawImage(image, 0, 0, null);
        if (progressimages.size() > 0) {
            int steps = (image.getWidth(null) - 20 - progressimages.get(0).getImage().getWidth(null)) / Math.max(2, (progressimages.size() - 1));
            for (int i = 0; i < Math.min(progressimages.size(), imageCounter); i++) {
                g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.min(alphaValue, progressimages.get(i).getAlpha())));
                g2d.drawImage(this.progressimages.get(i).getImage(), 10 + i * steps, image.getHeight() - 10 - progressimages.get(i).getImage().getHeight(null), null);
            }

            for (int i = imageCounter; i < progressimages.size(); i++) {
                g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.min(alphaValue, 0.2f)));
                g2d.drawImage(this.progressimages.get(i).getImage(), 10 + i * steps, image.getHeight() - 10 - progressimages.get(i).getImage().getHeight(null), null);
            }
        }
        g2d.dispose();

        return new ImageIcon(res);
    }

    public void addProgressImage(SplashProgressImage splashProgressImage) {
        this.progressimages.add(splashProgressImage);
    }

    public void finish() {
        timer.stop();
        window.dispose();
    }

}