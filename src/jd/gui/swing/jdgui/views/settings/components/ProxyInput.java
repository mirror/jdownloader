package jd.gui.swing.jdgui.views.settings.components;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.net.MalformedURLException;
import java.net.URL;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JTextField;

import org.appwork.scheduler.DelayedRunnable;
import org.appwork.storage.config.ValidationException;
import org.appwork.storage.config.events.GenericConfigEventListener;
import org.appwork.storage.config.handler.KeyHandler;
import org.appwork.swing.MigPanel;
import org.appwork.swing.components.ExtTextField;
import org.appwork.swing.components.tooltips.ToolTipController;
import org.appwork.utils.net.httpconnection.HTTPProxyStorable;
import org.appwork.utils.net.httpconnection.HTTPProxyStorable.TYPE;
import org.appwork.utils.swing.EDTRunner;
import org.jdownloader.gui.translate._GUI;

public class ProxyInput extends MigPanel implements SettingsComponent, GenericConfigEventListener<HTTPProxyStorable>, ActionListener, FocusListener {

    private static final long                  serialVersionUID = -1580999899097054630L;

    private StateUpdateEventSender<ProxyInput> eventSender;
    private boolean                            setting;
    private KeyHandler<HTTPProxyStorable>      keyHandler;
    {
        eventSender = new StateUpdateEventSender<ProxyInput>();

    }
    private final String[]                     types            = new String[] { _GUI._.jd_gui_swing_dialog_ProxyDialog_http(), _GUI._.jd_gui_swing_dialog_ProxyDialog_socks5(), _GUI._.jd_gui_swing_dialog_ProxyDialog_socks4(), _GUI._.jd_gui_swing_dialog_ProxyDialog_direct() };

    private JComboBox                          cmbType;

    private JLabel                             lblHost;

    private ExtTextField                       txtHost;

    private DelayedRunnable                    delayer;

    private JLabel                             lblPort;

    private JTextField                         txtPort;

    private JLabel                             lblUser;

    private JTextField                         txtUser;

    private JLabel                             lblPass;

    private JTextField                         txtPass;

    private JLabel                             proxy;

    public ProxyInput(KeyHandler<HTTPProxyStorable> handler) {
        super("ins 0, wrap 4", "[][grow 10,fill][][grow 3,fill]", "[]");
        this.keyHandler = handler;
        setOpaque(false);
        add(proxy = new JLabel(_GUI._.jd_gui_swing_dialog_ProxyDialog_type()));
        add(cmbType = new JComboBox(types), "spanx");
        cmbType.addActionListener(this);
        add(lblHost = new JLabel(_GUI._.jd_gui_swing_dialog_ProxyDialog_hostport()));
        add(txtHost = new ExtTextField() {
            @Override
            public void onChanged() {

                delayer.resetAndStart();

            }

        });

        this.delayer = new DelayedRunnable(ToolTipController.EXECUTER, 2000) {

            @Override
            public String getID() {
                return "ProxyDialog";
            }

            @Override
            public void delayedrun() {
                new EDTRunner() {

                    @Override
                    protected void runInEDT() {
                        set(txtHost.getText());

                    }

                };

            }

        };

        add(lblPort = new JLabel(":"));
        add(txtPort = new JTextField(), "shrinkx");
        txtPort.setText("8080");

        add(lblUser = new JLabel(_GUI._.jd_gui_swing_dialog_ProxyDialog_username()));
        add(txtUser = new JTextField(), "spanx");

        add(lblPass = new JLabel(_GUI._.jd_gui_swing_dialog_ProxyDialog_password()));
        add(txtPass = new JTextField(), "spanx");
        txtPass.addActionListener(this);
        txtPass.addFocusListener(this);

        txtHost.addActionListener(this);
        txtHost.addFocusListener(this);
        txtPort.addActionListener(this);
        txtPort.addFocusListener(this);
        txtUser.addActionListener(this);
        txtUser.addFocusListener(this);

        cmbType.addActionListener(this);
        setValue(keyHandler.getValue());

    }

    protected void set(final String text) {

        int carPos = txtHost.getCaretPosition();
        String myText = text;
        if (myText == null) myText = "";
        if (myText.endsWith(":")) return;
        for (int i = 0; i < 2; i++) {
            try {
                URL url = new URL(myText);
                txtHost.setText(url.getHost());
                if (url.getPort() > 0) {
                    txtPort.setText(url.getPort() + "");
                }
                String userInfo = url.getUserInfo();
                if (userInfo != null) {
                    int in = userInfo.indexOf(":");
                    if (in >= 0) {
                        txtUser.setText(userInfo.substring(0, in));
                        txtPass.setText(userInfo.substring(in + 1));
                    } else {
                        txtUser.setText(userInfo);
                    }
                }
                return;
            } catch (MalformedURLException e) {
                if (text.contains(":")) {
                    myText = "http://" + myText;
                }
            }
        }

        txtHost.setCaretPosition(carPos);

    }

    @SuppressWarnings("unchecked")
    public HTTPProxyStorable getValue() {
        return null;
    }

    public void setValue(HTTPProxyStorable selected) {
        if (selected == null) return;
        setting = true;
        try {

            switch (selected.getType()) {
            case DIRECT:
                cmbType.setSelectedIndex(3);
                break;
            case HTTP:
                cmbType.setSelectedIndex(0);
                break;
            case NONE:
                lblHost.setText("");
                txtPass.setText("");
                txtPort.setText("");
                txtUser.setText("");
                return;
            case SOCKS4:
                cmbType.setSelectedIndex(2);
                break;

            case SOCKS5:

                cmbType.setSelectedIndex(1);
                break;

            }
            txtHost.setText(selected.getAddress());
            txtPass.setText(selected.getPassword());
            txtPort.setText((selected.getPort() > 0 ? selected.getPort() : 8080) + "");
            txtUser.setText(selected.getUsername());

        } finally {
            setting = false;
        }
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        lblHost.setEnabled(enabled);
        lblPass.setEnabled(enabled);
        lblPort.setEnabled(enabled);
        lblUser.setEnabled(enabled);
        txtHost.setEnabled(enabled);
        txtPort.setEnabled(enabled);
        txtPass.setEnabled(enabled);
        txtUser.setEnabled(enabled);
        cmbType.setEnabled(enabled);
        proxy.setEnabled(enabled);
    }

    public String getConstraints() {
        return "";
    }

    public boolean isMultiline() {
        return true;
    }

    public void addStateUpdateListener(StateUpdateListener listener) {
        eventSender.addListener(listener);

    }

    @Override
    public void onConfigValidatorError(KeyHandler<HTTPProxyStorable> keyHandler, HTTPProxyStorable invalidValue, ValidationException validateException) {
    }

    @Override
    public void onConfigValueModified(KeyHandler<HTTPProxyStorable> keyHandler, HTTPProxyStorable newValue) {
        setValue(keyHandler.getValue());
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        save();
    }

    private void save() {
        if (setting) return;
        keyHandler.setValue(createStorable());
        setValue(createStorable());
    }

    private HTTPProxyStorable createStorable() {
        HTTPProxyStorable ret = new HTTPProxyStorable();
        ret.setAddress(txtHost.getText());
        try {
            ret.setPort(Integer.parseInt(txtPort.getText()));
        } catch (NumberFormatException e) {

        }
        ret.setPassword(txtPass.getText());
        ret.setUsername(txtUser.getText());
        switch (cmbType.getSelectedIndex()) {
        case 0:
            ret.setType(TYPE.HTTP);
            break;
        case 1:
            ret.setType(TYPE.SOCKS5);
            break;
        case 2:
            ret.setType(TYPE.SOCKS4);
            break;
        case 3:
            ret.setType(TYPE.DIRECT);
            break;
        }

        return ret;
    }

    @Override
    public void focusGained(FocusEvent e) {
    }

    @Override
    public void focusLost(FocusEvent e) {
        save();
    }

}
