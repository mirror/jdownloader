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

package jd.gui.skins.simple;

import java.awt.Dialog;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Window;

import javax.swing.JMenuBar;
import javax.swing.JRootPane;

import jd.gui.skins.simple.listener.MouseAreaListener;

import org.jvnet.lafwidget.animation.effects.GhostPaintingUtils;
import org.jvnet.substance.SubstanceLookAndFeel;
import org.jvnet.substance.SubstanceRootPaneUI;
import org.jvnet.substance.api.SubstanceColorScheme;
import org.jvnet.substance.api.SubstanceSkin;
import org.jvnet.substance.painter.decoration.DecorationAreaType;
import org.jvnet.substance.painter.decoration.SubstanceDecorationUtilities;
import org.jvnet.substance.utils.SubstanceColorUtilities;
import org.jvnet.substance.utils.SubstanceCoreUtilities;
import org.jvnet.substance.utils.SubstanceTextUtilities;
import org.jvnet.substance.utils.SubstanceTitlePane;

public class JDSubstanceTitlePane extends SubstanceTitlePane {

    private static final long serialVersionUID = -2571143182567635859L;
    // private SubstanceColorScheme scheme;
    private Image logo;
    private MouseAreaListener listener;

    public JDSubstanceTitlePane(JRootPane root, SubstanceRootPaneUI ui, Image logo) {
        super(root, ui);
        this.logo = logo;
        // final JRootPane rootPane = this.getRootPane();

        // SubstanceSkin skin = SubstanceCoreUtilities.getSkin(rootPane);
        // scheme =
        // skin.getMainDefaultColorScheme(DecorationAreaType.PRIMARY_TITLE_PANE);

    }

    // @Override
    protected JMenuBar createMenuBar() {
        // JMenuBar ret = super.createMenuBar();
        //
        // extendMenu();

        return null;
    }

    // private void extendMenu() {
    // menuBar.getMenu(0).removeAll();
    //
    // JDStartMenu.createMenu(menuBar.getMenu(0));
    // }

    private Window getWindow() {
        return this.window;
    }

    public Frame getFrame() {
        Window window = this.getWindow();

        if (window instanceof Frame) { return (Frame) window; }
        return null;
    }

    private String getTitle() {
        Window w = this.getWindow();

        if (w instanceof Frame) { return ((Frame) w).getTitle(); }
        if (w instanceof Dialog) { return ((Dialog) w).getTitle(); }
        return null;
    }

    public void paintComponent(Graphics g) {
        super.paintComponent(g);

        if (!(this.getWindow() instanceof Frame)) return;
        if (this.listener == null) {

            this.addMouseMotionListener(listener = new MouseAreaListener(JDToolBar.LEFTGAP, getHeight() - (48 - JDToolBar.DISPLAY), 48 + JDToolBar.LEFTGAP, getHeight()));
            this.addMouseListener(listener);
        }
        final JRootPane rootPane = this.getRootPane();

        int width = this.getWidth();
        int height = this.getHeight();
        g.clearRect(0, 0, width, height);
        SubstanceSkin skin = SubstanceCoreUtilities.getSkin(rootPane);
        if (skin == null) {
            SubstanceCoreUtilities.traceSubstanceApiUsage(this, "Substance delegate used when Substance is not the current LAF");
        }
        SubstanceColorScheme scheme = skin.getMainDefaultColorScheme(DecorationAreaType.PRIMARY_TITLE_PANE);

        int xOffset = 0;
        String theTitle = this.getTitle();

        if (theTitle != null) {
            // Rectangle titleTextRect = this.getTitleTextRectangle();
            FontMetrics fm = rootPane.getFontMetrics(g.getFont());
            // int titleWidth = titleTextRect.width - 20;
            // String clippedTitle = SubstanceCoreUtilities.clipString(fm,
            // titleWidth, theTitle);
            // // show tooltip with full title only if necessary
            // if (theTitle.equals(clippedTitle)) {
            // this.setToolTipText(null);
            // } else {
            // this.setToolTipText(theTitle);
            // }
            // theTitle = clippedTitle;

            xOffset = width / 2 - fm.stringWidth(theTitle) / 2;

        }

        Graphics2D graphics = (Graphics2D) g.create();
        Font font = SubstanceLookAndFeel.getFontPolicy().getFontSet("Substance", null).getWindowTitleFont();
        graphics.setFont(font);

        SubstanceDecorationUtilities.paintDecorationBackground(graphics, JDSubstanceTitlePane.this, false);

        // draw the title (if needed)
        if (theTitle != null) {
            FontMetrics fm = rootPane.getFontMetrics(graphics.getFont());
            int yOffset = ((height - fm.getHeight()) / 2) + fm.getAscent();

            SubstanceTextUtilities.paintTextWithDropShadow(this, graphics, SubstanceColorUtilities.getForegroundColor(scheme), theTitle, width, height, xOffset, yOffset);
        }

        GhostPaintingUtils.paintGhostImages(this, graphics);

        graphics.drawImage(logo, JDToolBar.LEFTGAP, height - (JDToolBar.IMGSIZE - JDToolBar.DISPLAY), JDToolBar.IMGSIZE + JDToolBar.LEFTGAP, height, 0, 0, JDToolBar.IMGSIZE, JDToolBar.IMGSIZE - JDToolBar.DISPLAY, null);
        // long end = System.nanoTime();
        // System.out.println(end - start);
        graphics.dispose();
    }

    public void setLogo(Image mainMenuIcon) {
        logo = mainMenuIcon;
        this.repaint(0, 0, JDToolBar.IMGSIZE, JDToolBar.IMGSIZE);

    }

}
