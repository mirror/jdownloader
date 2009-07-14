package jd.gui.skins.simple.jtattoo.ui.rootpane;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Toolkit;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JRootPane;
import javax.swing.JSeparator;
import javax.swing.UIManager;

import com.jtattoo.plaf.AbstractLookAndFeel;
import com.jtattoo.plaf.BaseRootPaneUI;
import com.jtattoo.plaf.BaseTitlePane;
import com.jtattoo.plaf.ColorHelper;
import com.jtattoo.plaf.DecorationHelper;
import com.jtattoo.plaf.JTattooUtilities;
import com.jtattoo.plaf.acryl.AcrylLookAndFeel;
import com.jtattoo.plaf.acryl.AcrylTitlePane;

/**
 * extended from JTattoo centers Title, smooth icon systemmenubar
 * 
 * @author Coalado
 * 
 */
public class JDAcrylRootPaneUI extends BaseRootPaneUI {

    public BaseTitlePane createTitlePane(JRootPane root) {
        return new JDAcrylTitlePane(root, this);
    }

    class JDAcrylTitlePane extends AcrylTitlePane {

        /**
         * 
         */
        private static final long serialVersionUID = -5491269052062514611L;

        public JDAcrylTitlePane(JRootPane root, BaseRootPaneUI baseTitlePane) {
            super(root, baseTitlePane);
        }

        protected void createMenuBar() {
            menuBar = new JDSystemMenuBar(getFrame());
            if (getWindowDecorationStyle() == BaseRootPaneUI.FRAME) {
                JMenu menu = new JMenu("");

                JMenuItem mi = menu.add(restoreAction);
                int mnemonic = getInt("MetalTitlePane.restoreMnemonic", -1);
                if (mnemonic != -1) {
                    mi.setMnemonic(mnemonic);
                }
                mi = menu.add(iconifyAction);
                mnemonic = getInt("MetalTitlePane.iconifyMnemonic", -1);
                if (mnemonic != -1) {
                    mi.setMnemonic(mnemonic);
                }

                if (DecorationHelper.isFrameStateSupported(Toolkit.getDefaultToolkit(), BaseRootPaneUI.MAXIMIZED_BOTH)) {
                    mi = menu.add(maximizeAction);
                    mnemonic = getInt("MetalTitlePane.maximizeMnemonic", -1);
                    if (mnemonic != -1) {
                        mi.setMnemonic(mnemonic);
                    }
                }
                menu.add(new JSeparator());
                mi = menu.add(closeAction);
                mnemonic = getInt("MetalTitlePane.closeMnemonic", -1);
                if (mnemonic != -1) {
                    mi.setMnemonic(mnemonic);
                }

                menuBar.add(menu);
            }

        }

        int getInt(Object key, int defaultValue) {
            Object value = UIManager.get(key);
            if (value instanceof Integer) { return ((Integer) value).intValue(); }
            if (value instanceof String) {
                try {
                    return Integer.parseInt((String) value);
                } catch (NumberFormatException nfe) {
                }
            }
            return defaultValue;
        }

        protected void installSubcomponents() {
            if (getWindowDecorationStyle() == BaseRootPaneUI.FRAME) {
                createActions();
                createMenuBar();
                createButtons();
                add(menuBar);
                add(iconifyButton);
                add(maxButton);
                add(closeButton);
            } else {
                createActions();
                createButtons();
                add(closeButton);
            }
        }

        public void paintComponent(Graphics g) {
            if (getFrame() != null) {
                setState(DecorationHelper.getExtendedState(getFrame()));
            }

            paintBackground(g);

            boolean leftToRight = isLeftToRight();
            boolean isSelected = (window == null) ? true : JTattooUtilities.isWindowActive(window);
            Color foreground = AbstractLookAndFeel.getWindowInactiveTitleForegroundColor();
            if (isSelected) {
                foreground = AbstractLookAndFeel.getWindowTitleForegroundColor();
            }

            int width = getWidth();
            int height = getHeight();
            int titleWidth = width - buttonsWidth - 4;
            int xOffset = leftToRight ? 4 : width - 4;
            if (getWindowDecorationStyle() == BaseRootPaneUI.FRAME) {
                xOffset += leftToRight ? height : -height;
                titleWidth -= height;
            }

            g.setFont(getFont());
            FontMetrics fm = g.getFontMetrics();
            String frameTitle = JTattooUtilities.getClippedText(getTitle(), fm, titleWidth);
            if (frameTitle != null) {
                int titleLength = fm.stringWidth(frameTitle);
                int yOffset = ((height - fm.getHeight()) / 2) + fm.getAscent() - 1;
                if (!leftToRight) {
                    xOffset -= titleLength;
                }
                if (getWindowDecorationStyle() == BaseRootPaneUI.FRAME) {
                    xOffset = (width - titleLength) / 2;
                }
                if (isSelected) {
                    g.setColor(ColorHelper.darker(AcrylLookAndFeel.getWindowTitleColorDark(), 30));
                    JTattooUtilities.drawString(rootPane, g, frameTitle, xOffset - 1, yOffset - 1);
                    JTattooUtilities.drawString(rootPane, g, frameTitle, xOffset - 1, yOffset + 1);
                    JTattooUtilities.drawString(rootPane, g, frameTitle, xOffset + 1, yOffset - 1);
                    JTattooUtilities.drawString(rootPane, g, frameTitle, xOffset + 1, yOffset + 1);
                    JTattooUtilities.drawString(rootPane, g, frameTitle, xOffset + 1, yOffset + 1);
                }

                g.setColor(foreground);
                JTattooUtilities.drawString(rootPane, g, frameTitle, xOffset, yOffset);
                paintText(g, xOffset, yOffset, frameTitle);
            }

        }
    };

}
