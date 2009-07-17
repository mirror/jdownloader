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
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Transparency;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JProgressBar;
import javax.swing.JWindow;
import javax.swing.Timer;
import javax.swing.plaf.basic.BasicProgressBarUI;

import jd.config.SubConfiguration;
import jd.controlling.JDController;
import jd.event.ControlEvent;
import jd.event.ControlListener;
import jd.gui.skins.simple.GuiRunnable;
import jd.gui.skins.simple.SimpleGuiConstants;
import jd.gui.swing.laf.LookAndFeelController;
import jd.nutils.nativeintegration.ScreenDevices;
import jd.utils.JDTheme;
import jd.utils.JDUtilities;
import net.miginfocom.swing.MigLayout;

import com.jtattoo.plaf.AbstractLookAndFeel;
import com.jtattoo.plaf.ColorHelper;
import com.jtattoo.plaf.JTattooUtilities;

class SplashProgressImage {

    private Image image;

    private long startTime = 0;
    private final int dur = 500;

    public SplashProgressImage(Image i) {
        image = i;
    }

    public Image getImage() {
        return image;
    }

    public float getAlpha() {
        if (this.startTime == 0) {
            this.startTime = System.currentTimeMillis();
        }
        return Math.min((System.currentTimeMillis() - startTime) / (float) dur, 1.0f);
    }

}

public class SplashScreen implements ActionListener, ControlListener {

    public static final int SPLASH_FINISH = 0;
    public static final int SPLASH_PROGRESS = 1;

    private float duration = 500.0f;

    private BufferedImage image;

    private JLabel label;

    private final int speed = 100;
    private long startTime = 0;

    private Timer timer;
    private JWindow window;

    private ArrayList<SplashProgressImage> progressimages;

    private int x;
    private int y;
    private int h;
    private int w;

    private int imageCounter = 1;

    private JProgressBar progress;

    private String curString = new String();

    private GraphicsDevice gd = null;

    public SplashScreen(JDController controller) throws IOException, AWTException {

        LookAndFeelController.setUIManager();
        this.image = (BufferedImage) JDTheme.I("gui.splash");
        progressimages = new ArrayList<SplashProgressImage>();
        progressimages.add(new SplashProgressImage(JDTheme.I("gui.splash.languages", 32, 32)));
        progressimages.add(new SplashProgressImage(JDTheme.I("gui.splash.settings", 32, 32)));
        progressimages.add(new SplashProgressImage(JDTheme.I("gui.splash.controller", 32, 32)));
        progressimages.add(new SplashProgressImage(JDTheme.I("gui.splash.update", 32, 32)));
        progressimages.add(new SplashProgressImage(JDTheme.I("gui.splash.plugins", 32, 32)));
        progressimages.add(new SplashProgressImage(JDTheme.I("gui.splash.screen", 32, 32)));
        progressimages.add(new SplashProgressImage(JDTheme.I("gui.splash.dllist", 32, 32)));
        try {
            Object loc = SubConfiguration.getConfig(SimpleGuiConstants.GUICONFIGNAME).getProperty("LOCATION_OF_MAINFRAME");
            if (loc != null && loc instanceof Point) {
                Point point = (Point) loc;
                if (point.x < 0) point.x = 0;
                if (point.y < 0) point.y = 0;
                gd = ScreenDevices.getGraphicsDeviceforPoint(point);
            } else {
                gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
            }
        } catch (Exception e) {
            gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        }
        Rectangle v = gd.getDefaultConfiguration().getBounds();
        int screenWidth = (int) v.getWidth();
        int screenHeight = (int) v.getHeight();
        x = screenWidth / 2 - image.getWidth(null) / 2;
        y = screenHeight / 2 - image.getHeight(null) / 2;
        w = image.getWidth(null);
        h = image.getHeight(null);
        initGui();
        startAnimation();
        controller.addControlListener(this);
    }

    private void startAnimation() {
        timer = new Timer(speed, this);
        timer.setCoalesce(true);
        timer.start();
        startTime = System.currentTimeMillis();
    }

    private void initGui() {
        GraphicsConfiguration gc = gd.getDefaultConfiguration();
        label = new JLabel();
        label.setIcon(drawImage(0.0f));

        window = new JWindow(gc);
        window.setLayout(new MigLayout("ins 0,wrap 1", "[grow,fill]", "[grow,fill]0[]"));
        window.setAlwaysOnTop(true);
        window.setSize(image.getWidth(null), image.getHeight(null));
        window.add(label);
        window.add(progress = new JProgressBar(), "hidemode 3,height 20!");
        progress.setVisible(true);
        progress.setUI(new BasicProgressBarUI() {

          

            protected void paintIndeterminate(Graphics g, JComponent c) {
                if (!(g instanceof Graphics2D)) { return; }
                Graphics2D g2D = (Graphics2D) g;

                Insets b = progressBar.getInsets(); // area for border
                int barRectWidth = progressBar.getWidth() - (b.right + b.left);
                int barRectHeight = progressBar.getHeight() - (b.top + b.bottom);

                Color colors[] = null;
                if (!JTattooUtilities.isActive(c)) {
                    colors = AbstractLookAndFeel.getTheme().getInActiveColors();
                } else if (c.isEnabled()) {
                    colors = AbstractLookAndFeel.getTheme().getProgressBarColors();
                } else {
                    colors = AbstractLookAndFeel.getTheme().getDisabledColors();
                }

                Color cHi = ColorHelper.darker(colors[colors.length - 1], 5);
                Color cLo = ColorHelper.darker(colors[colors.length - 1], 10);

                // Paint the bouncing box.
                Rectangle boxRect = getBox(null);
                if (boxRect != null) {
                    g2D.setColor(progressBar.getForeground());
                    JTattooUtilities.draw3DBorder(g, cHi, cLo, boxRect.x + 1, boxRect.y + 1, boxRect.width - 2, boxRect.height - 2);
                    JTattooUtilities.fillHorGradient(g, colors, boxRect.x + 2, boxRect.y + 2, boxRect.width - 4, boxRect.height - 4);
                }

                // Deal with possible text painting
                if (progressBar.isStringPainted()) {
                    Object savedRenderingHint = null;
                    if (AbstractLookAndFeel.getTheme().isTextAntiAliasingOn()) {
                        savedRenderingHint = g2D.getRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING);
                        
                        g2D.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
                    }
                    if (progressBar.getOrientation() == JProgressBar.HORIZONTAL) {
                        paintString(g2D, b.left, b.top, barRectWidth, barRectHeight, boxRect.width, b);
                    } else {
                        paintString(g2D, b.left, b.top, barRectWidth, barRectHeight, boxRect.height, b);
                    }
                    if (AbstractLookAndFeel.getTheme().isTextAntiAliasingOn()) {
                        g2D.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, savedRenderingHint);
                    }
                }
            }

            protected void paintDeterminate(Graphics g, JComponent c) {
                if (!(g instanceof Graphics2D)) { return; }

                Graphics2D g2D = (Graphics2D) g;
                Insets b = progressBar.getInsets(); // area for border
                int w = progressBar.getWidth() - (b.right + b.left);
                int h = progressBar.getHeight() - (b.top + b.bottom);

                // amount of progress to draw
                int amountFull = getAmountFull(b, w, h);
                Color colors[] = null;
                if (!JTattooUtilities.isActive(c)) {
                    colors = AbstractLookAndFeel.getTheme().getInActiveColors();
                } else if (c.isEnabled()) {
                    colors = AbstractLookAndFeel.getTheme().getProgressBarColors();
                } else {
                    colors = AbstractLookAndFeel.getTheme().getDisabledColors();
                }
                Color cHi = ColorHelper.darker(colors[colors.length - 1], 5);
                Color cLo = ColorHelper.darker(colors[colors.length - 1], 10);
                if (progressBar.getOrientation() == JProgressBar.HORIZONTAL) {
                    if (JTattooUtilities.isLeftToRight(progressBar)) {
                        JTattooUtilities.draw3DBorder(g, cHi, cLo, 2, 2, amountFull - 2, h - 2);
                        JTattooUtilities.fillHorGradient(g, colors, 3, 3, amountFull - 4, h - 4);
                    } else {
                        JTattooUtilities.draw3DBorder(g, cHi, cLo, w - amountFull + 2, 2, w - 2, h - 2);
                        JTattooUtilities.fillHorGradient(g, colors, w - amountFull + 3, 3, w - 4, h - 4);
                    }
                } else { // VERTICAL
                    JTattooUtilities.draw3DBorder(g, cHi, cLo, 2, 2, w - 2, amountFull - 2);
                    JTattooUtilities.fillVerGradient(g, colors, 3, 3, w - 4, amountFull - 4);
                }

                // Deal with possible text painting
                if (progressBar.isStringPainted()) {
                    Object savedRenderingHint = null;
                    if (AbstractLookAndFeel.getTheme().isTextAntiAliasingOn()) {
                        savedRenderingHint = g2D.getRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING);
                        g2D.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, AbstractLookAndFeel.getTheme().getTextAntiAliasingHint());
                    }
                    paintString(g, b.left, b.top, w, h, amountFull, b);
                    if (AbstractLookAndFeel.getTheme().isTextAntiAliasingOn()) {
                        g2D.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, savedRenderingHint);
                    }
                }
            }

            public void paint(Graphics g, JComponent c) {
                if (JTattooUtilities.getJavaVersion() >= 1.4) {
                    if (progressBar.isIndeterminate()) {
                        paintIndeterminate(g, c);
                    } else {
                        paintDeterminate(g, c);
                    }
                } else {
                    paintDeterminate(g, c);
                }
            }
        }

        );

        progress.setIndeterminate(true);
        window.pack();
        Rectangle b = gc.getBounds();
        window.setLocation(b.x + x, b.y + y);
        window.setVisible(true);

    }

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
        GraphicsConfiguration gc = gd.getDefaultConfiguration();
        BufferedImage res = gc.createCompatibleImage(w, h, Transparency.BITMASK);
        Graphics2D g2d = res.createGraphics();

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

    private void finish() {
        new GuiRunnable<Object>() {
            @Override
            public Object runSave() {
                timer.stop();
                window.dispose();
                return null;
            }
        }.start();
    }

    private void incProgress() {
        new GuiRunnable<Object>() {
            @Override
            public Object runSave() {
                progress.setStringPainted(true);
                progress.setString(curString);
                return null;
            }
        }.start();
    }

    public void controlEvent(ControlEvent event) {
        if (event.getID() == SPLASH_PROGRESS) {
            if (event.getParameter() != null && event.getParameter() instanceof String) {
                synchronized (curString) {
                    curString = (String) event.getParameter();
                }
            }
            imageCounter++;
            incProgress();
        } else if (event.getID() == ControlEvent.CONTROL_INIT_COMPLETE && event.getSource() instanceof Main) {
            JDUtilities.getController().removeControlListener(this);
            finish();
        } else if (event.getID() == SPLASH_FINISH) {
            JDUtilities.getController().removeControlListener(this);
            finish();
        }

    }
}