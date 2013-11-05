package org.jdownloader.gui.views.downloads.overviewpanel;

import org.appwork.swing.components.ExtButton;
import org.appwork.utils.images.IconIO;
import org.jdownloader.actions.AppAction;
import org.jdownloader.images.NewTheme;

public class SettingsButton extends ExtButton {

    public SettingsButton(AppAction appAction) {
        super(appAction);
        setRolloverEffectEnabled(true);
        onRollOut();
    }

    public void setBounds(int x, int y, int width, int height) {
        super.setBounds(x + 10, y, width, height);
    }

    /**
     * 
     */

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    protected void onRollOut() {
        setContentAreaFilled(false);
        setIcon(IconIO.getTransparentIcon(NewTheme.I().getImage("brightmix/wrench_8", -1), 0.6f));

    }

    /**
     * 
     */
    protected void onRollOver() {
        setIcon(IconIO.getTransparentIcon(NewTheme.I().getImage("brightmix/wrench_8", -1), 0.3f));

    }
}
