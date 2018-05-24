package org.jdownloader.premium;

import java.awt.event.ActionEvent;

import jd.controlling.AccountController;
import jd.plugins.PluginForHost;

import org.jdownloader.DomainInfo;
import org.jdownloader.actions.AppAction;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.logging.LogController;

public class OpenURLAction extends AppAction {
    /**
     *
     */
    private static final long   serialVersionUID = -7939070621339510855L;
    private final DomainInfo    info;
    private final String        id;
    private final static String NAME             = _GUI.T.OpenURLAction_OpenURLAction_();

    public OpenURLAction(DomainInfo info, String id) {
        super();
        this.id = id;
        this.info = info;
        setName(NAME);
    }

    public void actionPerformed(ActionEvent e) {
        PluginForHost plugin = null;
        try {
            plugin = info.findPlugin();
        } catch (final Throwable e2) {
            LogController.CL().log(e2);
        }
        final String customURL;
        if (plugin == null) {
            customURL = "http://" + info.getTld();
        } else {
            customURL = null;
        }
        AccountController.openAfflink(plugin, customURL, id);
    }
}
