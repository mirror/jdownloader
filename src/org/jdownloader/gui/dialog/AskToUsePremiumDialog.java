package org.jdownloader.gui.dialog;

import java.awt.Dialog.ModalityType;
import java.net.MalformedURLException;
import java.net.URL;

import jd.plugins.PluginForHost;

import org.appwork.utils.os.CrossSystem;
import org.appwork.utils.swing.dialog.ConfirmDialog;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.AbstractIcon;

public final class AskToUsePremiumDialog extends ConfirmDialog implements AskToUsePremiumDialogInterface {

    private String domain;

    public AskToUsePremiumDialog(String domain, PluginForHost plugin) {
        super(0, _GUI._.PluginForHost_showFreeDialog_title(domain), _GUI._.PluginForHost_showFreeDialog_message(domain), new AbstractIcon(IconKey.ICON_PREMIUM, 32), _GUI._.lit_yes(), _GUI._.lit_no());
        // setTimeout(5 * 60 * 1000);
        this.domain = domain;
    }

    @Override
    protected void setReturnmask(boolean b) {
        super.setReturnmask(b);
        if (b) {
            try {
                CrossSystem.openURL(new URL(getPremiumUrl()));
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public ModalityType getModalityType() {
        return ModalityType.MODELESS;
    }

    @Override
    public String getPremiumUrl() {
        return "http://update3.jdownloader.org/jdserv/BuyPremiumInterface/redirect?" + domain + "&freedialog";
    }

    @Override
    public String getDomain() {
        return domain;
    }
}