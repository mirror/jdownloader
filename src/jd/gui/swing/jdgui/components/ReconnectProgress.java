package jd.gui.swing.jdgui.components;

import javax.swing.ImageIcon;

import org.jdownloader.gui.IconKey;
import org.jdownloader.images.AbstractIcon;

public class ReconnectProgress extends IconedProcessIndicator {
    /**
	 * 
	 */
    private static final long serialVersionUID = 3717078119913109215L;

    public ReconnectProgress() {
        super(new AbstractIcon(IconKey.ICON_AUTO_RECONNECT, 16));
    }

    public ReconnectProgress(ImageIcon icon) {
        super(icon);
    }

}
