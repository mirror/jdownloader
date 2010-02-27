package jd.plugins.optional.jdnotifyme;

import java.awt.event.ActionEvent;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTextPane;

import jd.gui.UserIO;
import jd.gui.swing.components.linkbutton.JLink;
import jd.gui.swing.dialog.AbstractDialog;
import jd.utils.locale.JDL;

public class NotifyDialog extends AbstractDialog {

    public static void main(String[] args) {
        new NotifyDialog(new String[] { "Ich bin die Eine", "Jetzt bist du nicht mehr alleine", "Aller guten Dinge sind drei", "Ich sag mal, vier gewinnt =)" });
    }

    private static final long serialVersionUID = -7260383841332074793L;

    private static final String JDL_PREFIX = "jd.plugins.optional.jdnotifyme.NotifyDialog.";

    private int nextIndex = 1;
    private String[] messages;
    private JButton btnNext;
    private JTextPane textField;

    public NotifyDialog(String[] messages) {
        super(UserIO.NO_COUNTDOWN | UserIO.NO_ICON | UserIO.NO_CANCEL_OPTION, JDL.LF(JDL_PREFIX + "title", "%s new messages", messages.length), null, null, null);

        this.messages = messages;

        init();
    }

    @Override
    protected void packed() {
        this.setModal(false);
    }

    @Override
    protected void addButtons(JPanel buttonBar) {
        btnNext = new JButton(JDL.LF(JDL_PREFIX + "next", "Next message (of %s)", messages.length - nextIndex));
        btnNext.addActionListener(this);
        if (messages.length == 1) btnNext.setEnabled(false);
        buttonBar.add(btnNext);
    }

    @Override
    public JComponent contentInit() {
        textField = new JTextPane();
        textField.setContentType("text/html");
        textField.addHyperlinkListener(JLink.getHyperlinkListener());
        textField.setText(messages[0]);
        textField.setEditable(false);
        textField.setBackground(null);
        textField.setOpaque(false);
        textField.putClientProperty("Synthetica.opaque", Boolean.FALSE);
        return textField;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == btnNext) {
            textField.setText(messages[nextIndex]);
            nextIndex++;
            btnNext.setText(JDL.LF(JDL_PREFIX + "next", "Next message (of %s)", messages.length - nextIndex));
            if (nextIndex == messages.length) btnNext.setEnabled(false);
        } else {
            super.actionPerformed(e);
        }
    }

}
