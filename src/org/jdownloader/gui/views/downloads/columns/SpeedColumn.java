package org.jdownloader.gui.views.downloads.columns;

import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;

import javax.swing.Icon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;
import javax.swing.SwingConstants;

import jd.controlling.downloadcontroller.DownloadWatchDog;
import jd.controlling.downloadcontroller.SingleDownloadController;
import jd.controlling.packagecontroller.AbstractNode;
import jd.nutils.Formatter;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginForHost;

import org.appwork.storage.config.JsonConfig;
import org.appwork.storage.config.ValidationException;
import org.appwork.storage.config.events.GenericConfigEventListener;
import org.appwork.storage.config.handler.KeyHandler;
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
import org.jdownloader.settings.staticreferences.CFG_GUI;
import org.jdownloader.translate._JDT;

public class SpeedColumn extends ExtTextColumn<AbstractNode> {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    private boolean           warningEnabled;

    public SpeedColumn() {
        super(_GUI._.SpeedColumn_SpeedColumn());
        rendererField.setHorizontalAlignment(SwingConstants.RIGHT);
        warningEnabled = CFG_GUI.PREMIUM_ALERT_SPEED_COLUMN_ENABLED.isEnabled();
        CFG_GUI.PREMIUM_ALERT_SPEED_COLUMN_ENABLED.getEventSender().addListener(new GenericConfigEventListener<Boolean>() {

            @Override
            public void onConfigValueModified(KeyHandler<Boolean> keyHandler, Boolean newValue) {
                warningEnabled = CFG_GUI.PREMIUM_ALERT_SPEED_COLUMN_ENABLED.isEnabled();
            }

            @Override
            public void onConfigValidatorError(KeyHandler<Boolean> keyHandler, Boolean invalidValue, ValidationException validateException) {
            }
        });
    }

    public JPopupMenu createHeaderPopup() {

        final JPopupMenu ret = new JPopupMenu();
        LockColumnWidthAction action;
        ret.add(new JCheckBoxMenuItem(action = new LockColumnWidthAction(this)));

        ret.add(new JCheckBoxMenuItem(new AppAction() {
            {
                setName(_GUI._.literall_premium_alert());
                setSmallIcon(NewTheme.I().getIcon("warning", 16));
                setSelected(JsonConfig.create(GraphicalUserInterfaceSettings.class).isPremiumAlertSpeedColumnEnabled());
            }

            @Override
            public void actionPerformed(ActionEvent e) {
                JsonConfig.create(GraphicalUserInterfaceSettings.class).setPremiumAlertSpeedColumnEnabled(!JsonConfig.create(GraphicalUserInterfaceSettings.class).isPremiumAlertSpeedColumnEnabled());
            }
        }));
        ret.add(new JSeparator());
        return ret;

    }

    @Override
    protected boolean isDefaultResizable() {
        return false;
    }

    @Override
    public boolean isEnabled(AbstractNode obj) {
        return obj.isEnabled();
    }

    @Override
    public int getDefaultWidth() {
        return 130;
    }

    // @Override
    // public int getMaxWidth() {
    //
    // return 80;
    // }

    @Override
    protected Icon getIcon(AbstractNode value) {
        if (isSpeedWarning(value)) { return NewTheme.I().getIcon("warning", 16); }
        return null;
    }

    private boolean isSpeedWarning(AbstractNode value) {
        if (warningEnabled == false) return false;
        if (value instanceof DownloadLink) {
            DownloadLink dl = (DownloadLink) value;
            SingleDownloadController dlc = dl.getDownloadLinkController();
            if (dlc == null || dlc.getAccount() != null) return false;
            PluginForHost plugin = dl.getDefaultPlugin();
            if (plugin == null || !plugin.isPremiumEnabled()) {
                /* no account support yet for this plugin */
                return false;
            }
            long limit = dlc.getConnectionHandler().getLimit();
            if (limit > 0 && limit < 50 * 1024) {
                /* we have an active limit that is smaller than our warn speed */
                return false;
            }
            if (dl.getLinkStatus().hasStatus(LinkStatus.DOWNLOADINTERFACE_IN_PROGRESS) && ((DownloadLink) value).getDownloadCurrent() > 100 * 1024) {
                if (((DownloadLink) value).getDownloadSpeed() < 50 * 1024) { return true; }
            }
        }
        return false;
    }

    public boolean onSingleClick(final MouseEvent e, final AbstractNode obj) {
        if (isSpeedWarning(obj)) {

            try {
                Dialog.getInstance().showDialog(new PremiumInfoDialog((((DownloadLink) obj).getDomainInfo(true)), _GUI._.SpeedColumn_onSingleClick_object_(((DownloadLink) obj).getHost()), "SpeedColumn") {
                    protected String getDescription(DomainInfo info2) {
                        return _GUI._.SpeedColumn_getDescription_object_(info2.getTld());
                    }
                });
            } catch (DialogClosedException e1) {
                e1.printStackTrace();
            } catch (DialogCanceledException e1) {
                e1.printStackTrace();
            }
        }

        return false;
    }

    @Override
    public String getStringValue(AbstractNode value) {
        if (value instanceof DownloadLink) {
            if (((DownloadLink) value).getLinkStatus().hasStatus(LinkStatus.DOWNLOADINTERFACE_IN_PROGRESS)) {
                long speed = 0;
                if ((speed = ((DownloadLink) value).getDownloadSpeed()) > 0) {
                    return Formatter.formatReadable(speed) + "/s";
                } else {
                    return _JDT._.gui_download_create_connection();
                }
            }
        } else if (value instanceof FilePackage) {
            long speed = DownloadWatchDog.getInstance().getDownloadSpeedbyFilePackage((FilePackage) value);
            if (speed >= 0) { return Formatter.formatReadable(speed) + "/s"; }
        }
        return null;
    }
}
