package org.jdownloader.gui.toolbar;

import java.awt.event.ActionEvent;

import jd.controlling.downloadcontroller.DownloadWatchDog;
import jd.controlling.proxy.ProxyController;

import org.appwork.utils.StringUtils;
import org.appwork.utils.swing.dialog.Dialog;
import org.jdownloader.controlling.contextmenu.ActionContext;
import org.jdownloader.controlling.contextmenu.CustomizableAppAction;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;

public class UnskipAllSkippedLinksAction extends CustomizableAppAction implements ActionContext {

    public UnskipAllSkippedLinksAction() {
        addContextSetup(this);
        setName(_GUI._.UnskipAllSkippedLinksAction());
        setIconKey(IconKey.ICON_SKIPPED);
    }

    /**
     * @param path
     */
    public void updateIcon(String path) {
        if (StringUtils.equals(ProxyController.getInstance().getLatestProfilePath(), path) && StringUtils.isNotEmpty(path)) {
            setIconKey(IconKey.ICON_PROXY_ROTATE);
        } else {
            setIconKey(IconKey.ICON_PROXY);
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        try {
            DownloadWatchDog.getInstance().unSkipAllSkipped();

        } catch (Throwable e1) {
            Dialog.getInstance().showExceptionDialog(_GUI._.lit_error_occured(), e1.getMessage(), e1);
        } finally {

        }
    }

}
