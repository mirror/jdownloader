package org.jdownloader.dialogs;

import java.awt.Color;
import java.awt.Font;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

import org.appwork.swing.MigPanel;
import org.appwork.uio.UIOManager;
import org.appwork.utils.BinaryLogic;
import org.appwork.utils.StringUtils;
import org.appwork.utils.os.CrossSystem;
import org.appwork.utils.swing.EDTHelper;
import org.appwork.utils.swing.dialog.AbstractDialog;
import org.appwork.utils.swing.dialog.Dialog;
import org.jdownloader.translate._JDT;

public class NewPasswordDialog extends AbstractDialog<String[]> implements NewPasswordDialogInterface, CaretListener, MouseListener {
    public NewPasswordDialog(int flag, String title, String message, Icon icon, String ok, String cancel) {
        super(flag, title, icon, ok, cancel);
        this.message = message;
    }

    @Override
    protected int getPreferredWidth() {
        return 500;
    }

    public static void main(final String[] args) {
        final NewPasswordDialog d = new NewPasswordDialog(0, "titla", "msg", null, null, null);
        NewPasswordDialogInterface handler = UIOManager.I().show(null, d);
        System.out.println("Password: " + handler.getPassword() + " (" + (handler.getPasswordVerification().equals(handler.getPassword())) + ")");
    }

    private JPasswordField password;
    private JPasswordField passVerify;
    private Color          titleColor;
    private String         message;

    public void setMessage(final String message) {
        this.message = message;
    }

    private JLabel addSettingName(final String name) {
        final JLabel lbl = new JLabel(name);
        lbl.setForeground(titleColor);
        return lbl;
    }

    /**
     * @param textField
     */
    protected void modifyTextPane(JTextPane textField) {
        // TODO Auto-generated method stub
    }

    protected void addMessageComponent(final MigPanel p) {
        JTextPane textField = new JTextPane() {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean getScrollableTracksViewportWidth() {
                return !BinaryLogic.containsAll(flagMask, Dialog.STYLE_LARGE);
            }
        };
        modifyTextPane(textField);
        final Font font = textField.getFont();
        if (BinaryLogic.containsAll(flagMask, Dialog.STYLE_HTML)) {
            textField.setContentType("text/html");
            textField.addHyperlinkListener(new HyperlinkListener() {
                public void hyperlinkUpdate(final HyperlinkEvent e) {
                    if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                        CrossSystem.openURL(e.getURL());
                    }
                }
            });
        } else {
            textField.setContentType("text/plain");
            // this.textField.setMaximumSize(new Dimension(450, 600));
        }
        textField.setFont(font);
        textField.setText(getMessage());
        textField.setEditable(false);
        textField.setBackground(null);
        textField.setOpaque(false);
        textField.setFocusable(false);
        textField.putClientProperty("Synthetica.opaque", Boolean.FALSE);
        textField.setCaretPosition(0);
        if (BinaryLogic.containsAll(flagMask, Dialog.STYLE_LARGE)) {
            p.add(new JScrollPane(textField), "pushx,growx,spanx");
        } else {
            p.add(textField, "spanx");
        }
    }

    public String getMessage() {
        return message;
    }

    public void caretUpdate(final CaretEvent e) {
        cancel();
        String pw = new String(password.getPassword());
        String pwv = new String(passVerify.getPassword());
        if (StringUtils.isEmpty(pw)) {
            okButton.setEnabled(false);
        } else if (!StringUtils.equals(pw, pwv)) {
            okButton.setEnabled(false);
        } else {
            okButton.setEnabled(true);
        }
    }

    @Override
    protected String[] createReturnValue() {
        if ((getReturnmask() & (Dialog.RETURN_OK | Dialog.RETURN_TIMEOUT)) == 0) {
            return null;
        }
        return new String[] { new String(password.getPassword()), new String(passVerify.getPassword()) };
    }

    @Override
    public JComponent layoutDialogContent() {
        final MigPanel contentpane = new MigPanel("ins 5, wrap 2", "[]10[grow,fill]", "[][]");
        titleColor = Color.DARK_GRAY;
        password = new JPasswordField(10);
        passVerify = new JPasswordField(10);
        password.addCaretListener(this);
        passVerify.addCaretListener(this);
        password.addMouseListener(this);
        passVerify.addMouseListener(this);
        addMessageComponent(contentpane);
        contentpane.add(addSettingName(_JDT.T.newpassworddialog_password()));
        contentpane.add(password, "sizegroup g1");
        contentpane.add(addSettingName(_JDT.T.newpassworddialog_password_verify()));
        contentpane.add(passVerify, "sizegroup g1");
        caretUpdate(null);
        return contentpane;
    }

    @Override
    protected void packed() {
        super.packed();
        setResizable(false);
    }

    @Override
    protected void initFocus(final JComponent focus) {
        password.selectAll();
        password.requestFocusInWindow();
    }

    /*
     * (non-Javadoc)
     *
     * @see org.appwork.utils.swing.dialog.LoginDialogInterface#getPassword()
     */
    @Override
    public String getPassword() {
        if ((getReturnmask() & (Dialog.RETURN_OK | Dialog.RETURN_TIMEOUT)) == 0) {
            return null;
        }
        return new EDTHelper<String>() {
            @Override
            public String edtRun() {
                return new String(password.getPassword());
            }
        }.getReturnValue();
    }

    @Override
    public String getPasswordVerification() {
        if ((getReturnmask() & (Dialog.RETURN_OK | Dialog.RETURN_TIMEOUT)) == 0) {
            return null;
        }
        return new EDTHelper<String>() {
            @Override
            public String edtRun() {
                return new String(passVerify.getPassword());
            }
        }.getReturnValue();
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        cancel();
    }

    @Override
    public void mousePressed(MouseEvent e) {
    }

    @Override
    public void mouseReleased(MouseEvent e) {
    }

    @Override
    public void mouseEntered(MouseEvent e) {
    }

    @Override
    public void mouseExited(MouseEvent e) {
    }
}