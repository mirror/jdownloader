package jd.gui.skins.simple;

import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Window;

import javax.swing.JMenuBar;
import javax.swing.JRootPane;

import jd.gui.skins.simple.startmenu.JDStartMenu;

import org.jvnet.lafwidget.animation.effects.GhostPaintingUtils;
import org.jvnet.substance.SubstanceLookAndFeel;
import org.jvnet.substance.SubstanceRootPaneUI;
import org.jvnet.substance.api.SubstanceColorScheme;
import org.jvnet.substance.api.SubstanceSkin;
import org.jvnet.substance.painter.decoration.DecorationAreaType;
import org.jvnet.substance.utils.SubstanceColorUtilities;
import org.jvnet.substance.utils.SubstanceCoreUtilities;
import org.jvnet.substance.utils.SubstanceTextUtilities;
import org.jvnet.substance.utils.SubstanceTitlePane;

public class JDSubstanceTitlePane extends SubstanceTitlePane {

    private static final long serialVersionUID = -2571143182567635859L;
    private SubstanceColorScheme scheme;

    public JDSubstanceTitlePane(JRootPane root, SubstanceRootPaneUI ui) {
        super(root, ui);
        final JRootPane rootPane = this.getRootPane();        
        SubstanceSkin skin = SubstanceCoreUtilities.getSkin(rootPane);
     scheme = skin.getMainDefaultColorScheme(DecorationAreaType.PRIMARY_TITLE_PANE);
    }

    //@Override
    protected JMenuBar createMenuBar() {
        JMenuBar ret = super.createMenuBar();

        extendMenu();
        return ret;
    }

    private void extendMenu() {
        menuBar.getMenu(0).removeAll();

        JDStartMenu.createMenu(menuBar.getMenu(0));
    }

    private Window getWindow() {
        return this.window;
    }

    public Frame getFrame() {
        Window window = this.getWindow();

        if (window instanceof Frame) { return (Frame) window; }
        return null;
    }

    public void paintComponent(Graphics g) {
        Window w = this.getWindow();
        String theTitle = null;
        if (w instanceof Frame) {
            theTitle = ((Frame) w).getTitle();
            ((Frame) w).setTitle(null);
            super.paintComponent(g);
            ((Frame) w).setTitle(theTitle);
            int xOffset = 0;
            int width = this.getWidth();
            int height = this.getHeight();
            JRootPane rootPane = this.getRootPane();
            if (theTitle != null) {
                Rectangle titleTextRect = this.getTitleTextRectangle();
                FontMetrics fm = rootPane.getFontMetrics(g.getFont());
                
                

                xOffset = width/2-fm.stringWidth(theTitle)/2;
            }

            Graphics2D graphics = (Graphics2D) g.create();
            Font font = SubstanceLookAndFeel.getFontPolicy().getFontSet("Substance", null).getWindowTitleFont();
            graphics.setFont(font);

            // draw the title (if needed)
            if (theTitle != null) {
                FontMetrics fm = rootPane.getFontMetrics(graphics.getFont());
                int yOffset = ((height - fm.getHeight()) / 2) + fm.getAscent();

                SubstanceTextUtilities.paintTextWithDropShadow(this, graphics, SubstanceColorUtilities.getForegroundColor(scheme), theTitle, width, height, xOffset, yOffset);
            }

            GhostPaintingUtils.paintGhostImages(this, graphics);

            // long end = System.nanoTime();
            // System.out.println(end - start);
            graphics.dispose();

            return;
        } else {
            super.paintComponent(g);
            return;
        }

    }

}
