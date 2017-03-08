package org.jdownloader.container;

import java.awt.Dialog.ModalityType;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import jd.controlling.AccountController;
import jd.controlling.linkcollector.LinkOrigin;
import jd.controlling.linkcollector.LinkOriginDetails;
import jd.controlling.linkcrawler.CrawledLink;
import jd.gui.swing.jdgui.JDGui;
import jd.gui.swing.jdgui.views.settings.ConfigurationView;
import jd.gui.swing.jdgui.views.settings.panels.accountmanager.AccountManagerSettings;
import jd.plugins.Account;
import jd.plugins.ContainerStatus;
import jd.plugins.PluginsC;

import org.appwork.storage.config.JsonConfig;
import org.appwork.uio.CloseReason;
import org.appwork.uio.UIOManager;
import org.appwork.utils.swing.dialog.ConfirmDialog;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogNoAnswerException;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.AbstractIcon;
import org.jdownloader.settings.GraphicalUserInterfaceSettings;

public class JD2AccountsImport extends PluginsC {
    public JD2AccountsImport() {
        super("JD2 Accountdatabase Import", "file:/.+(org\\.jdownloader\\.settings\\.AccountSettings\\.accounts\\.ejs)$", "$Revision: 21176 $");
    }

    public JD2AccountsImport newPluginInstance() {
        return new JD2AccountsImport();
    }

    @Override
    public ArrayList<CrawledLink> decryptContainer(final CrawledLink source) {
        final LinkOriginDetails origin = source.getOrigin();
        if (origin != null && LinkOrigin.CLIPBOARD.equals(origin.getOrigin())) {
            return null;
        } else {
            return super.decryptContainer(source);
        }
    }

    @Override
    protected boolean isDeleteContainer(CrawledLink link, File file) {
        return false;
    }

    public ContainerStatus callDecryption(final File ejsFile) {
        final ContainerStatus cs = new ContainerStatus(ejsFile);
        cls = new ArrayList<CrawledLink>();
        try {
            final ConfirmDialog d = new ConfirmDialog(0, _GUI.T.AccountLoader_onNewFile_title(), _GUI.T.AccountLoader_onNewFile_msg(ejsFile.getAbsolutePath()), new AbstractIcon(IconKey.ICON_PREMIUM, 32), _GUI.T.lit_import(), null) {
                @Override
                public ModalityType getModalityType() {
                    return ModalityType.MODELESS;
                }
            };
            UIOManager.I().show(null, d);
            d.throwCloseExceptions();
            if (d.getCloseReason() == CloseReason.OK) {
                ConfigurationView.getInstance().setSelectedSubPanel(AccountManagerSettings.class);
                JsonConfig.create(GraphicalUserInterfaceSettings.class).setConfigViewVisible(true);
                JDGui.getInstance().setContent(ConfigurationView.getInstance(), true);
                final List<Account> accounts = AccountController.getInstance().importAccounts(ejsFile);
                cs.setStatus(ContainerStatus.STATUS_FINISHED);
                if (accounts.size() > 0) {
                    Dialog.getInstance().showMessageDialog(_GUI.T.AccountLoader_onNewFile_accounts_imported(accounts.size()));
                } else {
                    Dialog.getInstance().showMessageDialog(_GUI.T.AccountLoader_onNewFile_noaccounts());
                }
            } else {
                cs.setStatus(ContainerStatus.STATUS_ABORT);
            }
            return cs;
        } catch (DialogNoAnswerException e) {
            cs.setStatus(ContainerStatus.STATUS_ABORT);
            return cs;
        } catch (Throwable e) {
            logger.log(e);
            cs.setStatus(ContainerStatus.STATUS_FAILED);
            return cs;
        }
    }

    @Override
    public String[] encrypt(String plain) {
        return null;
    }
}
