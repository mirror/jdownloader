package org.jdownloader.gui.views.downloads.columns;

import java.awt.Point;
import java.awt.event.MouseEvent;

import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import javax.swing.table.TableColumn;

import jd.controlling.downloadcontroller.DownloadLinkCandidate;
import jd.controlling.downloadcontroller.DownloadLinkCandidateHistory;
import jd.controlling.downloadcontroller.DownloadWatchDog;
import jd.controlling.packagecontroller.AbstractNode;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;

import org.appwork.swing.components.ExtMergedIcon;
import org.appwork.swing.components.tooltips.ExtTooltip;
import org.appwork.swing.components.tooltips.ToolTipController;
import org.appwork.swing.exttable.columns.ExtTextColumn;
import org.appwork.utils.StringUtils;
import org.appwork.utils.images.IconIO;
import org.jdownloader.DomainInfo;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.downloads.columns.candidatetooltip.CandidateTooltip;
import org.jdownloader.images.AbstractIcon;
import org.jdownloader.images.BadgeIcon;

public class CandidateAccountColumn extends ExtTextColumn<AbstractNode> {

    private Icon iconDownload = new AbstractIcon(IconKey.ICON_DOWNLOAD, 20);

    public CandidateAccountColumn() {
        super(_GUI._.CandidateAccountColumn());
    }

    @Override
    public boolean isEnabled(AbstractNode obj) {
        return obj.isEnabled();
    }

    @Override
    public boolean isDefaultVisible() {
        return false;
    }

    @Override
    public int getDefaultWidth() {
        return 200;
    }

    @Override
    public ExtTooltip createToolTip(Point position, AbstractNode obj) {
        return CandidateTooltip.create(position, obj);
    }

    @Override
    public boolean onDoubleClick(final MouseEvent e, final AbstractNode obj) {

        SwingUtilities.invokeLater(new Runnable() {

            public void run() {

                if (obj instanceof DownloadLink) {
                    DownloadLinkCandidateHistory history = DownloadWatchDog.getInstance().getSession().getHistory((DownloadLink) obj);
                    if (history != null) {
                        ToolTipController.getInstance().show(CandidateTooltip.create(e.getPoint(), obj));
                    }
                }

            }

        });

        return true;
    }

    @Override
    public void configureRendererComponent(final AbstractNode value, final boolean isSelected, final boolean hasFocus, final int row, final int column) {

        if (value instanceof DownloadLink) {
            DownloadLink dl = (DownloadLink) value;
            DownloadLinkCandidate candidate = dl.getLatestCandidate();
            this.prepareColumn(value);
            configureRenderer(rendererField, rendererIcon, getTableColumn(), candidate);
            return;
        }
        super.configureRendererComponent(value, isSelected, hasFocus, row, column);
    }

    public void configureRenderer(JLabel rendererField, JLabel rendererIcon, TableColumn tableColumn, DownloadLinkCandidate candidate) {

        icon = null;
        String str = null;
        if (candidate != null) {

            Account account = candidate.getCachedAccount().getAccount();
            if (account != null) {

                collectInfo(account);
                if (icon == null) {
                    icon = DomainInfo.getInstance(account.getHoster()).getFavIcon();
                } else {
                    icon = new BadgeIcon(DomainInfo.getInstance(account.getHoster()).getFavIcon(), IconIO.getScaledInstance(icon, 12, 12), 4, 2);
                }
                String accountType = null;
                switch (candidate.getCachedAccount().getType()) {
                case MULTI:
                    accountType = _GUI._.CandidateAccountColumn_account_multi(account.getType().getLabel());
                    break;
                case NONE:
                    break;
                case ORIGINAL:
                    accountType = _GUI._.CandidateAccountColumn_account_original(account.getType().getLabel());
                    break;
                }
                if (!StringUtils.isEmpty(accountType)) {
                    str = _GUI._.CandidateAccountColumn_getStringValue_account_type(account.getUser(), account.getHoster(), accountType);
                } else {
                    str = _GUI._.CandidateAccountColumn_getStringValue_account(account.getUser(), account.getHoster());
                }
            } else {
                icon = iconDownload;
                str = _GUI._.CandidateAccountColumn_getStringValue_free_();
            }
        }
        rendererIcon.setIcon(icon);

        if (str == null) {
            // under substance, setting setText(null) somehow sets the label
            // opaque.
            str = "";
        }

        if (tableColumn != null) {
            try {
                rendererField.setText(org.appwork.sunwrapper.sun.swing.SwingUtilities2Wrapper.clipStringIfNecessary(rendererField, rendererField.getFontMetrics(rendererField.getFont()), str, tableColumn.getWidth() - rendererIcon.getPreferredSize().width - 5));
            } catch (Throwable e) {
                // fallback if org.appwork.swing.sunwrapper.SwingUtilities2 disappears someday
                e.printStackTrace();
                rendererField.setText(str);
            }
        } else {
            rendererField.setText(str);
        }
    }

    private Icon   iconRefresh = new AbstractIcon(IconKey.ICON_REFRESH, 20);
    private Icon   iconWait    = new AbstractIcon(IconKey.ICON_WAIT, 20);
    private Icon   iconOK      = new AbstractIcon(IconKey.ICON_OK, 20);
    private Icon   iconExpired = new ExtMergedIcon(new AbstractIcon(IconKey.ICON_ERROR, 18)).add(new AbstractIcon(IconKey.ICON_WAIT, 12), 6, 6);
    private Icon   iconError   = new AbstractIcon(IconKey.ICON_ERROR, 20);
    private Icon   icon;
    private String status;

    private void collectInfo(Account account) {
        icon = null;

        this.status = null;
        if (account.isChecking()) {
            icon = iconRefresh;
        }
        if (account.getError() == null) {
            icon = iconOK;
            AccountInfo ai = account.getAccountInfo();
            String ret = ai == null ? null : ai.getStatus();
            if (StringUtils.isEmpty(ret)) {
                if (account.isTempDisabled()) {
                    if (StringUtils.isNotEmpty(account.getErrorString())) {
                        status = account.getErrorString();
                        return;
                    }
                    ret = _GUI._.PremiumAccountTableModel_getStringValue_temp_disabled();
                } else {
                    ret = _GUI._.PremiumAccountTableModel_getStringValue_account_ok_();
                }
            } else {
                if (account.isTempDisabled()) {
                    if (StringUtils.isNotEmpty(account.getErrorString())) {

                        icon = iconWait;
                        status = account.getErrorString();
                        return;
                    }
                    ret = _GUI._.PremiumAccountTableModel_getStringValue_temp_disabled2(ret);
                } else {
                    ret = _GUI._.PremiumAccountTableModel_getStringValue_account_ok_2(ret);
                }

            }
            status = ret;
            return;
        }
        if (StringUtils.isNotEmpty(account.getErrorString())) {
            status = account.getErrorString();
            return;
        }
        switch (account.getError()) {
        case EXPIRED:
            icon = iconExpired;
            status = _GUI._.PremiumAccountTableModel_getStringValue_status_expired();
            break;
        case INVALID:
            icon = iconError;
            status = _GUI._.PremiumAccountTableModel_getStringValue_status_invalid();
            break;
        case PLUGIN_ERROR:
            icon = iconError;
            status = _GUI._.PremiumAccountTableModel_getStringValue_status_plugin_error();
            break;
        case TEMP_DISABLED:
            icon = iconWait;
            status = _GUI._.PremiumAccountTableModel_getStringValue_status_unknown_error();
            break;
        default:
            icon = iconError;
            status = _GUI._.PremiumAccountTableModel_getStringValue_status_unknown_error();
            break;
        }
    }

    @Override
    public String getStringValue(AbstractNode value) {
        // unused, because #configureRendererComponent is implemented
        return null;
    }
}
