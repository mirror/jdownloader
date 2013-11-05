package jd.gui.swing.jdgui.components;

import javax.swing.ImageIcon;

import org.jdownloader.images.NewTheme;

public class ReconnectProgress extends IconedProcessIndicator {
    /**
	 * 
	 */
    private static final long serialVersionUID = 3717078119913109215L;

    public ReconnectProgress() {
        super(NewTheme.I().getIcon("auto-reconnect", 16));
    }

    public ReconnectProgress(ImageIcon icon) {
        super(icon);
    }

}
