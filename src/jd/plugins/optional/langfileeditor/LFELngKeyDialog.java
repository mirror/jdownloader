package jd.plugins.optional.langfileeditor;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JSeparator;

import jd.nutils.Screen;
import jd.utils.JDLocale;
import net.miginfocom.swing.MigLayout;

public class LFELngKeyDialog extends JDialog implements ActionListener {

    private static final long serialVersionUID = -1164234990578764518L;

    public static String showDialog(JFrame owner, String oldKey) {
        LFELngKeyDialog dialog = new LFELngKeyDialog(owner, oldKey);
        dialog.setVisible(true);

        return dialog.getResult();
    }

    private JComboBox cmbKey;

    private JButton btnOk;

    private JButton btnCancel;

    private String result = null;

    private LFELngKeyDialog(JFrame owner, String oldKey) {
        super(owner);

        setModal(true);
        setTitle(JDLocale.L("plugins.optional.langfileeditor.translatedialog.title", "Languagekey"));
        setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {

            @Override
            public void windowClosing(WindowEvent e) {
                result = null;
                dispose();
            }

        });

        cmbKey = new JComboBox(new String[] { "da", "de", "fi", "fr", "el", "hi", "it", "ja", "ko", "hr", "nl", "no", "pl", "pt", "ro", "ru", "sv", "es", "cs", "en", "ar" });
        cmbKey.setSelectedItem(oldKey);

        btnOk = new JButton(JDLocale.L("gui.btn_ok", "OK"));
        btnOk.addActionListener(this);

        btnCancel = new JButton(JDLocale.L("gui.btn_cancel", "Cancel"));
        btnCancel.addActionListener(this);

        setLayout(new MigLayout("", "[center]"));
        add(new JLabel(JDLocale.L("plugins.optional.langfileeditor.translatedialog.message", "Choose Languagekey:")), "split 2");
        add(cmbKey, "wrap");
        add(new JSeparator(), "growx, wrap");
        add(btnOk, "split 2");
        add(btnCancel);
        pack();
        setLocation(Screen.getCenterOfComponent(owner, this));
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == btnOk) {
            result = cmbKey.getSelectedItem().toString();
        } else if (e.getSource() == btnCancel) {
            result = null;
        }
        dispose();
    }

    private String getResult() {
        return result;
    }

}
