package org.jdownloader.gui.views.downloads.columns;

import java.awt.Point;
import java.awt.event.MouseEvent;
import java.util.List;

import javax.swing.Icon;
import javax.swing.SwingUtilities;

import jd.controlling.downloadcontroller.HistoryEntry;
import jd.controlling.packagecontroller.AbstractNode;
import jd.plugins.Account;
import jd.plugins.DownloadLink;

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
                    List<HistoryEntry> his = ((DownloadLink) obj).getHistory();
                    if (his != null) {
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
            HistoryEntry history = dl.getLatestHistoryEntry();
            if (history != null) {
                Icon icon = history.getAccountIcon(18);
                String str = history.getAccountStatus();
                Account account = history.getAccount();
                if (account != null) {
                    if (icon == null) {
                        icon = DomainInfo.getInstance(account.getHoster()).getFavIcon();
                    } else {
                        icon = new BadgeIcon(DomainInfo.getInstance(account.getHoster()).getFavIcon(), IconIO.getScaledInstance(icon, 12, 12), 4, 2);
                    }
                    String accountType = null;
                    switch (history.getAccountType()) {
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
                    if (icon == null) {
                        icon = dl.getDomainInfo().getFavIcon();
                    } else {
                        icon = new BadgeIcon(dl.getDomainInfo().getFavIcon(), IconIO.getScaledInstance(icon, 12, 12), 4, 2);
                    }

                }

                if (str == null) {
                    // under substance, setting setText(null) somehow sets the label
                    // opaque.
                    str = "";
                }
                rendererIcon.setIcon(icon);
                if (getTableColumn() != null) {
                    try {
                        rendererField.setText(org.appwork.sunwrapper.sun.swing.SwingUtilities2Wrapper.clipStringIfNecessary(rendererField, rendererField.getFontMetrics(rendererField.getFont()), str, getTableColumn().getWidth() - rendererIcon.getPreferredSize().width - 5));
                    } catch (Throwable e) {
                        // fallback if org.appwork.swing.sunwrapper.SwingUtilities2 disappears someday
                        e.printStackTrace();
                        rendererField.setText(str);
                    }
                } else {
                    rendererField.setText(str);
                }
                return;
            }

        }
        super.configureRendererComponent(value, isSelected, hasFocus, row, column);
    }

    @Override
    public String getStringValue(AbstractNode value) {
        // unused, because #configureRendererComponent is implemented
        return null;
    }
}
