package jd.gui.skins.simple.config;

import javax.swing.BorderFactory;
import javax.swing.JPanel;

public class JSubPanel extends JPanel {

    private static final long serialVersionUID = 1823383684914263748L;

    public JSubPanel(String subPanelName) {
        this.setName(subPanelName);
        if (subPanelName.startsWith("hide-")) {
            this.setBorder(BorderFactory.createEtchedBorder());
        } else {
            this.setBorder(BorderFactory.createTitledBorder(subPanelName));
        }
    }

}
