package org.jdownloader.gui.views.downloads.overviewpanel;

import org.appwork.swing.components.ExtButton;
import org.jdownloader.actions.AppAction;
import org.jdownloader.gui.IconKey;
import org.jdownloader.images.AbstractIcon;

public class SettingsButton extends ExtButton {

    private AbstractIcon on;
    private AbstractIcon off;

    public SettingsButton(AppAction appAction) {
        super(appAction);
        setRolloverEffectEnabled(true);

        on = new AbstractIcon(IconKey.ICON_WRENCH, 10);
        off = new AbstractIcon(IconKey.ICON_WRENCH, 10);
        off.setAlpha(0.3f);
        onRollOut();
    }

    public void setBounds(int x, int y, int width, int height) {
        super.setBounds(x, y, width, height);
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
        setIcon(on);

    }

    /**
     *
     */
    protected void onRollOver() {
        setIcon(off);

    }
}
