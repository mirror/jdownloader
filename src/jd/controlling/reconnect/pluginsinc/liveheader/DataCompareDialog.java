package jd.controlling.reconnect.pluginsinc.liveheader;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JSeparator;
import javax.swing.JTextField;

import jd.controlling.reconnect.pluginsinc.liveheader.translate.T;

import org.appwork.swing.MigPanel;
import org.appwork.swing.components.ExtTextArea;
import org.appwork.swing.components.ExtTextField;
import org.appwork.utils.os.CrossSystem;
import org.appwork.utils.swing.dialog.AbstractDialog;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.AbstractIcon;
import org.jdownloader.images.NewTheme;

public class DataCompareDialog extends AbstractDialog<Object> {
    private static final int ICONSIZE = 32;
    private String           hostName;
    private String           manufactor;

    public String getHostName() {
        return txtIP.getText();
    }

    public String getManufactor() {
        return txtManufactor.getText();
    }

    public String getRouterName() {
        return txtName.getText();
    }

    public String getUsername() {
        return txtUser.getText();
    }

    public String getPassword() {
        return txtPassword.getText();
    }

    public String getFirmware() {
        return txtFirmware.getText();
    }

    private String     routerName;
    private String     username;
    private String     password;
    private JTextField txtName;
    private JTextField txtManufactor;
    private JTextField txtFirmware;
    private JTextField txtUser;
    private JTextField txtPassword;
    private String     firmware;
    private JTextField txtIP;
    private boolean    loginsOnly;
    private boolean    noLogins;
    private String     loginDesc;

    public DataCompareDialog(String hostName, String firmware, String manufactor, String routerName, String username, String password) {
        super(0, T.T.DataCompareDialog_DataCompareDialog_(), null, _GUI.T.literally_continue(), null);
        this.hostName = hostName;
        this.firmware = firmware;
        this.manufactor = manufactor;
        this.routerName = routerName;
        this.username = username;
        this.password = password;
        loginDesc = T.T.DataCompareDialog_layoutDialogContent_webinterface_desc();
    }

    @Override
    protected Object createReturnValue() {
        return null;
    }

    @Override
    public JComponent layoutDialogContent() {
        MigPanel p = new MigPanel("ins 0,wrap 3", "[][grow,fill][]", "[grow,fill]");
        JButton btnWebinterface = new JButton(new AbstractAction() {
            {
                putValue(NAME, T.T.DataCompareDialog_open_webinterface());
                putValue(SMALL_ICON, new AbstractIcon(IconKey.ICON_URL, 18));
            }

            public void actionPerformed(ActionEvent e) {
                CrossSystem.openURL("http://" + txtIP.getText());
            }
        });
        ExtTextArea desc = new ExtTextArea();
        desc.setText(T.T.DataCompareDialog_layoutDialogContent__desc());
        desc.setLabelMode(true);
        p.add(desc, "spanx");
        if (!loginsOnly) {
            p.add(header(NewTheme.I().getIcon(IconKey.ICON_MODEM, ICONSIZE), T.T.DataCompareDialog_layoutDialogContent_router(), T.T.DataCompareDialog_layoutDialogContent_router_desc()), "spanx");
            txtName = addField(p, T.T.DataCompareDialog_layoutDialogContent_name(), routerName, T.T.DataCompareDialog_layoutDialogContent_name_help(), null);
            txtManufactor = addField(p, T.T.DataCompareDialog_layoutDialogContent_manufactorName(), manufactor, T.T.DataCompareDialog_layoutDialogContent_manufactorName_help(), null);
            txtFirmware = addField(p, T.T.DataCompareDialog_layoutDialogContent_firmware(), firmware, T.T.DataCompareDialog_layoutDialogContent_firmware_help(), null);
        }
        if (!noLogins) {
            p.add(header(NewTheme.I().getIcon(org.jdownloader.gui.IconKey.ICON_BASICAUTH, ICONSIZE), T.T.DataCompareDialog_layoutDialogContent_webinterface(), loginDesc), "spanx");
            txtIP = addField(p, T.T.DataCompareDialog_layoutDialogContent_ip(), hostName, T.T.DataCompareDialog_layoutDialogContent_ip_help(), btnWebinterface);
            txtUser = addField(p, T.T.DataCompareDialog_layoutDialogContent_user(), username, T.T.DataCompareDialog_layoutDialogContent_user_help(), null);
            txtPassword = addField(p, T.T.DataCompareDialog_layoutDialogContent_password(), password, T.T.DataCompareDialog_layoutDialogContent_password_help(), null);
        }
        return p;
    }

    private JTextField addField(MigPanel p, String label, String value, String help, JComponent comp) {
        ExtTextField txt = new ExtTextField();
        txt.setHelpText(help);
        txt.setToolTipText(help);
        txt.setText(value);
        p.add(new JLabel(label), "gapleft " + (ICONSIZE + 8));
        if (comp == null) {
            p.add(txt, "spanx");
        } else {
            p.add(txt);
            p.add(comp);
        }
        return txt;
    }

    private MigPanel header(Icon icon, String string, String desc) {
        MigPanel ret = new MigPanel("ins 0", "[][][][grow,fill]", "[grow,fill]");
        ret.add(new JLabel(icon));
        ret.add(new JSeparator(), "width 10");
        ret.add(new JLabel(string));
        ret.add(new JSeparator());
        if (desc != null) {
            ExtTextArea txt = new ExtTextArea();
            txt.setText(desc);
            txt.setLabelMode(true);
            ret.add(txt, "spanx,pushx,growx,newline,gapleft " + (icon.getIconWidth() + 8));
        }
        return ret;
    }

    public void setLoginsOnly(boolean b) {
        loginsOnly = b;
    }

    public void setNoLogins(boolean b) {
        noLogins = b;
    }

    public boolean isNoLogins() {
        return noLogins;
    }

    public void setLoginsText(String loginDesc) {
        this.loginDesc = loginDesc;
    }
}
