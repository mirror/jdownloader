package org.jdownloader.gui.views.downloads.context;

import java.awt.event.ActionEvent;

import jd.gui.swing.jdgui.interfaces.ContextMenuAction;
import jd.plugins.DownloadLink;

import org.jdownloader.plugins.controller.host.HostPluginController;

public class CustomSpeed extends ContextMenuAction {

    private static final long serialVersionUID = -5155537516674035401L;
    private DownloadLink      link;

    public CustomSpeed(DownloadLink link) {
        this.link = link;
        init();
    }

    @Override
    protected String getIcon() {
        return "edit";
    }

    @Override
    protected String getName() {
        return "Custom Speed";
    }

    public void actionPerformed(ActionEvent e) {
        HostPluginController.getInstance().init(true);
    }

}
