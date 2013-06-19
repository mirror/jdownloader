package org.jdownloader.gui.views.downloads.columns;

import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;

import jd.controlling.packagecontroller.AbstractNode;
import jd.controlling.proxy.ProxyBlock;
import jd.controlling.proxy.ProxyController;
import jd.nutils.Formatter;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginForHost;
import jd.plugins.PluginProgress;

import org.appwork.storage.config.JsonConfig;
import org.appwork.swing.exttable.columnmenu.LockColumnWidthAction;
import org.appwork.swing.exttable.columns.ExtTextColumn;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.jdownloader.DomainInfo;
import org.jdownloader.actions.AppAction;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;
import org.jdownloader.premium.PremiumInfoDialog;
import org.jdownloader.settings.GraphicalUserInterfaceSettings;
import org.jdownloader.translate._JDT;

public class TaskColumn extends ExtTextColumn<AbstractNode> {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    private ImageIcon         trueIcon;
    private ImageIcon         falseIcon;
    private ImageIcon         infoIcon;

    private ImageIcon         iconWait;

    @Override
    public int getDefaultWidth() {
        return 180;
    }

    public boolean isPaintWidthLockIcon() {
        return false;
    }

    @Override
    public boolean isEnabled(AbstractNode obj) {
        return obj.isEnabled();
    }

    public TaskColumn() {
        super(_GUI._.StatusColumn_StatusColumn());
        this.trueIcon = NewTheme.I().getIcon("true", 16);
        this.falseIcon = NewTheme.I().getIcon("false", 16);
        this.infoIcon = NewTheme.I().getIcon("info", 16);

        this.iconWait = NewTheme.I().getIcon("wait", 16);

    }

    @Override
    public JPopupMenu createHeaderPopup() {

        final JPopupMenu ret = new JPopupMenu();
        LockColumnWidthAction action;
        ret.add(new JCheckBoxMenuItem(action = new LockColumnWidthAction(this)));

        ret.add(new JCheckBoxMenuItem(new AppAction() {
            {
                setName(_GUI._.literall_premium_alert());
                setSmallIcon(iconWait);
                setSelected(JsonConfig.create(GraphicalUserInterfaceSettings.class).isPremiumAlertTaskColumnEnabled());
            }

            @Override
            public void actionPerformed(ActionEvent e) {
                JsonConfig.create(GraphicalUserInterfaceSettings.class).setPremiumAlertTaskColumnEnabled(!JsonConfig.create(GraphicalUserInterfaceSettings.class).isPremiumAlertTaskColumnEnabled());
            }
        }));
        ret.add(new JSeparator());
        return ret;

    }

    public boolean onSingleClick(final MouseEvent e, final AbstractNode value) {
        if (value instanceof DownloadLink) {
            DownloadLink dl = (DownloadLink) value;
            if (JsonConfig.create(GraphicalUserInterfaceSettings.class).isPremiumAlertTaskColumnEnabled()) {
                if (!dl.getLinkStatus().isPluginActive() && dl.isEnabled() && dl.getLivePlugin() == null) {
                    if (!dl.getLinkStatus().hasStatus(LinkStatus.TEMP_IGNORE) && !dl.getLinkStatus().isFinished()) {
                        PluginForHost plugin = dl.getDefaultPlugin();
                        if (plugin == null || !plugin.isPremiumEnabled()) {
                            /* no account support yet for this plugin */
                            return false;
                        }
                        /* enabled links that are not running */
                        ProxyBlock timeout = null;
                        if ((timeout = ProxyController.getInstance().getHostIPBlockTimeout(dl.getHost())) != null) {
                            if (timeout.getLink() != value) {
                                try {
                                    Dialog.getInstance().showDialog(new PremiumInfoDialog(DomainInfo.getInstance(((DownloadLink) value).getHost()), _GUI._.TaskColumn_onSingleClick_object_(((DownloadLink) value).getHost()), "TaskColumnReconnect") {
                                        protected String getDescription(DomainInfo info2) {
                                            return _GUI._.TaskColumn_getDescription_object_(info2.getTld());
                                        }
                                    });
                                } catch (DialogClosedException e1) {
                                    e1.printStackTrace();
                                } catch (DialogCanceledException e1) {
                                    e1.printStackTrace();
                                }
                                return true;
                            }
                        }

                    }
                }
            }
        }
        return false;
    }

    @Override
    protected Icon getIcon(AbstractNode value) {
        if (value instanceof DownloadLink) {
            DownloadLink dl = (DownloadLink) value;
            LinkStatus ls = dl.getLinkStatus();
            PluginProgress prog = dl.getPluginProgress();
            ImageIcon icon = null;
            if (prog != null && prog.getPercent() > 0.0 && prog.getPercent() < 100.0) {
                return prog.getIcon();
            } else if ((icon = ls.getStatusIcon()) != null) {
                return icon;
            } else if (ls.isFinished()) {
                return trueIcon;
            } else if (ls.hasStatus(LinkStatus.TEMP_IGNORE)) {
                return infoIcon;
            } else if (ls.isFailed() || dl.getAvailableStatus() == AvailableStatus.FALSE) { return falseIcon; }

            if (!dl.getLinkStatus().isPluginActive() && dl.isEnabled() && dl.getLivePlugin() == null) {
                if (!dl.getLinkStatus().hasStatus(LinkStatus.TEMP_IGNORE) && !dl.getLinkStatus().isFinished()) {
                    /* enabled links that are not running */
                    ProxyBlock timeout = null;
                    if ((timeout = ProxyController.getInstance().getHostIPBlockTimeout(dl.getHost())) != null) {
                        if (timeout.getLink() == value) {
                            return null;
                        } else {
                            return iconWait;
                        }
                    }

                }
            }
        } else if (value instanceof FilePackage) {
            if (((FilePackage) value).getView().isFinished()) { return trueIcon; }
        }
        return null;
    }

    @Override
    public String getStringValue(AbstractNode value) {
        if (value instanceof DownloadLink) {
            if (((DownloadLink) value).isSkipped()) { return ((DownloadLink) value).getSkipReason().getExplanation(); }
            DownloadLink dl = (DownloadLink) value;
            if (!dl.getLinkStatus().isPluginActive() && dl.isEnabled() && dl.getLivePlugin() == null) {
                if (!dl.getLinkStatus().hasStatus(LinkStatus.TEMP_IGNORE) && !dl.getLinkStatus().isFinished()) {
                    /* enabled links that are not running */
                    ProxyBlock timeout = null;
                    String message = null;
                    if ((timeout = ProxyController.getInstance().getHostIPBlockTimeout(dl.getHost())) != null) {
                        if (timeout.getLink() == value) {
                            if ((message = dl.getLinkStatus().getMessage(true)) != null) return message;
                            return _JDT._.gui_download_waittime_status2(Formatter.formatSeconds(timeout.getBlockedTimeout() / 1000));
                        } else {
                            return _JDT._.gui_downloadlink_hosterwaittime();
                        }
                    }
                    if ((timeout = ProxyController.getInstance().getHostBlockedTimeout(dl.getHost())) != null) {
                        if (timeout.getLink() == value) {
                            if ((message = dl.getLinkStatus().getMessage(true)) != null) return message;
                            return _JDT._.gui_download_waittime_status2(Formatter.formatSeconds(timeout.getBlockedTimeout() / 1000));
                        } else {
                            return _JDT._.gui_downloadlink_hostertempunavail();
                        }
                    }
                }
            }
            return dl.getLinkStatus().getMessage(false);

        } else if (value instanceof FilePackage) {
            if (((FilePackage) value).getView().isFinished()) { return _GUI._.TaskColumn_getStringValue_finished_(); }
            if (((FilePackage) value).getView().getETA() != -1) { return _GUI._.TaskColumn_getStringValue_running_(); }
        }
        return "";
    }
}
