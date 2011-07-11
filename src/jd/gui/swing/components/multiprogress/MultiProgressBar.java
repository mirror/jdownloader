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

package jd.gui.swing.components.multiprogress;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.border.LineBorder;

public class MultiProgressBar extends JPanel {

    private static final long         serialVersionUID = -7489358722203326526L;
    private final List<ProgressEntry> entries;
    private long                      maximum;
    private long                      value;

    public MultiProgressBar() {
        entries = new ArrayList<ProgressEntry>();
        this.setBackground(getBackground().darker());
        LineBorder b = new LineBorder(getBackground().brighter(), 1, true);
        this.setBorder(b);
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(100, 30);
    }

    private int scale(long point, double faktor) {
        return (int) Math.ceil(point / faktor);
    }

    private double getFaktor() {
        return maximum / (double) Math.max(1, (getWidth()));
    }

    @Override
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
        long pvalue;
        for (int i = 0; i < entries.size(); i++) {
            e = entries.get(i);
            Color col = new Color(50, 255 - ((205 / (entries.size() + 1)) * i), 50);
            if (e.getValue() >= 0) {
                pvalue = e.getValue();
            } else {
                pvalue = e.getMaximum();
            }
            Rectangle rec = new Rectangle(scale(e.getPosition(), faktor), 0, scale(pvalue, faktor), height);
            if (e.getValue() < 0) {
                ((Graphics2D) g).setPaint(Color.RED);
            } else {
                ((Graphics2D) g).setPaint(new GradientPaint(width / 2, 0, col, width / 2, height, col2.darker()));
            }
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
            entries.clear();
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
