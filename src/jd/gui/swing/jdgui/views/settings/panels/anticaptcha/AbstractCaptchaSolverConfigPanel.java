package jd.gui.swing.jdgui.views.settings.panels.anticaptcha;

import net.miginfocom.swing.MigLayout;

import org.jdownloader.gui.settings.AbstractConfigPanel;

public abstract class AbstractCaptchaSolverConfigPanel extends AbstractConfigPanel {
    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    public AbstractCaptchaSolverConfigPanel() {
        super(0);
        setLayout(new MigLayout("ins 0, wrap 2", "[][grow,fill]", "[]"));

    }
}
