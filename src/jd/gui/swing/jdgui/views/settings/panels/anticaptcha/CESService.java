package jd.gui.swing.jdgui.views.settings.panels.anticaptcha;

import javax.swing.ImageIcon;

public interface CESService {

    public ImageIcon getIcon(int i);

    public String getDisplayName();

    public CESGenericConfigPanel createPanel();
}
