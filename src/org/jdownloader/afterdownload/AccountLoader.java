package org.jdownloader.afterdownload;

import java.awt.Dialog.ModalityType;
import java.io.File;
import java.util.List;

import jd.controlling.AccountController;
import jd.gui.swing.jdgui.JDGui;
import jd.gui.swing.jdgui.views.settings.ConfigurationView;
import jd.gui.swing.jdgui.views.settings.panels.accountmanager.AccountManagerSettings;
import jd.plugins.Account;

import org.appwork.storage.config.JsonConfig;
import org.appwork.uio.CloseReason;
import org.appwork.uio.UIOManager;
import org.appwork.utils.swing.dialog.ConfirmDialog;
import org.appwork.utils.swing.dialog.Dialog;
import org.jdownloader.controlling.FileCreationListener;
import org.jdownloader.controlling.FileCreationManager;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.AbstractIcon;
import org.jdownloader.settings.GraphicalUserInterfaceSettings;

public class AccountLoader implements FileCreationListener {

    public void init() {
        FileCreationManager.getInstance().getEventSender().addListener(this);
    }

    @Override
    public void onNewFile(Object caller, File[] fileList) {
        if (fileList != null) {
            for (File f : fileList) {
                if (f != null && f.getName().equalsIgnoreCase("org.jdownloader.settings.AccountSettings.accounts.ejs")) {
                    ConfirmDialog d = new ConfirmDialog(0, _GUI._.AccountLoader_onNewFile_title(), _GUI._.AccountLoader_onNewFile_msg(f.getAbsolutePath()), new AbstractIcon(IconKey.ICON_PREMIUM, 32), _GUI._.lit_import(), null) {
                        @Override
                        public ModalityType getModalityType() {
                            return ModalityType.MODELESS;
                        }
                    };
                    UIOManager.I().show(null, d);
                    if (d.getCloseReason() == CloseReason.OK) {
                        ConfigurationView.getInstance().setSelectedSubPanel(AccountManagerSettings.class);
                        JsonConfig.create(GraphicalUserInterfaceSettings.class).setConfigViewVisible(true);
                        JDGui.getInstance().setContent(ConfigurationView.getInstance(), true);
                        List<Account> accounts = AccountController.getInstance().importAccounts(f);
                        if (accounts.size() > 0) {
                            Dialog.getInstance().showMessageDialog(_GUI._.AccountLoader_onNewFile_accounts_imported(accounts.size()));

                        } else {
                            Dialog.getInstance().showMessageDialog(_GUI._.AccountLoader_onNewFile_noaccounts());
                        }
                    }
                }
            }
        }
    }
}
