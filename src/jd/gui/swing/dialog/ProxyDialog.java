package jd.gui.swing.dialog;

import java.awt.event.ActionEvent;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.NetworkInterface;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;

import javax.swing.Icon;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;

import jd.controlling.TaskQueue;
import jd.controlling.proxy.AbstractProxySelectorImpl;
import jd.controlling.proxy.PacProxySelectorImpl;
import jd.controlling.proxy.SingleBasicProxySelectorImpl;
import jd.controlling.proxy.SingleDirectGatewaySelector;
import net.miginfocom.swing.MigLayout;

import org.appwork.exceptions.WTFException;
import org.appwork.scheduler.DelayedRunnable;
import org.appwork.storage.config.JsonConfig;
import org.appwork.swing.MigPanel;
import org.appwork.swing.components.ExtTextField;
import org.appwork.swing.components.tooltips.ToolTipController;
import org.appwork.utils.BinaryLogic;
import org.appwork.utils.StringUtils;
import org.appwork.utils.event.queue.QueueAction;
import org.appwork.utils.net.httpconnection.HTTPProxy;
import org.appwork.utils.swing.EDTRunner;
import org.appwork.utils.swing.dialog.AbstractDialog;
import org.appwork.utils.swing.dialog.Dialog;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.components.PseudoCombo;
import org.jdownloader.images.AbstractIcon;
import org.jdownloader.images.NewTheme;
import org.jdownloader.logging.LogController;
import org.jdownloader.updatev2.InternetConnectionSettings;

public class ProxyDialog extends AbstractDialog<AbstractProxySelectorImpl> implements CaretListener {

    private JComboBox       cmbType;
    private PseudoCombo     cmbNetIf;
    private ExtTextField    txtHost;
    private JTextField      txtPort;
    private JTextField      txtUser;
    private JTextField      txtPass;

    private JLabel          lblUser;
    private JLabel          lblPass;
    private JLabel          lblPort;
    private JLabel          lblHost;
    private JLabel          lblNetIf;
    private DelayedRunnable delayer;

    public ProxyDialog() {
        super(0, _GUI.T.jd_gui_swing_dialog_ProxyDialog_title(), new AbstractIcon(IconKey.ICON_PROXY_ROTATE, 32), null, null);

    }

    protected int getPreferredWidth() {
        return super.getPreferredWidth();
    }

    private static class NetIfSelection {
        private final String name;

        public String getName() {
            return name;
        }

        public String getDisplayName() {
            return displayName;
        }

        private final String displayName;

        private NetIfSelection(final String name, final String displayName) {
            this.name = name;
            this.displayName = displayName;
        }

        @Override
        public String toString() {
            if (StringUtils.isNotEmpty(getDisplayName())) {
                return getName() + "|(" + getDisplayName() + ")";
            } else {
                return getName();
            }
        }
    }

    @Override
    public JComponent layoutDialogContent() {
        panel = new MigPanel("ins 0, wrap 4", "[][grow,fill,n:300:n][][grow,fill,n:50:n]", "[]");

        String[] types = null;
        if (JsonConfig.create(InternetConnectionSettings.PATH, InternetConnectionSettings.class).isProxyVoleAutodetectionEnabled()) {
            types = new String[] { _GUI.T.jd_gui_swing_dialog_ProxyDialog_http(), _GUI.T.jd_gui_swing_dialog_ProxyDialog_https(), _GUI.T.jd_gui_swing_dialog_ProxyDialog_socks5(), _GUI.T.jd_gui_swing_dialog_ProxyDialog_socks4(), _GUI.T.jd_gui_swing_dialog_ProxyDialog_direct(), _GUI.T.jd_gui_swing_dialog_ProxyDialog_pac() };
        } else {
            types = new String[] { _GUI.T.jd_gui_swing_dialog_ProxyDialog_http(), _GUI.T.jd_gui_swing_dialog_ProxyDialog_https(), _GUI.T.jd_gui_swing_dialog_ProxyDialog_socks5(), _GUI.T.jd_gui_swing_dialog_ProxyDialog_socks4(), _GUI.T.jd_gui_swing_dialog_ProxyDialog_direct() };

        }
        cmbType = new JComboBox(types);
        cmbType.addActionListener(this);
        //
        lblHost = new JLabel(_GUI.T.jd_gui_swing_dialog_ProxyDialog_hostport());
        txtHost = new ExtTextField() {
            @Override
            public void onChanged() {
                delayer.resetAndStart();

            }

        };
        txtHost.addCaretListener(this);
        //
        lblPort = new JLabel(":");
        txtPort = new JTextField();
        txtPort.setText("8080");
        txtPort.addCaretListener(this);
        //
        lblUser = new JLabel(_GUI.T.jd_gui_swing_dialog_ProxyDialog_username());
        txtUser = new JTextField();
        //
        lblPass = new JLabel(_GUI.T.jd_gui_swing_dialog_ProxyDialog_password());
        txtPass = new JTextField();
        //
        lblNetIf = new JLabel(_GUI.T.jd_gui_swing_dialog_ProxyDialog_netif());

        cmbNetIf = new PseudoCombo<NetIfSelection>(new NetIfSelection[] { new NetIfSelection("-", "") }) {
            @Override
            protected Icon getPopIcon(boolean closed) {
                if (closed) {
                    return NewTheme.I().getIcon(IconKey.ICON_POPDOWNLARGE, -1);
                } else {
                    return NewTheme.I().getIcon(IconKey.ICON_POPUPLARGE, -1);
                }
            }

        };
        cmbNetIf.setPopDown(true);
        cmbNetIf.addActionListener(this);
        //
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
        relayout();
        this.okButton.setEnabled(false);
        set(null);
        TaskQueue.getQueue().add(new QueueAction<Void, RuntimeException>() {

            @Override
            protected Void run() throws RuntimeException {
                try {

                    final Enumeration<NetworkInterface> nets = NetworkInterface.getNetworkInterfaces();
                    final HashSet<NetIfSelection> netIfs = new HashSet<NetIfSelection>();
                    final NetIfSelection first;
                    netIfs.add(first = new NetIfSelection("-", ""));
                    for (final NetworkInterface netint : Collections.list(nets)) {
                        if (netint.isUp() && !netint.isLoopback()) {
                            netIfs.add(new NetIfSelection(netint.getName(), netint.getDisplayName()));
                        }
                    }
                    new EDTRunner() {

                        @Override
                        protected void runInEDT() {
                            cmbNetIf.setValues(netIfs.toArray(new NetIfSelection[0]));
                            cmbNetIf.setSelectedItem(first);
                        }
                    };
                } catch (IOException e) {
                    org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().log(e);
                }
                return null;
            }
        });
        return panel;
    }

    public void dispose() {
        super.dispose();
        delayer.stop();
    }

    protected void set(final String text) {
        int carPos = txtHost.getCaretPosition();
        String myText = text;
        if (myText == null) {
            myText = "";
        }
        if (myText.endsWith(":")) {
            return;
        }
        if (JsonConfig.create(InternetConnectionSettings.PATH, InternetConnectionSettings.class).isProxyVoleAutodetectionEnabled() && myText.startsWith("pac://") && cmbType.getSelectedIndex() == 4) {
            return;
        }
        for (int i = 0; i < 2; i++) {
            try {
                URL url = new URL(myText);
                if (cmbType.getSelectedIndex() == 4) {
                    txtHost.setText(url + "");
                } else {
                    txtHost.setText(url.getHost());
                }
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
                if (text != null && text.contains(":")) {
                    myText = "http://" + myText;
                }
            }
        }
        txtHost.setCaretPosition(carPos);

    }

    @Override
    public void actionPerformed(final ActionEvent e) {
        if (e.getSource() == cmbNetIf && cmbType.getSelectedIndex() == 4) {
            final Object selection = cmbNetIf.getSelectedItem();
            if (selection instanceof NetIfSelection) {
                final NetIfSelection netIfSelection = (NetIfSelection) selection;
                if ("-".equals(netIfSelection.getName())) {
                    txtHost.setText("");
                } else {
                    txtHost.setText(netIfSelection.getName());
                }
            }
        } else if (e.getSource() == cmbType) {
            relayout();
            panel.revalidate();
            panel.repaint();

        } else {
            super.actionPerformed(e);
        }
    }

    private void relayout() {
        panel.removeAll();

        switch (cmbType.getSelectedIndex()) {
        case 0:
            // http
        case 1:
            // https
            panel.setLayout(new MigLayout("ins 0, wrap 4", "[][grow,fill,n:300:n][][grow,fill,n:50:n]", "[]"));
            panel.add(new JLabel(_GUI.T.jd_gui_swing_dialog_ProxyDialog_type()));
            panel.add(cmbType, "spanx");

            panel.add(lblHost);
            panel.add(txtHost);

            panel.add(lblPort, "hidemode 3");
            panel.add(txtPort, "hidemode 3,gapleft 0");

            panel.add(lblUser);
            panel.add(txtUser, "spanx");

            panel.add(lblPass);
            panel.add(txtPass, "spanx");

            lblHost.setText(_GUI.T.jd_gui_swing_dialog_ProxyDialog_hostport());
            if (StringUtils.isEmpty(txtPort.getText())) {
                txtPort.setText("8080");
            }
            break;
        case 2:
            // socks5
            panel.setLayout(new MigLayout("ins 0, wrap 4", "[][grow,fill,n:300:n][][grow,fill,n:50:n]", "[]"));
            panel.add(new JLabel(_GUI.T.jd_gui_swing_dialog_ProxyDialog_type()));
            panel.add(cmbType, "spanx");

            panel.add(lblHost);
            panel.add(txtHost);

            panel.add(lblPort, "hidemode 3");
            panel.add(txtPort, "hidemode 3,gapleft 0");

            panel.add(lblUser);
            panel.add(txtUser, "spanx");

            panel.add(lblPass);
            panel.add(txtPass, "spanx");

            lblHost.setText(_GUI.T.jd_gui_swing_dialog_ProxyDialog_hostport());
            if (StringUtils.isEmpty(txtPort.getText())) {
                txtPort.setText("1080");
            }
            break;
        case 3:
            // socks4
            panel.setLayout(new MigLayout("ins 0, wrap 4", "[][grow,fill,n:300:n][][grow,fill,n:50:n]", "[]"));
            panel.add(new JLabel(_GUI.T.jd_gui_swing_dialog_ProxyDialog_type()));
            panel.add(cmbType, "spanx");

            panel.add(lblHost);
            panel.add(txtHost);

            panel.add(lblPort, "hidemode 3");
            panel.add(txtPort, "hidemode 3,gapleft 0");

            panel.add(lblUser);
            panel.add(txtUser, "spanx");

            lblHost.setText(_GUI.T.jd_gui_swing_dialog_ProxyDialog_hostport());
            if (StringUtils.isEmpty(txtPort.getText())) {
                txtPort.setText("1080");
            }
            break;
        case 4:
            // direct
            panel.setLayout(new MigLayout("ins 0, wrap 2", "[][grow,fill]", "[]"));
            panel.add(new JLabel(_GUI.T.jd_gui_swing_dialog_ProxyDialog_type()));
            panel.add(cmbType, "spanx");

            panel.add(lblHost);
            panel.add(txtHost);

            panel.add(lblNetIf);
            panel.add(cmbNetIf);

            lblHost.setText(_GUI.T.jd_gui_swing_dialog_ProxyDialog_local());
            break;
        case 5:
            // pac
            if (!JsonConfig.create(InternetConnectionSettings.PATH, InternetConnectionSettings.class).isProxyVoleAutodetectionEnabled()) {
                throw new WTFException("Not possible");
            }
            panel.setLayout(new MigLayout("ins 0, wrap 2", "[][grow,fill]", "[]"));
            panel.add(new JLabel(_GUI.T.jd_gui_swing_dialog_ProxyDialog_type()));
            panel.add(cmbType, "spanx");

            panel.add(lblHost);
            panel.add(txtHost);

            panel.add(lblUser);
            panel.add(txtUser, "spanx");
            panel.add(lblPass);
            panel.add(txtPass, "spanx");

            lblHost.setText(_GUI.T.jd_gui_swing_dialog_ProxyDialog_pac_url());
            break;
        default:
            throw new WTFException("Not Possible");
        }
        txtHost.requestFocus();
    }

    @Override
    protected void packed() {
        super.packed();

    }

    @Override
    protected void initFocus(JComponent focus) {
        super.initFocus(focus);
        txtHost.requestFocus();

    }

    /**
     * returns HTTPProxy for given settings
     */
    @Override
    protected AbstractProxySelectorImpl createReturnValue() {
        final int mask = getReturnmask();
        if (BinaryLogic.containsSome(mask, Dialog.RETURN_CLOSED)) {
            return null;
        }
        if (BinaryLogic.containsSome(mask, Dialog.RETURN_CANCEL)) {
            return null;
        }
        try {
            HTTPProxy.TYPE type = null;
            if (cmbType.getSelectedIndex() == 0) {
                type = HTTPProxy.TYPE.HTTP;
            } else if (cmbType.getSelectedIndex() == 1) {
                type = HTTPProxy.TYPE.HTTPS;
            } else if (cmbType.getSelectedIndex() == 2) {
                type = HTTPProxy.TYPE.SOCKS5;
            } else if (cmbType.getSelectedIndex() == 3) {
                type = HTTPProxy.TYPE.SOCKS4;
            } else if (cmbType.getSelectedIndex() == 4) {
                type = HTTPProxy.TYPE.DIRECT;
                return new SingleDirectGatewaySelector(HTTPProxy.parseHTTPProxy("direct://" + txtHost.getText()));
            } else if (cmbType.getSelectedIndex() == 5) {
                return new PacProxySelectorImpl(txtHost.getText(), txtUser.getText(), txtPass.getText());
            } else {
                return null;
            }
            HTTPProxy ret = new HTTPProxy(type, txtHost.getText(), Integer.parseInt(txtPort.getText().trim()));

            ret.setPass(txtPass.getText());
            ret.setUser(txtUser.getText());

            return new SingleBasicProxySelectorImpl(ret);
        } catch (final Throwable e) {
            LogController.CL().log(e);
            return null;
        }
    }

    /**
     * update okayButton enabled status, check if host/port(valid number) or host is given
     */
    public void caretUpdate(CaretEvent e) {
        boolean enable = false;
        try {
            if (cmbType.getSelectedIndex() != 2) {
                if (txtHost.getDocument().getLength() > 0 && txtPort.getDocument().getLength() > 0) {
                    try {
                        int port = Integer.parseInt(txtPort.getText());
                        if (port > 0 && port < 65535) {
                            enable = true;
                        }
                    } catch (final Throwable ee) {
                    }
                }
            } else {
                if (txtHost.getDocument().getLength() > 0) {
                    enable = true;
                }
            }
        } finally {
            this.okButton.setEnabled(enable);
        }
    }

}