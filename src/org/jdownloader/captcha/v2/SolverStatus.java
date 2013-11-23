package org.jdownloader.captcha.v2;

import javax.swing.Icon;

public class SolverStatus {

    private String label;

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public Icon getIcon() {
        return icon;
    }

    public void setIcon(Icon icon) {
        this.icon = icon;
    }

    private Icon icon;

    public SolverStatus(String label, Icon icon) {
        this.label = label;
        this.icon = icon;
    }

}
