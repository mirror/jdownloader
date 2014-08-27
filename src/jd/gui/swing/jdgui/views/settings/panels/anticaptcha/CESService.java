package jd.gui.swing.jdgui.views.settings.panels.anticaptcha;

import javax.swing.Icon;

public interface CESService {

    public Icon getIcon(int i);

    public String getDisplayName();

    public CESGenericConfigPanel createPanel();

    public String getDescription();
}
