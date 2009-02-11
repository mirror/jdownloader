package jd.gui.skins.simple.components;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.plaf.FontUIResource;

import jd.config.Configuration;
import jd.config.Property;
import jd.event.ControlEvent;
import jd.event.ControlListener;
import jd.utils.JDUtilities;

public class SpeedMeterPanel extends JPanel implements ControlListener, ActionListener {

    /**
     * 
     */
    private static final long serialVersionUID = 5571694800446993879L;
    private int i;
    private int[] cache;
    private Thread th;

    private static final int CAPACITY = 40;

    public SpeedMeterPanel() {
        // Set background color for the applet's panel.
        this.i = 0;
        this.setOpaque(false);
        this.setBorder(BorderFactory.createEtchedBorder());
        this.cache = new int[CAPACITY];

        for (int x = 0; x < CAPACITY; x++) {
            cache[x] = 0;

        }
        this.setVisible(false);
        JDUtilities.getController().addControlListener(this);

    }

    public void start() {
        if (th != null) return;
        th = new Thread() {

            public void run() {
                while (!this.isInterrupted()) {

                    update();

                    try {
                        Thread.sleep(1000);
                        cache[i] = JDUtilities.getController().getSpeedMeter();
                        i++;
                        i = i % cache.length;
                    } catch (InterruptedException e) {
                        return;
                    }
                }
            }
        };
        th.start();

       
        fadeIn();
        // ctor
    }

    public void stop() {
        if(th!=null){
        th.interrupt();
        th = null;
        }
        fadeOut();
       
    }

    // public Dimension getPreferredSize() {
    // return new Dimension(WIDTH, HEIGHT);
    // }
    public synchronized void update() {
        repaint();
    }

    public void paintComponent(Graphics g) {
        // Paint background
        ((Graphics2D) g).setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, opacity));
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setStroke(new BasicStroke(1));
     
        // g2.clearRect(0, 0, getWidth(), getHeight());
        Color col1 = new Color(0x7CD622);
        Color col2 = new Color(0x339933);

        int id = i;
        int limit = JDUtilities.getSubConfig("DOWNLOAD").getIntegerProperty(Configuration.PARAM_DOWNLOAD_MAX_SPEED, 0) * 1024;
        int max = Math.max(10, limit);
        for (int x = 0; x < cache.length; x++) {
            max = Math.max(cache[x], max);
        }
        Polygon poly = new Polygon();
        int width = getWidth();
        int height = getHeight();
        poly.addPoint(0, height);

        for (int x = 0; x < CAPACITY; x++) {

            poly.addPoint((x * width) / (CAPACITY - 1), height - (int) (height * cache[id] * 0.9) / max);
            id++;
            id = id % cache.length;

        }
        poly.addPoint(width, height);

        ((Graphics2D) g).setPaint(new GradientPaint(width / 2, 0, col1, width / 2, height, col2.darker()));

        // g2.draw(poly);
        g2.fill(poly);
        FontUIResource f = (FontUIResource) UIManager.getDefaults().get("Panel.font");

        g2.setFont(f);

        String txt = JDUtilities.formatKbReadable(JDUtilities.getController().getSpeedMeter() / 1024) + "/s";
        FontMetrics fmetrics = g2.getFontMetrics();

        int len = fmetrics.stringWidth(txt);
        Color fontCol = Color.DARK_GRAY;
        if (limit > 0) {
            int limitpx = height - (int) (height * limit * 0.9) / max;
            g2.setColor(Color.RED);
            g2.drawLine(0, limitpx, width, limitpx);
            if (limitpx > height / 2) {
                g2.drawString(JDUtilities.formatKbReadable(limit / 1024) + "/s", 5, limitpx - 4);

            } else {
                g2.drawString(JDUtilities.formatKbReadable(limit / 1024) + "/s", 5, limitpx + 12);

            }
        }

        g2.setColor(fontCol);

        g2.drawString(JDUtilities.formatKbReadable(JDUtilities.getController().getSpeedMeter() / 1024) + "/s", width - len - 5, 12);

        // int[] xPoints = { 30, 700, 400 };
        // int[] yPoints = { 30, 30, 600 };
        // Polygon imageTriangle = new Polygon(xPoints, yPoints, 3);
        // g2.draw(imageTriangle);
        // g2.fill(imageTriangle);
        // Set current drawing color

        // Draw a rectangle centered at the mid-point

    } // paintComponent

    public void controlEvent(ControlEvent event) {

        if (event.getID() == ControlEvent.CONTROL_JDPROPERTY_CHANGED) {
            Property p = (Property) event.getSource();
            if (event.getParameter().equals(Configuration.PARAM_DOWNLOAD_MAX_SPEED)) {
                update();
            }
        }

    }

    private float opacity = 0f;
    private float fadeSteps = .1f;
    private Timer fadeTimer;

    public void fadeIn() {
        if (fadeTimer != null) {
            fadeTimer.stop();
            fadeTimer = null;
        }
        this.setVisible(true);
        fadeSteps = .1f;
        fadeTimer = new Timer(75, this);
        fadeTimer.setInitialDelay(0);
        fadeTimer.start();

    }

    public void fadeOut() {
        if (fadeTimer != null) {
            fadeTimer.stop();
            fadeTimer = null;
        }
        fadeSteps = -.1f;
        fadeTimer = new Timer(75, this);
        fadeTimer.setInitialDelay(0);
        fadeTimer.start();

    }

    public void actionPerformed(ActionEvent e) {
        opacity += fadeSteps;
        if (opacity > 1 ) {
            opacity = 1;
            fadeTimer.stop();
            fadeTimer = null;
        }else if(opacity < 0) {
            opacity = 0;
            this.setVisible(false);
            fadeTimer.stop();
            fadeTimer = null;
        }
        
  
        update();
    }
    // public void paintComponent(Graphics g) {
    // ((Graphics2D) g).setComposite(
    // AlphaComposite.getInstance(AlphaComposite.SRC_OVER,
    // opacity));
    // g.setColor(getBackground());
    // g.fillRect(0,0,getWidth(),getHeight());
    // }
    // }
}