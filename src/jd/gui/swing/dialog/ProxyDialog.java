package jd.gui.swing.dialog;

import java.awt.event.ActionEvent;
import java.net.MalformedURLException;
import java.net.URL;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;

import jd.controlling.ClipboardMonitoring;
import jd.controlling.ClipboardMonitoring.ClipboardContent;
import net.miginfocom.swing.MigLayout;

import org.appwork.scheduler.DelayedRunnable;
import org.appwork.swing.components.ExtTextField;
import org.appwork.swing.components.tooltips.ToolTipController;
import org.appwork.utils.BinaryLogic;
import org.appwork.utils.StringUtils;
import org.appwork.utils.net.httpconnection.HTTPProxy;
import org.appwork.utils.swing.EDTRunner;
import org.appwork.utils.swing.dialog.AbstractDialog;
import org.appwork.utils.swing.dialog.Dialog;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;
import org.jdownloader.logging.LogController;

public class ProxyDialog extends AbstractDialog<HTTPProxy> implements CaretListener {

    private JComboBox       cmbType;
    private ExtTextField    txtHost;
    private JTextField      txtPort;
    private JTextField      txtUser;
    private JTextField      txtPass;

    private final String[]  types = new String[] { _GUI._.jd_gui_swing_dialog_ProxyDialog_http(), _GUI._.jd_gui_swing_dialog_ProxyDialog_socks5(), _GUI._.jd_gui_swing_dialog_ProxyDialog_socks4(), _GUI._.jd_gui_swing_dialog_ProxyDialog_direct() };
    private JLabel          lblUser;
    private JLabel          lblPass;
    private JLabel          lblPort;
    private JLabel          lblHost;
    private DelayedRunnable delayer;

    public ProxyDialog() {
        super(0, _GUI._.jd_gui_swing_dialog_ProxyDialog_title(), NewTheme.I().getIcon("proxy_rotate", 32), null, null);

    }

    protected int getPreferredWidth() {
        // TODO Auto-generated method stub
        return 350;
    }

    @Override
    public JComponent layoutDialogContent() {
        JPanel panel = new JPanel(new MigLayout("ins 0, wrap 4", "[][grow 10,fill][][grow 3,fill]"));

        panel.add(new JLabel(_GUI._.jd_gui_swing_dialog_ProxyDialog_type()));
        panel.add(cmbType = new JComboBox(types), "spanx");
        cmbType.addActionListener(this);
        panel.add(lblHost = new JLabel(_GUI._.jd_gui_swing_dialog_ProxyDialog_hostport()));
        panel.add(txtHost = new ExtTextField() {
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
        txtHost.addCaretListener(this);
        panel.add(lblPort = new JLabel(":"));
        panel.add(txtPort = new JTextField(), "shrinkx");
        txtPort.setText("8080");
        txtPort.addCaretListener(this);

        panel.add(lblUser = new JLabel(_GUI._.jd_gui_swing_dialog_ProxyDialog_username()));
        panel.add(txtUser = new JTextField(), "spanx");

        panel.add(lblPass = new JLabel(_GUI._.jd_gui_swing_dialog_ProxyDialog_password()));
        panel.add(txtPass = new JTextField(), "spanx");
        this.okButton.setEnabled(false);
        ClipboardContent content = ClipboardMonitoring.getINSTANCE().getCurrentContent();
        String clipboardTxt = "";
        if (content != null) clipboardTxt = content.getContent();
        set(clipboardTxt);

        return panel;
    }

    public void dispose() {
        super.dispose();
        delayer.stop();
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

    @Override
    public void actionPerformed(final ActionEvent e) {
        if (e.getSource() == cmbType) {

            switch (cmbType.getSelectedIndex()) {
            case 0:
                // http
                txtPass.setVisible(true);
                lblPass.setVisible(true);
                txtPort.setVisible(true);
                lblUser.setVisible(true);
                txtUser.setVisible(true);
                lblPort.setVisible(true);
                if (StringUtils.isEmpty(txtPort.getText())) {
                    txtPort.setText("8080");
                }
                break;
            case 1:
                // socks5
                txtPass.setVisible(true);
                lblPass.setVisible(true);
                txtPort.setVisible(true);
                lblUser.setVisible(true);
                txtUser.setVisible(true);
                lblPort.setVisible(true);
                if (StringUtils.isEmpty(txtPort.getText())) {
                    txtPort.setText("1080");
                }
                break;
            case 2:
                // socks4
                txtPass.setVisible(false);
                lblPass.setVisible(false);
                txtPort.setVisible(true);
                lblUser.setVisible(true);
                txtUser.setVisible(true);
                lblPort.setVisible(true);
                if (StringUtils.isEmpty(txtPort.getText())) {
                    txtPort.setText("1080");
                }
                break;
            case 3:
                // direct
                txtPass.setVisible(false);
                lblPass.setVisible(false);
                txtPort.setVisible(false);
                lblUser.setVisible(false);
                txtUser.setVisible(false);
                lblPort.setVisible(false);
                break;
            default:
                txtPass.setVisible(false);
                lblPass.setVisible(false);
                lblUser.setVisible(true);
                txtUser.setVisible(true);
                lblPort.setVisible(true);
                if (StringUtils.isEmpty(txtPort.getText())) {
                    txtPort.setText("1080");
                }
            }

        } else {
            super.actionPerformed(e);
        }
    }

    /**
     * returns HTTPProxy for given settings
     */
    @Override
    protected HTTPProxy createReturnValue() {
        final int mask = getReturnmask();
        if (BinaryLogic.containsSome(mask, Dialog.RETURN_CLOSED)) return null;
        if (BinaryLogic.containsSome(mask, Dialog.RETURN_CANCEL)) return null;
        try {

            HTTPProxy.TYPE type = null;
            if (cmbType.getSelectedIndex() == 0) {
                type = HTTPProxy.TYPE.HTTP;
            } else if (cmbType.getSelectedIndex() == 1) {
                type = HTTPProxy.TYPE.SOCKS5;
            } else if (cmbType.getSelectedIndex() == 2) {
                type = HTTPProxy.TYPE.SOCKS4;
            } else if (cmbType.getSelectedIndex() == 3) {
                type = HTTPProxy.TYPE.DIRECT;
                return HTTPProxy.parseHTTPProxy("direct://" + txtHost.getText());
            } else {
                return null;
            }
            HTTPProxy ret = new HTTPProxy(type, txtHost.getText(), Integer.parseInt(txtPort.getText().trim()));

            ret.setPass(txtPass.getText());
            ret.setUser(txtUser.getText());

            return ret;
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