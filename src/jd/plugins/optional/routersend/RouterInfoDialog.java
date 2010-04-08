package jd.plugins.optional.routersend;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import jd.gui.UserIO;
import jd.gui.swing.dialog.AbstractDialog;
import jd.utils.locale.JDL;
import net.miginfocom.swing.MigLayout;

public class RouterInfoDialog extends AbstractDialog {

    public static void main(String[] args) {
        System.out.println(Arrays.toString(RouterInfoDialog.showDialog()));
    }

    public static String[] showDialog() {
        RouterInfoDialog dialog = new RouterInfoDialog();
        if (UserIO.isOK(dialog.getReturnValue())) return dialog.getResult();
        return null;
    }

    private static final long serialVersionUID = -1900685024690304148L;

    private static final String JDL_PREFIX = "jd.plugins.optional.routersend.RouterInfoDialog.";

    private ComboTextPanel manufacturer, name, firmware;
    private JTextField username, password;

    private RouterInfoDialog() {
        super(UserIO.NO_COUNTDOWN | UserIO.NO_ICON, JDL.L(JDL_PREFIX + "title", "Select Router"), null, JDL.L(JDL_PREFIX + "submit", "Submit Script"), null);

        init();
    }

    public String[] getResult() {
        return new String[] { manufacturer.getValue(), name.getValue(), firmware.getValue(), username.getText(), password.getText() };
    }

    @Override
    public JComponent contentInit() {
        JPanel content = new JPanel(new MigLayout("ins 0, wrap 2", "[][grow,fill]"));

        manufacturer = new ComboTextPanel(content, JDL.L(JDL_PREFIX + "manufacturer", "Manufacturer:"), new String[] { "Manufacturer A", "Manufacturer B", "Manufacturer C" });

        name = new ComboTextPanel(content, JDL.L(JDL_PREFIX + "name", "Router Name:"), new String[] { "Name A", "Name B", "Name C" });

        firmware = new ComboTextPanel(content, JDL.L(JDL_PREFIX + "firmware", "Firmware:"), new String[] { "Firmware A", "Firmware B", "Firmware C" });

        content.add(new JLabel("INFOTEXT BLABLA ;-)"), "spanx, center");

        content.add(new JLabel(JDL.L(JDL_PREFIX + "username", "Username:")));
        content.add(username = new JTextField());

        content.add(new JLabel(JDL.L(JDL_PREFIX + "password", "Password:")));
        content.add(password = new JTextField());

        return content;
    }

    private class ComboTextPanel implements ActionListener {

        private static final long serialVersionUID = -5840845914684775943L;

        private JLabel lbl;
        private JComboBox cb;
        private JTextField tf;

        public ComboTextPanel(JPanel parent, String name, String[] values) {
            lbl = new JLabel(name);

            cb = new JComboBox(values);
            cb.addItem(JDL.L(JDL_PREFIX + "other", "Other"));
            cb.addActionListener(this);

            tf = new JTextField();
            tf.setEnabled(false);

            parent.add(lbl);
            parent.add(cb);
            parent.add(tf, "skip, gapbottom 10");
        }

        @SuppressWarnings("unused")
        public void setEnabled(boolean b) {
            lbl.setEnabled(b);
            cb.setEnabled(b);
            tf.setEnabled(b);
        }

        public String getValue() {
            if (tf.isEnabled()) return tf.getText();
            return cb.getSelectedItem().toString();
        }

        public void actionPerformed(ActionEvent e) {
            tf.setEnabled(cb.getSelectedItem().toString().equals(JDL.L(JDL_PREFIX + "other", "Other")));
        }

    }

}
