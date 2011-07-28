package org.jdownloader.gui.views.downloads;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Component;
import java.awt.Composite;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import jd.gui.swing.jdgui.JDGui;
import jd.plugins.DownloadLink;

import org.appwork.app.gui.MigPanel;
import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.swing.dialog.AbstractDialog;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;

public class LinkPropertiesDialog extends AbstractDialog<Object> implements FocusListener {
    private JTextField   txtName;
    private JTextArea    txtComment;
    private Color        headName;
    private DownloadLink link;
    private JTextField   txtMd5;
    private JTextField   txtSha1;
    private JButton      btnReset;
    private JTextField   txtDownloadpassword;

    public LinkPropertiesDialog(DownloadLink fp) {
        super(0, _GUI._.LinkPropertiesDialog_LinkPropertiesDialog(fp.getName()), null, _GUI._.LinkPropertiesDialog_LinkPropertiesDialog_save(), null);
        this.link = fp;
    }

    @Override
    protected Object createReturnValue() {
        return null;
    }

    public String getComment() {
        return txtComment.getText();
    }

    public String getLinkName() {
        return txtName.getText();

    }

    public String getDownloadPassword() {
        return txtDownloadpassword.getText();
    }

    public String getMd5() {
        return txtMd5.getText();

    }

    public String getSha1() {
        return txtSha1.getText();
    }

    @Override
    public JComponent layoutDialogContent() {
        final MigPanel p = new MigPanel("ins 0,wrap 2", "[][grow,fill]", "[]") {

            @Override
            protected void paintComponent(Graphics g) {

                final Graphics2D g2 = (Graphics2D) g;
                Composite comp = g2.getComposite();
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.3f));
                g.drawImage(NewTheme.I().getImage("file", 128), 0, getHeight() - 128, null);
                g2.setComposite(comp);
                setOpaque(false);
                super.paintComponent(g2);

            }

        };
        headName = p.getBackground().darker().darker().darker();
        txtName = new JTextField();
        txtDownloadpassword = new JTextField();
        txtDownloadpassword.setText(link.getDownloadPassword());
        txtName.setText(link.getName());
        txtMd5 = new JTextField();
        txtMd5.setText(link.getMD5Hash());
        txtSha1 = new JTextField();
        txtSha1.setText(link.getSha1Hash());
        txtComment = new JTextArea();
        txtComment.setText(link.getSourcePluginComment());
        btnReset = new JButton(NewTheme.I().getIcon("reset", 16));
        btnReset.setToolTipText(_GUI._.LinkPropertiesDialog_layoutDialogContent_reset_filename_tt());
        btnReset.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {

                txtName.setText(link.getFinalFileName());
            }
        });

        // icon.setVerticalAlignment(SwingConstants.TOP);
        // p.add(icon, "spany 5,aligny top");
        p.add(head(_GUI._.LinkPropertiesDialog_layoutDialogContent_name()));
        p.add(txtName, "width n:300:n,split 2");
        p.add(btnReset, "width 24!");
        p.add(head(_GUI._.LinkPropertiesDialog_layoutDialogContent_downloadpassword()));
        p.add(txtDownloadpassword);
        p.add(head(_GUI._.LinkPropertiesDialog_layoutDialogContent_md5()));
        p.add(txtMd5);
        p.add(head(_GUI._.LinkPropertiesDialog_layoutDialogContent_sha1()));
        p.add(txtSha1);
        txtMd5.addFocusListener(this);
        txtSha1.addFocusListener(this);
        p.add(head(_GUI._.LinkPropertiesDialog_layoutDialogContent_comment()));
        p.add(new JScrollPane(txtComment), "height 40:80:n");

        p.add(head(_GUI._.LinkPropertiesDialog_layoutDialogContent_size(SizeFormatter.formatBytes(link.getDownloadSize())), NewTheme.I().getIcon("batch", 16)), "spanx,newline,skip");

        p.add(head(_GUI._.LinkPropertiesDialog_layoutDialogContent_hoster(link.getHost()), link.getDefaultPlugin().getHosterIconScaled()), "spanx,newline,skip");
        if (!link.isAvailabilityStatusChecked()) {
            p.add(head(_GUI._.LinkPropertiesDialog_layoutDialogContent_unchecked(), NewTheme.I().getIcon("help", 16)), "spanx,newline,skip");

        } else if (link.isAvailable()) {
            p.add(head(_GUI._.LinkPropertiesDialog_layoutDialogContent_available(), NewTheme.I().getIcon("ok", 16)), "spanx,newline,skip");

        } else {
            p.add(head(_GUI._.LinkPropertiesDialog_layoutDialogContent_notavalable(), NewTheme.I().getIcon("error", 16)), "spanx,newline,skip");

        }

        return p;
    }

    private Component head(String name) {

        return head(name, null);
    }

    private Component head(String name, ImageIcon imageIcon) {
        JLabel lbl = new JLabel(name);
        lbl.setIcon(imageIcon);
        lbl.setForeground(headName);
        return lbl;
    }

    public void focusGained(FocusEvent e) {
        JDGui.help(_GUI._.LinkPropertiesDialog_focusGained_(), _GUI._.LinkPropertiesDialog_focusGained_msg(), NewTheme.I().getIcon("warning", 32));
    }

    public void focusLost(FocusEvent e) {
    }
}
