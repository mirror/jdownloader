package jd.gui.skins.simple;

import javax.swing.JComponent;
import javax.swing.JRootPane;

import org.jvnet.substance.SubstanceRootPaneUI;
import org.jvnet.substance.utils.SubstanceTitlePane;

public class JDSubstanceUI extends SubstanceRootPaneUI {

    protected JComponent createTitlePane(JRootPane root) {
        return new JDSubstanceTitlePane(root, this);
    }


}
