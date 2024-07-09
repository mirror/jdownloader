package org.jdownloader.gui.dialog;

import java.awt.Dialog.ModalityType;

import jd.controlling.AccountController;
import jd.plugins.PluginForHost;

import org.appwork.uio.UIOManager;
import org.appwork.utils.swing.dialog.ConfirmDialog;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.AbstractIcon;
import org.jdownloader.plugins.controller.host.LazyHostPlugin;

public class AskToUsePremiumDialog extends ConfirmDialog implements AskToUsePremiumDialogInterface {
    protected final LazyHostPlugin lazyHostPlugin;

    public AskToUsePremiumDialog(PluginForHost plugin) {
        super(UIOManager.LOGIC_COUNTDOWN, _GUI.T.PluginForHost_showFreeDialog_title(plugin.getHost()), _GUI.T.PluginForHost_showFreeDialog_message(plugin.getHost()), new AbstractIcon(IconKey.ICON_PREMIUM, 32), _GUI.T.lit_yes(), _GUI.T.lit_no());
        setTimeout(5 * 60 * 1000);
        this.lazyHostPlugin = plugin.getLazyP();
    }

    @Override
    public ModalityType getModalityType() {
        return ModalityType.MODELESS;
    }

    @Override
    public String getPremiumUrl() {
        return AccountController.buildAfflink(lazyHostPlugin, null, "freedialog");
    }

    @Override
    public String getDomain() {
        return lazyHostPlugin.getHost();
    }
}