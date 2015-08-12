package jd.gui.swing.jdgui.views.myjd.panels;

import javax.swing.Icon;
import javax.swing.JScrollPane;

import jd.gui.swing.jdgui.views.myjd.MyJDownloaderView;

import org.jdownloader.api.myjdownloader.MyJDownloaderConnectionStatus;
import org.jdownloader.api.myjdownloader.MyJDownloaderController;
import org.jdownloader.api.myjdownloader.MyJDownloaderSettings.MyJDownloaderError;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.settings.AbstractConfigPanel;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.AbstractIcon;
import org.jdownloader.settings.staticreferences.CFG_MYJD;

public class MyJDownloaderAccount extends AbstractConfigPanel {
    private ConnectedDevicesTable table;

    public MyJDownloaderAccount() {
        this.addHeader(getTitle(), getIcon());
        this.addDescription(_GUI._.MyJDownloaderSettingsPanel_MyJDownloaderSettingsPanel_description());
        this.addHeader(_GUI._.MyJDownloaderAccount_connected_devices(), new AbstractIcon(IconKey.ICON_DESKTOP, 32));
        // ConnectedDevicesTableContainer container = new ConnectedDevicesTableContainer();
        // add(container);
        add(new JScrollPane(table = new ConnectedDevicesTable()));

    }

    @Override
    public String getTitle() {
        return _GUI._.MyJDownloaderAccount_title();
    }

    @Override
    public Icon getIcon() {
        return new AbstractIcon(IconKey.ICON_BOARD, 32);
    }

    @Override
    protected void onShow() {
        super.onShow();
        if (!isConnected()) {
            MyJDownloaderView.getInstance().setSelectedSubPanel(MyJDownloaderSettingsPanelForTab.class);
        }
    }

    @Override
    public boolean isEnabled() {
        return super.isEnabled() && isConnected();
    }

    private boolean isConnected() {
        final MyJDownloaderConnectionStatus connectionStatus = MyJDownloaderController.getInstance().getConnectionStatus();
        final boolean connected = connectionStatus != MyJDownloaderConnectionStatus.UNCONNECTED;
        if (connected) {
            switch (connectionStatus) {
            case PENDING:
                final MyJDownloaderError latestError = CFG_MYJD.CFG.getLatestError();
                switch (latestError) {
                case IO:
                case SERVER_DOWN:
                case NO_INTERNET_CONNECTION:
                    return false;
                default:
                    return true;
                }

            case CONNECTED:
                return true;
            case UNCONNECTED:
                return false;
            }
        } else {
            return false;
        }
        return false;
    }

    @Override
    public void save() {
    }

    @Override
    public void updateContents() {

    }

}
