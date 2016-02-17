package org.jdownloader.gui.views.downloads.overviewpanel;

import org.appwork.swing.components.ExtButton;
import org.appwork.utils.images.IconIO;
import org.jdownloader.actions.AppAction;
import org.jdownloader.gui.IconKey;
import org.jdownloader.images.AbstractIcon;
import org.jdownloader.images.NewTheme;

public class CloseButton extends ExtButton {

    public CloseButton(AppAction appAction) {
        super(appAction);
        setRolloverEffectEnabled(true);
        onRollOut();
        setBorderPainted(false);
        setContentAreaFilled(false);
    }

    public void setBounds(int x, int y, int width, int height) {
        super.setBounds(x + 4, y, width, height);
    }

    protected void onReleased() {
        onRollOut();

    }

    private static final long serialVersionUID = 1L;

    protected void onRollOut() {
        setIcon(new AbstractIcon(IconKey.ICON_CLOSE, 10));

    }

    /**
     *
     */
    protected void onRollOver() {
        setIcon(IconIO.getTransparentIcon(NewTheme.I().getImage("close", 10), 0.5f));

    }
}
