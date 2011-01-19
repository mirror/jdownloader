package jd.gui.swing.dialog;

import java.util.Arrays;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import jd.utils.JDTheme;
import jd.utils.locale.JDL;
import net.miginfocom.swing.MigLayout;

import org.appwork.utils.swing.dialog.AbstractDialog;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;

public class ProxyDialog extends AbstractDialog<String[]> {

    public static void main(String[] args) {
        JDTheme.setTheme("default");
        try {
            System.out.println(Arrays.toString(Dialog.getInstance().showDialog(new ProxyDialog())));
        } catch (DialogClosedException e) {
            e.printStackTrace();
        } catch (DialogCanceledException e) {
            e.printStackTrace();
        }
    }

    private static final long   serialVersionUID = 8512889396415100663L;
    private static final String JDL_PREFIX       = "jd.gui.swing.dialog.ProxyDialog.";

    private JComboBox           cmbType;
    private JTextField          txtHost;
    private JTextField          txtPort;
    private JTextField          txtUser;
    private JTextField          txtPass;

    public ProxyDialog() {
        super(0, JDL.L(JDL_PREFIX + "title", "Add new Proxy"), JDTheme.II("gui.images.proxy", 32, 32), null, null);
    }

    @Override
    public JComponent layoutDialogContent() {
        JPanel panel = new JPanel(new MigLayout("ins 0, wrap 4", "[][grow 10,fill][][grow 3,fill]"));

        panel.add(new JLabel(JDL.L(JDL_PREFIX + "type", "Type:")));
        panel.add(cmbType = new JComboBox(new String[] { JDL.L(JDL_PREFIX + "http", "HTTP"), JDL.L(JDL_PREFIX + "socks5", "Socks5"), JDL.L(JDL_PREFIX + "localip", "Local IP") }), "spanx");

        panel.add(new JLabel(JDL.L(JDL_PREFIX + "hostport", "Host/Port:")));
        panel.add(txtHost = new JTextField());
        panel.add(new JLabel(":"));
        panel.add(txtPort = new JTextField(), "shrinkx");

        panel.add(new JLabel(JDL.L(JDL_PREFIX + "username", "Username:")));
        panel.add(txtUser = new JTextField(), "spanx");

        panel.add(new JLabel(JDL.L(JDL_PREFIX + "password", "Password:")));
        panel.add(txtPass = new JTextField(), "spanx");

        return panel;
    }

    @Override
    protected String[] createReturnValue() {
        return new String[] { cmbType.getSelectedItem().toString(), txtHost.getText(), txtPort.getText(), txtUser.getText(), txtPass.getText() };
    }

}
