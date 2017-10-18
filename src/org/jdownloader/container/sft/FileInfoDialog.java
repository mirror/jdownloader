package org.jdownloader.container.sft;

import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JRootPane;
import javax.swing.JSeparator;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

import net.miginfocom.swing.MigLayout;

import org.appwork.uio.UIOManager;
import org.appwork.utils.locale._AWU;
import org.appwork.utils.os.CrossSystem;
import org.appwork.utils.swing.dialog.AbstractDialog;

public class FileInfoDialog extends AbstractDialog<String> implements ActionListener, DocumentListener {
    protected final JButton        dynamicOkButton = new JButton(_AWU.T.ABSTRACTDIALOG_BUTTON_OK());
    protected final JPasswordField passwordField   = new JPasswordField();
    protected final JTextPane      textPane        = new JTextPane();
    protected final boolean        needPassword;
    protected final sftContainer   container;
    protected boolean              lastCryptState;

    public FileInfoDialog(sftContainer container) {
        super(UIOManager.BUTTONS_HIDE_OK, "Downloaddetails", null, null, null);
        this.needPassword = container.needPassword();
        this.lastCryptState = !container.isDecrypted();
        this.container = container;
    }

    protected void setInfo() {
        if (lastCryptState != container.isDecrypted()) {
            String unknown = "??";
            String na = "N/A";
            String Description = container.isDecrypted() ? container.getDescription() : unknown;
            String Uploader = container.isDecrypted() ? container.getUploader() : unknown;
            String Comment = container.isDecrypted() ? container.getComment() : unknown;
            if (Description == null) {
                Description = na;
            }
            if (Uploader == null) {
                Uploader = na;
            }
            if (Comment == null) {
                Comment = na;
            }
            StringBuilder info_builder = new StringBuilder();
            info_builder.append("<table border=\"0\" cellpadding=\"0\" cellspacing=\"0\"><tr><td width=\"1%\"><strong>");
            info_builder.append("Beschreibung:");
            info_builder.append("  </strong></td><td>");
            info_builder.append(Description);
            info_builder.append("</tr><tr><td><strong>");
            info_builder.append("Upper:");
            info_builder.append("  </strong></td><td>");
            info_builder.append(Uploader);
            info_builder.append("</td></tr><tr><td><strong>");
            info_builder.append("Kommentar:");
            info_builder.append("  </strong></td><td>");
            info_builder.append(Comment);
            info_builder.append("</td></tr></table>");
            textPane.setText(info_builder.toString());
            lastCryptState = container.isDecrypted();
        }
    }

    @Override
    public void insertUpdate(DocumentEvent e) {
        this.changedUpdate(e);
    }

    @Override
    public void removeUpdate(DocumentEvent e) {
        this.changedUpdate(e);
    }

    @Override
    public void changedUpdate(DocumentEvent e) {
        final boolean passwordOk = this.container.setPassword(this.passwordField.getPassword());
        this.dynamicOkButton.setEnabled(passwordOk);
        setInfo();
    }

    @Override
    public void actionPerformed(final ActionEvent e) {
        if (e.getSource() == this.dynamicOkButton) {
            org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().fine("Answer: Button<OK:" + this.dynamicOkButton.getText() + ">");
            this.setReturnmask(true);
        } else if (e.getActionCommand().equals("enterPushed")) {
            if (this.container.isDecrypted()) {
                org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().fine("Answer: Button<OK:" + this.okButton.getText() + ">");
                this.setReturnmask(true);
            } else {
                return;
            }
        }
        super.actionPerformed(e);
    }

    @Override
    protected String createReturnValue() {
        return null;
    }

    @Override
    protected void addButtons(final JPanel buttonBar) {
        this.dynamicOkButton.addActionListener(this);
        this.dynamicOkButton.setEnabled(!this.needPassword);
        this.dynamicOkButton.addActionListener(this);
        this.dynamicOkButton.addHierarchyListener(new HierarchyListener() {
            public void hierarchyChanged(final HierarchyEvent e) {
                if ((e.getChangeFlags() & HierarchyEvent.PARENT_CHANGED) != 0) {
                    final JButton defaultButton = (JButton) e.getComponent();
                    final JRootPane root = SwingUtilities.getRootPane(defaultButton);
                    if (root != null) {
                        root.setDefaultButton(defaultButton);
                    }
                }
            }
        });
        this.dynamicOkButton.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(final FocusEvent e) {
                final JRootPane root = SwingUtilities.getRootPane(e.getComponent());
                if (root != null && e.getComponent() instanceof JButton) {
                    root.setDefaultButton((JButton) e.getComponent());
                }
            }

            @Override
            public void focusLost(final FocusEvent e) {
                final JRootPane root = SwingUtilities.getRootPane(e.getComponent());
                if (root != null) {
                    root.setDefaultButton(null);
                }
            }
        });
        buttonBar.add(this.dynamicOkButton, "alignx right,tag ok,sizegroup confirms");
    }

    @Override
    public JComponent layoutDialogContent() {
        final JPanel centerPanel = new JPanel();
        final JSeparator separator = new JSeparator();
        final JLabel labelPassword = new JLabel("Passwort:");
        String Layout = new String("[][][]");
        if (!this.needPassword) {
            this.passwordField.setVisible(false);
            labelPassword.setVisible(false);
            Layout = new String("[][::0px][]");
        }
        labelPassword.setLabelFor(passwordField);
        this.passwordField.getDocument().addDocumentListener(this);
        this.passwordField.setActionCommand("enterPushed");
        this.passwordField.addActionListener(this);
        final Font font = textPane.getFont();
        textPane.setContentType("text/html");
        textPane.setFont(font);
        this.textPane.setEditable(false);
        this.textPane.setBackground(null);
        this.textPane.setOpaque(false);
        this.textPane.putClientProperty("Synthetica.opaque", Boolean.FALSE);
        this.textPane.setCaretPosition(0);
        this.textPane.addHyperlinkListener(new HyperlinkListener() {
            public void hyperlinkUpdate(final HyperlinkEvent e) {
                if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                    CrossSystem.openURL(e.getURL());
                }
            }
        });
        centerPanel.setLayout(new MigLayout("", "[320px:n]", Layout));
        centerPanel.add(separator, "cell 0 2,grow");
        centerPanel.add(labelPassword, "flowx,cell 0 1,alignx left,aligny center");
        centerPanel.add(this.textPane, "cell 0 0,growx,aligny top");
        centerPanel.add(this.passwordField, "cell 0 1,growx,aligny top");
        this.setInfo();
        return centerPanel;
    }
}
