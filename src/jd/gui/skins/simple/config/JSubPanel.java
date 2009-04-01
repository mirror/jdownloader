package jd.gui.skins.simple.config;

import javax.swing.BorderFactory;

public class JSubPanel extends javax.swing.JPanel {

    public JSubPanel(String subPanelName) {
        this.setName(subPanelName);
        if (subPanelName.startsWith("hide-")) {
            this.setBorder(BorderFactory.createEtchedBorder());
        } else {
            this.setBorder(BorderFactory.createTitledBorder(subPanelName));
        }
    }

}
