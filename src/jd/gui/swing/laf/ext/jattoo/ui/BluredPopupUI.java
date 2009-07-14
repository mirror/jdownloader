package jd.gui.swing.laf.ext.jattoo.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;

import javax.swing.JComponent;
import javax.swing.JPopupMenu;
import javax.swing.Popup;
import javax.swing.UIManager;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicPopupMenuUI;

import jd.nutils.nativeintegration.ScreenDevices;

import com.jhlabs.image.BoxBlurFilter;
import com.jtattoo.plaf.AbstractLookAndFeel;

/**
 * Blured popup ui based on Jtattoo
 * 
 * @author Coalado jd.gui.swing.laf.ext.jattoo.ui.BluredPopupUI
 */
public class BluredPopupUI extends BasicPopupMenuUI {

    private BufferedImage screenImage;

    public static ComponentUI createUI(JComponent x) {
        return new BluredPopupUI();
    }

    public Popup getPopup(JPopupMenu popupMenu, int x, int y) {
        try {
            Dimension size = popupMenu.getPreferredSize();
            Rectangle screenRect = new Rectangle(x, y, size.width, size.height);
            screenImage = ScreenDevices.getScreenShot(screenRect);
            Object blurParameter = UIManager.get("PopupMenu.blurParameter");
            if (blurParameter != null && blurParameter instanceof int[]) {
                BoxBlurFilter blur = new BoxBlurFilter(((int[]) blurParameter)[0], ((int[]) blurParameter)[1], ((int[]) blurParameter)[2]);

                blur.filter(screenImage, screenImage);

            } else {

                BoxBlurFilter blur = new BoxBlurFilter(2, 2, 3);

                blur.filter(screenImage, screenImage);
            }
            // ContrastFilter contrast = new ContrastFilter();
            // contrast.setContrast(1.0f);
            // contrast.setBrightness(1.0f);
            //
            // contrast.filter(screenImage, screenImage);
            //            

        } catch (Exception ex) {
            screenImage = null;
        }

        return super.getPopup(popupMenu, x, y);
    }

    public void update(Graphics g, JComponent c) {
        if (screenImage != null) {
            g.drawImage(screenImage, 0, 0, null);
        } else {
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, c.getWidth(), c.getHeight());
        }
    }

    private PopupMenuListener myPopupListener = null;

    public void installUI(JComponent c) {
        super.installUI(c);
        c.setOpaque(false);
    }

    public void uninstallUI(JComponent c) {
        super.uninstallUI(c);
        c.setOpaque(true);
    }

    public void installListeners() {
        super.installListeners();
        if (!isMenuOpaque()) {
            myPopupListener = new PopupMenuListener() {

                public void popupMenuCanceled(PopupMenuEvent e) {
                }

                public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
                    if (screenImage != null) {
                        // JPopupMenu popup = (JPopupMenu) e.getSource();
                        // JRootPane root = popup.getRootPane();
                        // Point ptPopup = popup.getLocationOnScreen();
                        // Point ptRoot = root.getLocationOnScreen();
                        // Graphics g = popup.getRootPane().getGraphics();
                        // g.drawImage(screenImage, ptPopup.x - ptRoot.x,
                        // ptPopup.y - ptRoot.y, null);
                        resetScreenImage();
                    }
                }

                public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                }
            };
            popupMenu.addPopupMenuListener(myPopupListener);
        }
    }

    public void uninstallListeners() {
        if (!isMenuOpaque()) {
            popupMenu.removePopupMenuListener(myPopupListener);
        }
        super.uninstallListeners();
    }

    private boolean isMenuOpaque() {
        return (AbstractLookAndFeel.getTheme().isMenuOpaque() || (!ScreenDevices.gotRobots()));
    }

    private void resetScreenImage() {
        screenImage = null;
    }

};
