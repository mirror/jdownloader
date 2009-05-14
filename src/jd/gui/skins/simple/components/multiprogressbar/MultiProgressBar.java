package jd.gui.skins.simple.components.multiprogressbar;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.io.IOException;
import java.util.ArrayList;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.LineBorder;

import net.miginfocom.swing.MigLayout;

public class MultiProgressBar extends JPanel {

    private static final long serialVersionUID = -7489358722203326526L;
    private ArrayList<ProgressEntry> entries;
    private long maximum;
    private long value;

    public MultiProgressBar() {
        entries = new ArrayList<ProgressEntry>();
        this.setBackground(getBackground().darker());
        LineBorder b = new LineBorder(getBackground().brighter(), 1, true);
        this.setBorder(b);
    }

    public static void main(String args[]) throws IOException {
        MultiProgressBar pm = new MultiProgressBar();

        pm.setMaximums(10, 20, 30, 10, 20);
        pm.setValues(3, 12, 11, 4, 19);
        JFrame f = new JFrame();
        f.setLayout(new MigLayout("ins 10"));
        f.add(pm);
        f.pack();
        f.setVisible(true);
    }

    public Dimension getPreferredSize() {
        return new Dimension(600, 30);
    }

    private int scale(long point, double faktor) {
        return (int) (point / faktor);
    }

    private double getFaktor() {
        return maximum / (double) Math.max(1, (getWidth()));
    }

    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        int width = this.getWidth();
        int height = (int) (this.getHeight() * 0.8);
        Color col1 = new Color(0x7CD622);
        Color col2 = new Color(0x339933);
        double faktor = getFaktor();
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setStroke(new BasicStroke(1));
        ProgressEntry e;
        for (int i = 0; i < entries.size(); i++) {
            e = entries.get(i);

            Rectangle rec = new Rectangle(scale(e.getPosition(), faktor), 0, scale(e.getValue(), faktor), height);

            ((Graphics2D) g).setPaint(new GradientPaint(width / 2, 0, col1, width / 2, height, col2.darker()));
            g2.fill(rec);
            ((Graphics2D) g).setPaint(Color.black);
            g2.drawLine(scale(e.getPosition(), faktor), 0, scale(e.getPosition(), faktor), height);

        }

        col1 = col1.brighter();
        col2 = col2.brighter();
        ((Graphics2D) g).setPaint(getBackground().darker().darker());
        g2.setStroke(new BasicStroke(2));
        g2.drawLine(0, height, width, height);
        Rectangle rec = new Rectangle(0, height + 2, scale(value, faktor), getHeight() - height);
        ((Graphics2D) g).setPaint(new GradientPaint(width / 2, 0, col1, width / 2, height, col2.darker()));
        g2.fill(rec);
        g2.dispose();

    }

    public void setValues(long... values) {
        synchronized (entries) {
            for (int i = 0; i < values.length; i++) {
                if (entries.size() <= i) {
                    entries.add(new ProgressEntry(0));
                }
                entries.get(i).setValue(values[i]);

            }
        }
        update();
        this.repaint();
    }

    public void setMaximums(long... max) {
        if (max == null) {
            entries = new ArrayList<ProgressEntry>();
            return;
        }
        synchronized (entries) {
            for (int i = 0; i < max.length; i++) {
                if (entries.size() <= i) {
                    entries.add(new ProgressEntry(max[i]));
                } else {
                    entries.get(i).setMaximum(max[i]);
                }
            }
        }
        update();
        this.repaint();
    }

    private void update() {
        long max = 0;
        long totalvalue = 0;
        synchronized (entries) {
            ProgressEntry e;
            for (int i = 0; i < entries.size(); i++) {
                e = entries.get(i);
                e.setPosition(max);
                max += e.getMaximum();
                totalvalue += e.getValue();
            }
            this.maximum = max;
            this.value = totalvalue;
        }

    }
}
