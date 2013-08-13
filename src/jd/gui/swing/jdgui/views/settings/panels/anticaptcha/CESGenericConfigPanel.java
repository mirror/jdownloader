package jd.gui.swing.jdgui.views.settings.panels.anticaptcha;

import javax.swing.ImageIcon;

import net.miginfocom.swing.MigLayout;

import org.jdownloader.gui.settings.AbstractConfigPanel;

public abstract class CESGenericConfigPanel extends AbstractConfigPanel {
    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    private CESService        service;

    public CESGenericConfigPanel(CESService service) {
        setLayout(new MigLayout("ins 0, wrap 2", "[][grow,fill]", "[]"));
        this.service = service;
    }

    @Override
    public ImageIcon getIcon() {
        return service.getIcon(32);
    }

    @Override
    public String getTitle() {
        return service.getDisplayName();
    }

}
