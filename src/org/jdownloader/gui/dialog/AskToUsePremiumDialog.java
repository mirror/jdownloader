package org.jdownloader.gui.dialog;

import java.awt.Dialog.ModalityType;

import jd.plugins.PluginForHost;

import org.appwork.uio.UIOManager;
import org.appwork.utils.swing.dialog.ConfirmDialog;
import org.appwork.utils.swing.dialog.Dialog;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.AbstractIcon;

public class AskToUsePremiumDialog extends ConfirmDialog implements AskToUsePremiumDialogInterface {
    
    private final String domain;
    
    public AskToUsePremiumDialog(String domain, PluginForHost plugin) {
        super(Dialog.STYLE_SHOW_DO_NOT_DISPLAY_AGAIN | UIOManager.LOGIC_DONT_SHOW_AGAIN_IGNORES_OK, _GUI._.PluginForHost_showFreeDialog_title(domain), _GUI._.PluginForHost_showFreeDialog_message(domain), new AbstractIcon(IconKey.ICON_PREMIUM, 32), _GUI._.lit_yes(), _GUI._.lit_no());
        // setTimeout(5 * 60 * 1000);
        this.domain = domain;
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