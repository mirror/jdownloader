//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team jdownloader@freenet.de
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program  is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSSee the
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
@SuppressWarnings("serial")
public class ScrollPaneWindow extends BasicWindow {

 
    private JPanel panel;

    /**
     * @param owner
     */
    public ScrollPaneWindow(Object owner) {
        super(owner);
        panel = new JPanel();
        this.setLayout(new BorderLayout());
        panel.setLayout(new GridBagLayout());
        JScrollPane scrollPane = new JScrollPane(panel);
        this.add(scrollPane, BorderLayout.CENTER);
        setSize(200, 200);
        setLocation(0, 0);
        setTitle("new ScrollPaneWindow");
        setVisible(true);
        pack();
        repack();

    }
    /**
     * Fügt relative Threadsafe  an x,y die Kompoente cmp ein
     * @param x
     * @param y
     * @param cmp
     */
    public void setComponent(final int x, final int y, final Component cmp) {
        if(cmp==null)return;
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                panel.add(cmp, getGBC(x, y, 1, 1));
               
            }
        });
    }
    /**
     * Fügt relative Threadsafe  an x,y den text cmp ein
     * @param x
     * @param y
     * @param cmp
     */
    public void setText(final int x, final int y, final Object cmp) {
        if(cmp==null)return;
        // final ScrollPaneWindow _this=this;

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                JTextField tf = new JTextField();
                tf.setText(cmp.toString());
                panel.add(tf, getGBC(x, y, 1, 1));
              ;
            }
        });

    }
    /**
     * Fügt relative Threadsafe  an x,y das Bild img ein
     * @param x
     * @param y
     * @param img 
     */
    public void setImage(final int x, final int y, final Image img) {
        if(img==null)return;
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                panel.add(new ImageComponent(img), getGBC(x, y, 1, 1));

            }
        });
    }

}