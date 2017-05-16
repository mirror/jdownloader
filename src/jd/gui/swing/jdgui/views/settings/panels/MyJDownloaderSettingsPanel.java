//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.
package jd.gui.swing.jdgui.views.settings.panels;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Box;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JTextArea;
import javax.swing.Timer;

import jd.controlling.TaskQueue;
import jd.gui.swing.jdgui.JDGui;
import jd.gui.swing.jdgui.views.myjd.MyJDownloaderView;
import jd.gui.swing.jdgui.views.myjd.panels.MyJDownloaderSettingsPanelForTab;
import jd.gui.swing.jdgui.views.settings.components.PasswordInput;
import jd.gui.swing.jdgui.views.settings.components.SettingsButton;
import jd.gui.swing.jdgui.views.settings.components.TextInput;

import org.appwork.storage.StorageException;
import org.appwork.storage.config.ValidationException;
import org.appwork.storage.config.events.GenericConfigEventListener;
import org.appwork.storage.config.handler.KeyHandler;
import org.appwork.swing.MigPanel;
import org.appwork.swing.components.ExtButton;
import org.appwork.utils.event.queue.QueueAction;
import org.appwork.utils.formatter.TimeFormatter;
import org.appwork.utils.os.CrossSystem;
import org.appwork.utils.swing.EDTRunner;
import org.appwork.utils.swing.SwingUtils;
import org.jdownloader.actions.AppAction;
import org.jdownloader.api.myjdownloader.MyJDownloaderConnectionStatus;
import org.jdownloader.api.myjdownloader.MyJDownloaderController;
import org.jdownloader.api.myjdownloader.MyJDownloaderSettings.MyJDownloaderError;
import org.jdownloader.api.myjdownloader.event.MyJDownloaderListener;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.settings.AbstractConfigPanel;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.AbstractIcon;
import org.jdownloader.images.NewTheme;
import org.jdownloader.settings.staticreferences.CFG_MYJD;

public class MyJDownloaderSettingsPanel extends AbstractConfigPanel implements GenericConfigEventListener<Enum>, MyJDownloaderListener {
    private static final long    serialVersionUID = 1L;
    private final SettingsButton openMyJDownloader;
    private final TextInput      email;
    private final PasswordInput  passWord;
    private final JTextArea      timerText;
    private final JTextArea      errorText;
    private final JTextArea      statusText;
    private final JButton        connectButton;
    private final AppAction      connectAction;
    private final AppAction      disconnectAction;
    private final AppAction      reconnectAction;
    private final ExtButton      disconnectButton;
    private final TextInput      deviceName;
    private final SettingsButton openMyJDownloaderTab;

    public String getTitle() {
        return _GUI.T.MyJDownloaderSettingsPanel_MyJDownloaderSettingsPanel_title_();
    }

    public MyJDownloaderSettingsPanel() {
        super();
        openMyJDownloader = new SettingsButton(new AppAction() {
            {
                setName(_GUI.T.MyJDownloaderSettingsPanel_MyJDownloaderSettingsPanel_open_());
            }

            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    CrossSystem.openURL("http://my.jdownloader.org?referer=JDownloader");
                } catch (StorageException e1) {
                    e1.printStackTrace();
                }
            }
        });
        openMyJDownloaderTab = new SettingsButton(new AppAction() {
            {
                setName(_GUI.T.MyJDownloaderSettingsPanel_MyJDownloaderSettingsPanel_tab_());
            }

            @Override
            public void actionPerformed(ActionEvent e) {
                JDGui.getInstance().setContent(MyJDownloaderView.getInstance(), true);
            }
        });
        deviceName = new TextInput(CFG_MYJD.DEVICE_NAME);
        email = new TextInput(CFG_MYJD.EMAIL);
        passWord = new PasswordInput(CFG_MYJD.PASSWORD);
        GenericConfigEventListener<String> loginsChangeListener = new GenericConfigEventListener<String>() {
            @Override
            public void onConfigValidatorError(KeyHandler<String> keyHandler, String invalidValue, ValidationException validateException) {
            }

            @Override
            public void onConfigValueModified(KeyHandler<String> keyHandler, String newValue) {
                updateContents();
            }
        };
        CFG_MYJD.EMAIL.getEventSender().addListener(loginsChangeListener);
        CFG_MYJD.PASSWORD.getEventSender().addListener(loginsChangeListener);
        CFG_MYJD.DEVICE_NAME.getEventSender().addListener(loginsChangeListener);
        errorText = new JTextArea();
        SwingUtils.setOpaque(errorText, false);
        errorText.setEditable(false);
        errorText.setLineWrap(true);
        errorText.setWrapStyleWord(true);
        errorText.setFocusable(false);
        errorText.setForeground(Color.RED);
        SwingUtils.toBold(errorText);
        statusText = new JTextArea();
        SwingUtils.setOpaque(statusText, false);
        statusText.setEditable(false);
        statusText.setLineWrap(true);
        statusText.setWrapStyleWord(true);
        statusText.setFocusable(false);
        SwingUtils.toBold(statusText);
        timerText = new JTextArea();
        SwingUtils.setOpaque(timerText, false);
        timerText.setEditable(false);
        timerText.setLineWrap(true);
        timerText.setWrapStyleWord(true);
        timerText.setFocusable(false);
        timerText.setForeground(Color.YELLOW.darker());
        SwingUtils.toBold(timerText);
        connectAction = new AppAction() {
            {
                setName(_GUI.T.MyJDownloaderSettingsPanel_MyJDownloaderSettingsPanel_connect_());
            }

            @Override
            public void actionPerformed(ActionEvent e) {
                TaskQueue.getQueue().add(new QueueAction<Void, RuntimeException>() {
                    @Override
                    protected Void run() throws RuntimeException {
                        if (MyJDownloaderController.getInstance().isLoginValid()) {
                            MyJDownloaderController.getInstance().connect();
                        } else {
                            MyJDownloaderController.getInstance().onError(MyJDownloaderError.BAD_LOGINS);
                        }
                        return null;
                    }
                });
            }
        };
        disconnectAction = new AppAction() {
            {
                setName(_GUI.T.MyJDownloaderSettingsPanel_MyJDownloaderSettingsPanel_disconnect_());
            }

            @Override
            public void actionPerformed(ActionEvent e) {
                TaskQueue.getQueue().add(new QueueAction<Void, RuntimeException>() {
                    @Override
                    protected Void run() throws RuntimeException {
                        MyJDownloaderController.getInstance().disconnect();
                        return null;
                    }
                });
            }
        };
        reconnectAction = new AppAction() {
            {
                setName(_GUI.T.MyJDownloaderSettingsPanel_MyJDownloaderSettingsPanel_reconnect_());
            }

            @Override
            public void actionPerformed(final ActionEvent e) {
                TaskQueue.getQueue().add(new QueueAction<Void, RuntimeException>() {
                    @Override
                    protected Void run() throws RuntimeException {
                        disconnectAction.actionPerformed(e);
                        connectAction.actionPerformed(e);
                        return null;
                    }
                });
            }
        };
        connectButton = new ExtButton(connectAction);
        disconnectButton = new ExtButton(disconnectAction);
        MyJDownloaderController.getInstance().getEventSender().addListener(this, true);
        CFG_MYJD.LATEST_ERROR.getEventSender().addListener(this, true);
        this.addHeader(getTitle(), getIcon());
        this.addDescription(_GUI.T.MyJDownloaderSettingsPanel_MyJDownloaderSettingsPanel_description());
        add(openMyJDownloader, "gapleft 37,spanx,pushx,growx");
        if (!(this instanceof MyJDownloaderSettingsPanelForTab)) {
            add(openMyJDownloaderTab, "gapleft 37,spanx,pushx,growx");
        }
        this.addHeader(_GUI.T.MyJDownloaderSettingsPanel_MyJDownloaderSettingsPanel_logins_(), NewTheme.I().getIcon(IconKey.ICON_LOGINS, 32));
        // addPair(_GUI.T.MyJDownloaderSettingsPanel_MyJDownloaderSettingsPanel_enabled(), null, checkBox);
        this.addDescriptionPlain(_GUI.T.MyJDownloaderSettingsPanel_MyJDownloaderSettingsPanel_jd_logins());
        addPair(_GUI.T.MyJDownloaderSettingsPanel_MyJDownloaderSettingsPanel_email_(), null, email);
        addPair(_GUI.T.MyJDownloaderSettingsPanel_MyJDownloaderSettingsPanel_password_(), null, passWord);
        this.addDescriptionPlain(_GUI.T.MyJDownloaderSettingsPanel_MyJDownloaderSettingsPanel_jd_name());
        addPair(_GUI.T.MyJDownloaderSettingsPanel_MyJDownloaderSettingsPanel_devicename_(), null, deviceName);
        MigPanel p = new MigPanel("ins 0 0 0 0", "[grow,fill][][]", "[]");
        p.setOpaque(false);
        p.add(statusText, "wmin 10");
        // add(status, "gaptop 0,spanx,growx,pushx,gapbottom 5,wmin 10");
        // p.add(Box.createHorizontalGlue());
        p.add(connectButton);
        p.add(disconnectButton);
        add(Box.createHorizontalGlue(), "gapleft 37");
        add(p, "spanx,pushx,growx");
        add(Box.createHorizontalGlue(), "gapleft 37");
        add(errorText, "gaptop 0,spanx,growx,pushx,gapbottom 5,wmin 10,hidemode 3");
        add(Box.createHorizontalGlue(), "gapleft 37");
        add(timerText, "gaptop 0,spanx,growx,pushx,gapbottom 5,wmin 10,hidemode 3");
    }

    @Override
    public Icon getIcon() {
        return new AbstractIcon(IconKey.ICON_LOGO_MYJDOWNLOADER, 32);
    }

    @Override
    public void save() {
    }

    private volatile Timer timer = null;

    @Override
    protected void onHide() {
        super.onHide();
        timer = null;
    }

    @Override
    protected void onShow() {
        super.onShow();
        timer = new Timer(1000, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (e.getSource() == timer) {
                    final long retryTimeStamp = MyJDownloaderController.getInstance().getRetryTimeStamp();
                    final long wait = retryTimeStamp - System.currentTimeMillis();
                    if (retryTimeStamp > 0 && wait > 0) {
                        timerText.setVisible(true);
                        timerText.setText("Retry in: " + TimeFormatter.formatMilliSeconds(wait, 0));
                    } else {
                        timerText.setVisible(false);
                    }
                } else {
                    ((Timer) e.getSource()).stop();
                }
            }
        }) {
        };
        timer.start();
    }

    @Override
    public void updateContents() {
        onMyJDownloaderConnectionStatusChanged(MyJDownloaderController.getInstance().getConnectionStatus(), MyJDownloaderController.getInstance().getEstablishedConnections());
        final MyJDownloaderError latestError = CFG_MYJD.CFG.getLatestError();
        new EDTRunner() {
            @Override
            protected void runInEDT() {
                switch (latestError) {
                case NONE:
                    errorText.setVisible(false);
                    break;
                case ACCOUNT_UNCONFIRMED:
                    errorText.setVisible(true);
                    errorText.setForeground(Color.RED);
                    errorText.setText(_GUI.T.MyJDownloaderSettingsPanel_runInEDT_account_unconfirmed_());
                    break;
                case BAD_LOGINS:
                case EMAIL_INVALID:
                    errorText.setVisible(true);
                    errorText.setForeground(Color.RED);
                    errorText.setText(_GUI.T.MyJDownloaderSettingsPanel_runInEDT_account_badlogins());
                    break;
                case SERVER_MAINTENANCE:
                    errorText.setVisible(true);
                    errorText.setForeground(Color.YELLOW.darker());
                    errorText.setText(_GUI.T.MyJDownloaderSettingsPanel_runInEDT_maintenance_());
                    break;
                case SERVER_OVERLOAD:
                    errorText.setVisible(true);
                    errorText.setForeground(Color.YELLOW.darker());
                    errorText.setText(_GUI.T.MyJDownloaderSettingsPanel_runInEDT_disconnected_2(latestError.toString()));
                    break;
                default:
                    errorText.setVisible(true);
                    errorText.setForeground(Color.RED);
                    errorText.setText(_GUI.T.MyJDownloaderSettingsPanel_runInEDT_disconnected_2(latestError.toString()));
                    break;
                }
            }
        };
    }

    @Override
    public void onConfigValidatorError(KeyHandler<Enum> keyHandler, Enum invalidValue, ValidationException validateException) {
    }

    @Override
    public void onConfigValueModified(KeyHandler<Enum> keyHandler, Enum newValue) {
        updateContents();
    }

    @Override
    public void onMyJDownloaderConnectionStatusChanged(final MyJDownloaderConnectionStatus connectionStatus, final int connections) {
        new EDTRunner() {
            @Override
            protected void runInEDT() {
                if (!MyJDownloaderSettingsPanel.this.isShowing()) {
                    return;
                }
                final String cUser = MyJDownloaderController.getInstance().getCurrentEmail();
                final String cPass = MyJDownloaderController.getInstance().getCurrentPassword();
                final long retryTimeStamp = MyJDownloaderController.getInstance().getRetryTimeStamp();
                final boolean connected = connectionStatus != MyJDownloaderConnectionStatus.UNCONNECTED;
                disconnectAction.setEnabled(connected);
                connectAction.setEnabled(!connected);
                reconnectAction.setEnabled(false);
                if (connected) {
                    String cDevice = MyJDownloaderController.getInstance().getCurrentDeviceName();
                    connectButton.setAction(connectAction);
                    if (MyJDownloaderController.validateLogins(cUser, cPass)) {
                        if ((cUser == null || !cUser.equals(CFG_MYJD.EMAIL.getValue())) || (cPass == null || !cPass.equals(CFG_MYJD.PASSWORD.getValue())) || (cDevice == null || !cDevice.equals(CFG_MYJD.DEVICE_NAME.getValue()))) {
                            reconnectAction.setEnabled(true);
                            connectButton.setAction(reconnectAction);
                        }
                    }
                    switch (connectionStatus) {
                    case PENDING:
                        MyJDownloaderError latestError = CFG_MYJD.CFG.getLatestError();
                        switch (latestError) {
                        case SERVER_MAINTENANCE:
                        case SERVER_OVERLOAD:
                            statusText.setForeground(Color.YELLOW.darker());
                            statusText.setText(_GUI.T.MyJDownloaderSettingsPanel_runInEDT_connections(connections));
                            break;
                        case IO:
                        case SERVER_DOWN:
                        case NO_INTERNET_CONNECTION:
                            statusText.setForeground(Color.RED);
                            statusText.setText(_GUI.T.MyJDownloaderSettingsPanel_runInEDT_connections(connections));
                            break;
                        default:
                            statusText.setForeground(Color.GREEN.darker());
                            statusText.setText(_GUI.T.MyJDownloaderSettingsPanel_runInEDT_connected_2() + "\r\n" + _GUI.T.MyJDownloaderSettingsPanel_runInEDT_connections(connections));
                            break;
                        }
                        break;
                    case CONNECTED:
                        statusText.setForeground(Color.GREEN);
                        statusText.setText(_GUI.T.MyJDownloaderSettingsPanel_runInEDT_connected_2() + "\r\n" + _GUI.T.MyJDownloaderSettingsPanel_runInEDT_connections(connections));
                        break;
                    }
                } else {
                    statusText.setText(_GUI.T.MyJDownloaderSettingsPanel_runInEDT_disconnected_());
                    statusText.setForeground(Color.RED);
                    connectButton.setAction(connectAction);
                }
            }
        };
    }
}