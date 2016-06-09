package org.jdownloader.phantomjs.installation;

import javax.swing.Icon;

import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.downloads.columns.ETAColumn;
import org.jdownloader.images.AbstractIcon;
import org.jdownloader.plugins.PluginTaskID;

import jd.plugins.PluginProgress;

public class InstallProgress extends PluginProgress {

    @Override
    public PluginTaskID getID() {
        return PluginTaskID.PHANTOMJS_INSTALLATION;
    }

    public InstallProgress() {
        super(0, 100, null);
        setIcon(new AbstractIcon(IconKey.ICON_LOGO_PHANTOMJS_LOGO, 18));

    }

    @Override
    public Icon getIcon(Object requestor) {
        if (requestor instanceof ETAColumn) {
            return null;
        }
        return super.getIcon(requestor);
    }

    @Override
    public String getMessage(Object requestor) {
        if (requestor instanceof ETAColumn) {
            return "";
        }
        return _GUI.T.phantomjs_setup_progress();
    }

}
