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

import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JTextArea;

import jd.controlling.TaskQueue;
import jd.gui.swing.jdgui.views.settings.components.Checkbox;
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
import org.jdownloader.images.NewTheme;
import org.jdownloader.settings.staticreferences.CFG_MYJD;

public class MyJDownloaderSettingsPanel extends AbstractConfigPanel implements GenericConfigEventListener<Enum>, MyJDownloaderListener {
    
    private static final long serialVersionUID = 1L;
    private SettingsButton    openMyJDownloader;
    private TextInput         userName;
    private PasswordInput     passWord;
    
    private JTextArea         error;
    private JTextArea         status;
    private JButton           connectButton;
    private AppAction         connectAction;
    private AppAction         disconnectAction;
    private Checkbox          checkBox;
    private AppAction         reconnectAction;
    private ExtButton         disconnectButton;
    private TextInput         deviceName;
    
    public String getTitle() {
        return _GUI._.MyJDownloaderSettingsPanel_MyJDownloaderSettingsPanel_title_();
    }
    
    public MyJDownloaderSettingsPanel() {
        super();
        
        openMyJDownloader = new SettingsButton(new AppAction() {
            {
                setName(_GUI._.MyJDownloaderSettingsPanel_MyJDownloaderSettingsPanel_open_());
                
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
        deviceName = new TextInput(CFG_MYJD.DEVICE_NAME);
        userName = new TextInput(CFG_MYJD.EMAIL);
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
        reconnectAction = new AppAction() {
            {
                setName(_GUI._.MyJDownloaderSettingsPanel_MyJDownloaderSettingsPanel_reconnect_());
            }
            
            @Override
            public void actionPerformed(ActionEvent e) {
                TaskQueue.getQueue().add(new QueueAction<Void, RuntimeException>() {
                    
                    @Override
                    protected Void run() throws RuntimeException {
                        MyJDownloaderController.getInstance().disconnect();
                        MyJDownloaderController.getInstance().connect();
                        return null;
                    }
                });
                
            }
        };
        connectAction = new AppAction() {
            {
                setName(_GUI._.MyJDownloaderSettingsPanel_MyJDownloaderSettingsPanel_connect_());
            }
            
            @Override
            public void actionPerformed(ActionEvent e) {
                TaskQueue.getQueue().add(new QueueAction<Void, RuntimeException>() {
                    
                    @Override
                    protected Void run() throws RuntimeException {
                        MyJDownloaderController.getInstance().connect();
                        return null;
                    }
                });
            }
        };
        disconnectAction = new AppAction() {
            {
                setName(_GUI._.MyJDownloaderSettingsPanel_MyJDownloaderSettingsPanel_disconnect_());
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
        connectButton = new ExtButton(connectAction);
        disconnectButton = new ExtButton(disconnectAction);
        MyJDownloaderController.getInstance().getEventSender().addListener(this, true);
        SwingUtils.toBold(status);
        checkBox = new Checkbox(CFG_MYJD.AUTO_CONNECT_ENABLED);
        checkBox.setToolTipText(_GUI._.MyJDownloaderSettingsPanel_MyJDownloaderSettingsPanel_autoconnect_tt());
        
        CFG_MYJD.LATEST_ERROR.getEventSender().addListener(this, true);
        this.addHeader(getTitle(), NewTheme.I().getIcon("myjdownloader", 32));
        this.addDescription(_GUI._.MyJDownloaderSettingsPanel_MyJDownloaderSettingsPanel_description());
        add(openMyJDownloader, "gapleft 37,spanx,pushx,growx");
        this.addHeader(_GUI._.MyJDownloaderSettingsPanel_MyJDownloaderSettingsPanel_logins_(), NewTheme.I().getIcon(IconKey.ICON_LOGINS, 32));
        // addPair(_GUI._.MyJDownloaderSettingsPanel_MyJDownloaderSettingsPanel_enabled(), null, checkBox);
        this.addDescriptionPlain(_GUI._.MyJDownloaderSettingsPanel_MyJDownloaderSettingsPanel_jd_logins());
        addPair(_GUI._.MyJDownloaderSettingsPanel_MyJDownloaderSettingsPanel_username_(), null, userName);
        addPair(_GUI._.MyJDownloaderSettingsPanel_MyJDownloaderSettingsPanel_password_(), null, passWord);
        
        this.addDescriptionPlain(_GUI._.MyJDownloaderSettingsPanel_MyJDownloaderSettingsPanel_jd_name());
        
        addPair(_GUI._.MyJDownloaderSettingsPanel_MyJDownloaderSettingsPanel_devicename_(), null, deviceName);
        
        MigPanel p = new MigPanel("ins 0 0 0 0", "[grow,fill][][][][]", "[]");
        p.setOpaque(false);
        p.add(status, "wmin 10");
        // add(status, "gaptop 0,spanx,growx,pushx,gapbottom 5,wmin 10");
        // p.add(Box.createHorizontalGlue());
        JLabel lbl;
        p.add(lbl = new JLabel(_GUI._.MyJDownloaderSettingsPanel_MyJDownloaderSettingsPanel_autoconnect_()));
        lbl.setToolTipText(_GUI._.MyJDownloaderSettingsPanel_MyJDownloaderSettingsPanel_autoconnect_tt());
        checkBox.setToolTipText(_GUI._.MyJDownloaderSettingsPanel_MyJDownloaderSettingsPanel_autoconnect_tt());
        
        p.add(checkBox);
        
        p.add(connectButton);
        p.add(disconnectButton);
        add(Box.createHorizontalGlue(), "gapleft 37");
        add(p, "spanx,pushx,growx");
        add(Box.createHorizontalGlue(), "gapleft 37");
        add(error, "gaptop 0,spanx,growx,pushx,gapbottom 5,wmin 10,hidemode 3");
        
    }
    
    @Override
    public ImageIcon getIcon() {
        return NewTheme.I().getIcon("myjdownloader", 32);
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
                        error.setText(_GUI._.MyJDownloaderSettingsPanel_runInEDT_account_unconfirmed_());
                        break;
                    case BAD_LOGINS:
                    case EMAIL_INVALID:
                        error.setVisible(true);
                        error.setText(_GUI._.MyJDownloaderSettingsPanel_runInEDT_account_badlogins());
                        break;
                    case IO:
                    case SERVER_DOWN:
                    case NO_INTERNET_CONNECTION:
                        error.setVisible(true);
                        error.setText(_GUI._.MyJDownloaderSettingsPanel_runInEDT_disconnected_2(latestError.toString()));
                        break;
                    default:
                        error.setVisible(true);
                        error.setText(_GUI._.MyJDownloaderSettingsPanel_runInEDT_account_unknown(latestError.toString()));
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
                if (!MyJDownloaderSettingsPanel.this.isShowing()) return;
                boolean connected = connectionStatus != MyJDownloaderConnectionStatus.UNCONNECTED;
                disconnectAction.setEnabled(connected);
                connectAction.setEnabled(!connected);
                reconnectAction.setEnabled(false);
                
                if (connected) {
                    String cUser = MyJDownloaderController.getInstance().getCurrentEmail();
                    String cPass = MyJDownloaderController.getInstance().getCurrentPassword();
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
                                case IO:
                                case SERVER_DOWN:
                                case NO_INTERNET_CONNECTION:
                                    status.setForeground(Color.YELLOW.darker());
                                    status.setText(_GUI._.MyJDownloaderSettingsPanel_runInEDT_connections(connections));
                                    break;
                                default:
                                    status.setForeground(Color.GREEN.darker());
                                    status.setText(_GUI._.MyJDownloaderSettingsPanel_runInEDT_connected_2() + "\r\n" + _GUI._.MyJDownloaderSettingsPanel_runInEDT_connections(connections));
                                    break;
                            }
                            break;
                        case CONNECTED:
                            status.setForeground(Color.GREEN);
                            status.setText(_GUI._.MyJDownloaderSettingsPanel_runInEDT_connected_2() + "\r\n" + _GUI._.MyJDownloaderSettingsPanel_runInEDT_connections(connections));
                            break;
                    }
                } else {
                    status.setText(_GUI._.MyJDownloaderSettingsPanel_runInEDT_disconnected_());
                    status.setForeground(Color.RED);
                    connectButton.setAction(connectAction);
                }
                
            }
        };
    }
    
}