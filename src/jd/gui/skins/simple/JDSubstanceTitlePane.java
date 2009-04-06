package jd.gui.skins.simple;

import javax.swing.JMenuBar;
import javax.swing.JRootPane;

import jd.gui.skins.simple.startmenu.JDStartMenu;

import org.jvnet.substance.SubstanceRootPaneUI;
import org.jvnet.substance.utils.SubstanceTitlePane;

public class JDSubstanceTitlePane extends SubstanceTitlePane {

    private static final long serialVersionUID = -2571143182567635859L;

    public JDSubstanceTitlePane(JRootPane root, SubstanceRootPaneUI ui) {
        super(root, ui);
    }

    @Override
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
