package jd.gui.swing.jdgui.views.myjd.panels;

import java.awt.Color;

import javax.swing.Box;
import javax.swing.Icon;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import jd.gui.swing.jdgui.views.myjd.MyJDownloaderView;
import jd.gui.swing.jdgui.views.settings.components.TextInput;

import org.appwork.swing.MigPanel;
import org.appwork.utils.swing.EDTRunner;
import org.appwork.utils.swing.SwingUtils;
import org.jdownloader.api.myjdownloader.MyJDownloaderConnectionStatus;
import org.jdownloader.api.myjdownloader.MyJDownloaderController;
import org.jdownloader.api.myjdownloader.MyJDownloaderSettings.MyJDownloaderError;
import org.jdownloader.api.myjdownloader.event.MyJDownloaderListener;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.settings.AbstractConfigPanel;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.AbstractIcon;
import org.jdownloader.settings.staticreferences.CFG_MYJD;

public class MyJDownloaderAccount extends AbstractConfigPanel implements MyJDownloaderListener {
    private ConnectedDevicesTable table;
    private JTextArea             error;
    private JTextArea             status;

    public MyJDownloaderAccount() {
        this.addHeader(getTitle(), getIcon());
        // this.addDescription(_GUI.T.MyJDownloaderSettingsPanel_MyJDownloaderSettingsPanel_description());

        TextInput deviceName = new TextInput(CFG_MYJD.DEVICE_NAME);
        TextInput email = new TextInput(CFG_MYJD.EMAIL);
        deviceName.setEditable(false);
        email.setEditable(false);
        addPair(_GUI.T.MyJDownloaderSettingsPanel_MyJDownloaderSettingsPanel_email_(), null, email);

        addPair(_GUI.T.MyJDownloaderSettingsPanel_MyJDownloaderSettingsPanel_devicename_(), null, deviceName);

        error = new JTextArea();
        SwingUtils.setOpaque(error, false);
        error.setEditable(false);
        error.setLineWrap(true);
        error.setWrapStyleWord(true);
        error.setFocusable(false);
        error.setForeground(Color.RED);
        SwingUtils.toBold(error);

        status = new JTextArea();
        SwingUtils.setOpaque(status, false);
        status.setEditable(false);
        status.setLineWrap(true);
        status.setWrapStyleWord(true);
        status.setFocusable(false);

        SwingUtils.toBold(error);

        MyJDownloaderController.getInstance().getEventSender().addListener(this, true);
        SwingUtils.toBold(status);

        MigPanel p = new MigPanel("ins 0 0 0 0", "[grow,fill][][]", "[]");
        p.setOpaque(false);
        p.add(status, "wmin 10");

        add(Box.createHorizontalGlue(), "gapleft 37");
        add(p, "spanx,pushx,growx");
        add(Box.createHorizontalGlue(), "gapleft 37");
        add(error, "gaptop 0,spanx,growx,pushx,gapbottom 5,wmin 10,hidemode 3");
        this.addHeader(_GUI.T.MyJDownloaderAccount_connected_devices(), new AbstractIcon(IconKey.ICON_DESKTOP, 32));
        // ConnectedDevicesTableContainer container = new ConnectedDevicesTableContainer();
        // add(container);
        add(new JScrollPane(table = new ConnectedDevicesTable()));

    }

    @Override
    public String getTitle() {
        return _GUI.T.MyJDownloaderAccount_title();
    }

    @Override
    public Icon getIcon() {
        return new AbstractIcon(IconKey.ICON_PROFILE, 32);
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
                case SERVER_MAINTENANCE:
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
        onMyJDownloaderConnectionStatusChanged(MyJDownloaderController.getInstance().getConnectionStatus(), MyJDownloaderController.getInstance().getEstablishedConnections());
        new EDTRunner() {

            @Override
            protected void runInEDT() {
                MyJDownloaderError latestError = CFG_MYJD.CFG.getLatestError();
                switch (latestError) {
                case NONE:
                    error.setVisible(false);
                    break;
                case ACCOUNT_UNCONFIRMED:
                    error.setVisible(true);
                    error.setText(_GUI.T.MyJDownloaderSettingsPanel_runInEDT_account_unconfirmed_());
                    break;
                case BAD_LOGINS:
                case EMAIL_INVALID:
                    error.setVisible(true);
                    error.setText(_GUI.T.MyJDownloaderSettingsPanel_runInEDT_account_badlogins());
                    break;
                case SERVER_MAINTENANCE:
                    error.setVisible(true);
                    error.setText(_GUI.T.MyJDownloaderSettingsPanel_runInEDT_maintenance_());
                    break;
                default:
                    error.setVisible(true);
                    error.setText(_GUI.T.MyJDownloaderSettingsPanel_runInEDT_disconnected_2(latestError.toString()));
                    break;
                }
            }
        };
    }

    @Override
    public void onMyJDownloaderConnectionStatusChanged(final MyJDownloaderConnectionStatus connectionStatus, final int connections) {

        new EDTRunner() {

            @Override
            protected void runInEDT() {
                if (!isShowing()) {
                    return;
                }
                boolean connected = connectionStatus != MyJDownloaderConnectionStatus.UNCONNECTED;

                if (connected) {

                    switch (connectionStatus) {
                    case PENDING:
                        MyJDownloaderError latestError = CFG_MYJD.CFG.getLatestError();
                        switch (latestError) {
                        case IO:
                        case SERVER_DOWN:
                        case SERVER_OVERLOAD:
                        case SERVER_MAINTENANCE:
                        case NO_INTERNET_CONNECTION:
                            status.setForeground(Color.YELLOW.darker());
                            status.setText(_GUI.T.MyJDownloaderSettingsPanel_runInEDT_connections(connections));
                            break;
                        default:
                            status.setForeground(Color.GREEN.darker());
                            status.setText(_GUI.T.MyJDownloaderSettingsPanel_runInEDT_connected_2() + "\r\n" + _GUI.T.MyJDownloaderSettingsPanel_runInEDT_connections(connections));
                            break;
                        }
                        break;
                    case CONNECTED:
                        status.setForeground(Color.GREEN);
                        status.setText(_GUI.T.MyJDownloaderSettingsPanel_runInEDT_connected_2() + "\r\n" + _GUI.T.MyJDownloaderSettingsPanel_runInEDT_connections(connections));
                        break;
                    }
                } else {
                    status.setText(_GUI.T.MyJDownloaderSettingsPanel_runInEDT_disconnected_());
                    status.setForeground(Color.RED);

                }

            }
        };
    }

}
