package jd.gui.skins.simple;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JMenuBar;
import javax.swing.JPopupMenu;
import javax.swing.Popup;
import javax.swing.UIManager;
import javax.swing.plaf.PopupMenuUI;

import jd.gui.skins.simple.jtattoo.ui.rootpane.JDAcrylRootPaneUI;

import com.jtattoo.plaf.BasePopupMenuUI;

public class JTattooUtils {
    /**
     * Some tweeks to override some LAF features. This is the place where
     * Jtattoo gets a centered title this is also the place, where jtattoo gets
     * a proper scaled Frame icon
     * 
     * 
     *Extend this function to set an updated UI for each JTattoo LAF
     * 
     * @param frame
     */
    public static void setJTattooRootPane(JFrame frame) {

        if (UIManager.getLookAndFeel() instanceof com.jtattoo.plaf.acryl.AcrylLookAndFeel) {
            frame.getRootPane().setUI(new JDAcrylRootPaneUI());
        }

    }

    public static void setJTattooMenuBarUI(JMenuBar ret) {

        //        
        // com.jtattoo.plaf.BasePopupMenuUI
        // UIManager.getDefaults().getUIClass(uiClassID)
        // if (UIManager.getLookAndFeel() instanceof
        // com.jtattoo.plaf.acryl.AcrylLookAndFeel) {
        // ret.setUI(new BasicMenuBarUI() {
        //
        // public void paint(Graphics g, JComponent c) {
        // int w = c.getWidth();
        // int h = c.getHeight();
        // Color[] cols = AbstractLookAndFeel.getTheme().getWindowTitleColors();
        // JTattooUtilities.fillHorGradient(g,new Color[]{cols[cols.length-1]} ,
        // 0, 0, w, h);
        // }
        // });
        //
        // }

    }

    /**
     * creates a popupui bases on jtattoo that shows a blured menu
     * 
     * @return
     */
    public static PopupMenuUI getPopupUI() {
        // TODO Auto-generated method stub
        return new BasePopupMenuUI() {

            private Robot robot;
            private BufferedImage screenImage;

            private Robot getRobot() {
                if (robot == null) {
                    try {
                        robot = new Robot();
                    } catch (Exception ex) {
                    }
                }
                return robot;
            }

            public Popup getPopup(JPopupMenu popupMenu, int x, int y) {

                try {
                    Dimension size = popupMenu.getPreferredSize();
                    Rectangle screenRect = new Rectangle(x, y, size.width, size.height);
                    screenImage = getRobot().createScreenCapture(screenRect);

                    float[] blurMatrix = { 1.0f / 8.0f, 1.0f / 8.0f, 1.0f / 8.0f, 1.0f / 8.0f, 1.0f / 8.0f, 1.0f / 8.0f, 1.0f / 8.0f, 1.0f / 8.0f, 1.0f / 8.0f };
                    BufferedImageOp blurFilter = new ConvolveOp(new Kernel(3, 3, blurMatrix), ConvolveOp.EDGE_NO_OP, null);
                    screenImage = blurFilter.filter(screenImage, null);

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
        };
    }

}
