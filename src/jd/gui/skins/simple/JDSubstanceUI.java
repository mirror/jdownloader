package jd.gui.skins.simple;

import java.awt.Image;

import javax.swing.JComponent;
import javax.swing.JRootPane;

import org.jvnet.substance.SubstanceRootPaneUI;

public class JDSubstanceUI extends SubstanceRootPaneUI {

    private JDSubstanceTitlePane titlePane;
    private Image logo;

    public JDSubstanceUI(Image mainMenuIcon) {
        logo = mainMenuIcon;
    }

    protected JComponent createTitlePane(JRootPane root) {
        return titlePane = new JDSubstanceTitlePane(root, this, logo);
    }

    public void setMainMenuIcon(Image mainMenuIcon) {
        logo = mainMenuIcon;
        titlePane.setLogo(logo);
    }

    public void setToolTipText(String string) {
        titlePane.setToolTipText(string);
    }

}
