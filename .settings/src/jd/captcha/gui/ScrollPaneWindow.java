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

package jd.captcha.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridBagLayout;
import java.awt.Image;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

/**
 * Die Klasse dient als WIndow mit Scrollpane.
 * 
 * @author JD-Team
 */
public class ScrollPaneWindow extends BasicWindow {

    private static final long serialVersionUID = 4174011156785741071L;

    private JPanel panel;

    public ScrollPaneWindow() {
        super();
        this.setAlwaysOnTop(true);
        panel = new JPanel();
        panel.setLayout(new GridBagLayout());
        setLayout(new BorderLayout());
        add(new JScrollPane(panel), BorderLayout.CENTER);
        setSize(200, 200);
        setLocation(0, 0);
        setTitle("new ScrollPaneWindow");
        refreshUI();
        setVisible(true);
    }

    /**
     * Fügt relative Threadsafe an x,y die Kompoente cmp ein
     * 
     * @param x
     * @param y
     * @param cmp
     */
    // @Override
    public void setComponent(final int x, final int y, final Component cmp) {
        if (cmp == null) return;
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                panel.add(cmp, getGBC(x, y, 1, 1));
            }
        });
    }

    /**
     * Fügt relative Threadsafe an x,y das Bild img ein
     * 
     * @param x
     * @param y
     * @param img
     */
    // @Override
    public void setImage(final int x, final int y, final Image img) {
        if (img == null) return;
        setComponent(x, y, new ImageComponent(img));
    }

    /**
     * Fügt relative Threadsafe an x,y den text cmp ein
     * 
     * @param x
     * @param y
     * @param cmp
     */
    // @Override
    public void setText(final int x, final int y, final Object cmp) {
        if (cmp == null) return;
        setComponent(x, y, new JTextField(cmp.toString()));
    }

}