package org.jdownloader.captcha.v2;

import javax.swing.Icon;

import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;

public class SolverStatus {

    public static final SolverStatus UPLOADING = new SolverStatus(_GUI._.DeathByCaptchaSolver_solveBasicCaptchaChallenge_uploading(), NewTheme.I().getIcon(IconKey.ICON_UPLOAD, 20));
    public static final SolverStatus SOLVING   = new SolverStatus(_GUI._.DeathByCaptchaSolver_solveBasicCaptchaChallenge_solving(), NewTheme.I().getIcon(IconKey.ICON_WAIT, 20));
    public static final SolverStatus UNSOLVED  = new SolverStatus(_GUI._.DeathByCaptchaSolver_solveBasicCaptchaChallenge_failed(), NewTheme.I().getIcon(IconKey.ICON_BAD, 20));

    private String                   label;

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
