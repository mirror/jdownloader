package org.jdownloader.gui.notify.downloads;

import java.io.File;

import jd.controlling.downloadcontroller.SingleDownloadController;
import jd.controlling.proxy.AbstractProxySelectorImpl;
import jd.controlling.proxy.NoProxySelector;
import jd.plugins.Account;
import jd.plugins.DownloadLink;

import org.appwork.utils.os.CrossSystem;
import org.appwork.utils.swing.SwingUtils;
import org.jdownloader.DomainInfo;
import org.jdownloader.gui.notify.AbstractBubbleContentPanel;
import org.jdownloader.gui.notify.gui.CFG_BUBBLE;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;

public class DownloadStartedContentPanel extends AbstractBubbleContentPanel {

    protected SingleDownloadController downloadController;
    private Pair                       filename;
    private Pair                       hoster;
    private Pair                       saveTo;
    private Pair                       proxy;
    private Pair                       account;

    public DownloadStartedContentPanel(SingleDownloadController downloadController) {
        super();
        this.downloadController = downloadController;
        layoutComponents();
        SwingUtils.setOpaque(this, false);
    }

    protected void layoutComponents() {

        DownloadLink downloadLink = downloadController.getDownloadLink();

        if (CFG_BUBBLE.DOWNLOAD_STARTED_BUBBLE_CONTENT_FILENAME_VISIBLE.isEnabled()) {
            filename = addPair(filename, _GUI._.lit_filename() + ":", downloadLink.getIcon());
            filename.setText(new File(downloadLink.getFileOutput()).getName());
        }
        if (CFG_BUBBLE.DOWNLOAD_STARTED_BUBBLE_CONTENT_HOSTER_VISIBLE.isEnabled()) {
            hoster = addPair(hoster, _GUI._.lit_hoster() + ":", downloadLink.getDomainInfo().getFavIcon());
            hoster.setText(downloadLink.getDomainInfo().getTld());
        }
        Account account = downloadController.getAccount();
        if (CFG_BUBBLE.DOWNLOAD_STARTED_BUBBLE_CONTENT_ACCOUNT_VISIBLE.isEnabled()) {

            if (account != null) {
                this.account = addPair(this.account, _GUI._.lit_account() + ":", DomainInfo.getInstance(account.getHoster()).getFavIcon());
                this.account.setText(account.getUser() + "@" + account.getHoster());
            }
        }
        AbstractProxySelectorImpl proxy = downloadController.getDownloadLinkCandidate().getProxySelector();
        if (CFG_BUBBLE.DOWNLOAD_STARTED_BUBBLE_CONTENT_PROXY_VISIBLE.isEnabled()) {
            if (proxy != null && !(proxy instanceof NoProxySelector)) {

                this.proxy = addPair(this.proxy, _GUI._.lit_proxy() + ":", NewTheme.I().getIcon("proxy", 18));
                this.proxy.setText(proxy.toString());
            }
        }
        if (CFG_BUBBLE.DOWNLOAD_STARTED_BUBBLE_CONTENT_SAVE_TO_VISIBLE.isEnabled()) {
            this.saveTo = addPair(saveTo, _GUI._.lit_save_to() + ":", NewTheme.I().getIcon("folder", 18));

            saveTo.setText(new File(downloadLink.getFileOutput()).getParent());
        }

    }

    public void onClicked() {
        CrossSystem.showInExplorer(new File(downloadController.getDownloadLink().getFileOutput()));
    }

    @Override
    public void updateLayout() {
        removeAll();
        layoutComponents();
        revalidate();
        repaint();
    }

}
