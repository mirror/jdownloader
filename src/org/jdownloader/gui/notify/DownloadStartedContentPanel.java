package org.jdownloader.gui.notify;

import java.io.File;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.SwingConstants;

import jd.controlling.downloadcontroller.SingleDownloadController;
import jd.controlling.proxy.ProxyInfo;
import jd.plugins.Account;
import jd.plugins.DownloadLink;

import org.appwork.swing.MigPanel;
import org.appwork.utils.os.CrossSystem;
import org.appwork.utils.swing.SwingUtils;
import org.jdownloader.DomainInfo;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;

public class DownloadStartedContentPanel extends MigPanel {

    private SingleDownloadController downloadController;

    public DownloadStartedContentPanel(SingleDownloadController downloadController) {
        super("ins 3 3 0 3,wrap 2", "[fill][grow,fill]", "[]");
        this.downloadController = downloadController;
        DownloadLink downloadLink = downloadController.getDownloadLink();
        add(createHeaderLabel(_GUI._.lit_filename() + ":"));
        add(new JLabel(new File(downloadLink.getFileOutput()).getName(), downloadLink.getIcon(), SwingConstants.LEFT));
        add(createHeaderLabel(_GUI._.lit_hoster() + ":"));
        add(new JLabel(downloadLink.getDomainInfo().getTld(), downloadLink.getDomainInfo().getFavIcon(), SwingConstants.LEFT));
        Account account = downloadController.getAccount();
        if (account != null) {
            add(createHeaderLabel(_GUI._.lit_account() + ":"));
            add(new JLabel(account.getUser() + "@" + account.getHoster(), DomainInfo.getInstance(account.getHoster()).getFavIcon(), SwingConstants.LEFT));
        }
        ProxyInfo proxy = downloadController.getDownloadLinkCandidate().getProxy();
        if (proxy != null && !proxy.isNone()) {
            add(createHeaderLabel(_GUI._.lit_proxy() + ":"));
            add(new JLabel(proxy.toString(), NewTheme.I().getIcon("proxy", 18), SwingConstants.LEFT));

        }

        add(createHeaderLabel(_GUI._.lit_save_to() + ":"));
        add(new JLabel(new File(downloadLink.getFileOutput()).getParent(), NewTheme.I().getIcon("folder", 18), SwingConstants.LEFT));
        SwingUtils.setOpaque(this, false);
    }

    protected JComponent createHeaderLabel(String label) {
        JLabel lbl = new JLabel(label);
        SwingUtils.toBold(lbl);
        lbl.setEnabled(false);
        lbl.setHorizontalAlignment(SwingConstants.RIGHT);
        return lbl;
    }

    public void onClicked() {
        CrossSystem.showInExplorer(new File(downloadController.getDownloadLink().getFileOutput()));
    }

}
