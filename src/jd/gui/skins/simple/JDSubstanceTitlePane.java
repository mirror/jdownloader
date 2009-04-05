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
import jd.utils.JDLocale;

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

    public JDSubstanceTitlePane(JRootPane root, SubstanceRootPaneUI ui) {
        super(root, ui);
        // TODO Auto-generated constructor stub
    }

    protected JMenuBar createMenuBar() {

        JMenuBar ret = super.createMenuBar();
 
        extendMenu();
        return ret;
    }

    private void extendMenu() {
        menuBar.getMenu(0).removeAll();

        JDStartMenu.createMenu(menuBar.getMenu(0));

    }
   
}
