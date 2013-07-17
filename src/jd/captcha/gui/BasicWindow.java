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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import jd.captcha.utils.Utilities;

import org.appwork.utils.swing.WindowManager;
import org.appwork.utils.swing.WindowManager.FrameState;

/**
 * Die Klasse dient als Window Basis Klasse.
 * 
 * @author JD-Team
 */
public class BasicWindow extends JFrame {

    private static final long serialVersionUID = 8474181150357563979L;

    /**
     * Zeigt ein Image in einem neuen Fenster an.
     * 
     * @param img
     * @return Das neue fenster
     */
    public static BasicWindow showImage(Image img) {
        return BasicWindow.showImage(img, img.toString());
    }

    /**
     * Zeigt ein Image in einem neuen Fenster an.
     * 
     * @param img
     * @param title
     * @return Das neue Fenster
     */
    public static BasicWindow showImage(Image img, String title) {
        BasicWindow w = new BasicWindow();
        w.setAlwaysOnTop(true);

        ImageComponent ic = new ImageComponent(img);
        w.setSize(ic.getImageWidth() + 10, ic.getImageHeight() + 20);
        w.setTitle(title);
        w.setLayout(new GridBagLayout());
        w.add(ic, Utilities.getGBC(0, 0, 1, 1));
        WindowManager.getInstance().setVisible(w, true, FrameState.FOCUS);
        w.refreshUI();
        return w;
    }

    /**
     * Erstellt ein einfaches neues GUI Fenster
     */
    public BasicWindow() {
        initWindow();
    }

    /**
     * Gibt das Fenster wieder frei
     */
    public void destroy() {
        setVisible(false);
        dispose();
    }

    /**
     * Gibt die default GridbagConstants zurück
     * 
     * @param x
     * @param y
     * @param width
     * @param height
     * @return Default GridBagConstraints
     */
    protected GridBagConstraints getGBC(int x, int y, int width, int height) {

        GridBagConstraints gbc = Utilities.getGBC(x, y, width, height);
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 1;
        gbc.weightx = 1;

        return gbc;
    }

    /**
     * Initialisiert das Fenster und setzt den WindowClosing Adapter
     */
    private void initWindow() {
        addWindowListener(new WindowAdapter() {
            // @Override
            @Override
            public void windowClosing(WindowEvent event) {
                // Window window = event.getWindow();
                // WindowManager.getInstance().setVisible(window,false,WindowState.FOCUS);
                // window.dispose();
                destroy();
            }
        });

        resizeWindow(100);
        setLocationByScreenPercent(50, 50);
        setBackground(Color.LIGHT_GRAY);
    }

    // @Override
    @Override
    public void pack() {
        try {
            super.pack();
            Dimension screenSize = getToolkit().getScreenSize();

            int newWidth = (int) Math.min(Math.max(this.getSize().width, 300), screenSize.getWidth());
            int newHeight = (int) Math.min(Math.max(this.getSize().height, 300), screenSize.getHeight());
            this.setSize(newWidth, newHeight);
        } catch (Exception e) {

        }
    }

    /**
     * Skaliert alle Komponenten und das fenster neu
     */
    public void refreshUI() {
        pack();
        repack();
    }

    /**
     * packt das fenster neu
     */
    public void repack() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                SwingUtilities.updateComponentTreeUI(BasicWindow.this);
            }
        });
    }

    /**
     * Prozentuales (im bezug aufd en Screen) setzend er größe
     * 
     * @param percent
     *            in screenProzent
     */
    public void resizeWindow(int percent) {
        Dimension screenSize = getToolkit().getScreenSize();
        setSize(screenSize.width * percent / 100, screenSize.height * percent / 100);
    }

    /**
     * Prozentuales Positionsetzen des fensters (Mittelpunkt
     * 
     * @param width
     *            in screenprozent
     * @param height
     *            in ScreenProzent
     */
    public void setLocationByScreenPercent(int width, int height) {
        Dimension screenSize = getToolkit().getScreenSize();

        setLocation((screenSize.width - getSize().width) * width / 100, (screenSize.height - getSize().height) * height / 100);
    }

}