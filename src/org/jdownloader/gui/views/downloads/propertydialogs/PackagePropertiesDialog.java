package org.jdownloader.gui.views.downloads.propertydialogs;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Component;
import java.awt.Composite;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;
import java.util.Enumeration;

import javax.swing.Box;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import jd.HostPluginWrapper;
import jd.gui.swing.jdgui.views.settings.components.FolderChooser;
import jd.plugins.FilePackage;

import org.appwork.app.gui.MigPanel;
import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.swing.dialog.AbstractDialog;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;

public class PackagePropertiesDialog extends AbstractDialog<Object> {

    private JTextField       txtName;
    private JTextArea        txtComment;
    private JList            lstPasswords;
    private JCheckBox        chbExtract;
    private Color            headName;
    private JButton          btAdd;
    private JButton          btRemove;
    private FilePackage      pkg;
    private JScrollPane      pwScroll;
    private JButton          btBrowser;
    private FolderChooser    txtFolder;
    private DefaultListModel model;

    public PackagePropertiesDialog(FilePackage fp) {
        super(0, _GUI._.PackagePropertiesDialog_PackagePropertiesDialog(fp.getName()), null, _GUI._.PackagePropertiesDialog_PackagePropertiesDialog_save(), null);
        this.pkg = fp;
    }

    @Override
    protected Object createReturnValue() {
        return null;
    }

    public ArrayList<String> getPasswordList() {
        ArrayList<String> ret = new ArrayList<String>();
        Enumeration<?> elements = model.elements();
        while (elements.hasMoreElements()) {
            ret.add((String) elements.nextElement());
        }
        return ret;
    }

    public boolean isExtractEnabled() {
        return chbExtract.isSelected();
    }

    public String getComment() {
        return txtComment.getText();
    }

    public String getDownloadDirectory() {
        return txtFolder.getText();
    }

    public String getPackageName() {
        return txtName.getText();

    }

    @Override
    public JComponent layoutDialogContent() {
        final MigPanel p = new MigPanel("ins 0,wrap 2", "[][grow,fill]", "[]") {

            @Override
            protected void paintComponent(Graphics g) {

                final Graphics2D g2 = (Graphics2D) g;
                Composite comp = g2.getComposite();
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.3f));
                g.drawImage(NewTheme.I().getImage("archive", 128), 0, getHeight() - 128, null);
                g2.setComposite(comp);
                setOpaque(false);
                super.paintComponent(g2);

            }

        };
        headName = p.getBackground().darker().darker().darker();
        txtName = new JTextField();
        txtName.setText(pkg.getName());
        txtComment = new JTextArea();
        txtComment.setText(pkg.getComment());
        txtFolder = new FolderChooser("downloadfolder");
        txtFolder.setText(pkg.getDownloadDirectory());
        model = new DefaultListModel();
        for (String pw : pkg.getPasswordList()) {
            model.addElement(pw);
        }
        lstPasswords = new JList(model);
        lstPasswords.addKeyListener(new KeyListener() {

            public void keyTyped(KeyEvent e) {
            }

            public void keyReleased(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_DELETE) {
                    Object[] selected = lstPasswords.getSelectedValues();
                    for (Object o : selected) {
                        model.removeElement(o);
                    }
                }
            }

            public void keyPressed(KeyEvent e) {
            }
        });
        chbExtract = new JCheckBox();
        chbExtract.setSelected(pkg.isPostProcessing());
        // chbExtract.setHorizontalAlignment(SwingConstants.RIGHT);

        btAdd = new JButton(NewTheme.I().getIcon("add", 16));
        btAdd.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                String pw;
                try {
                    pw = Dialog.getInstance().showInputDialog(0, _GUI._.PackagePropertiesDialog_actionPerformed_newpassword_(pkg.getName()), "");

                    model.addElement(pw);

                } catch (DialogClosedException e1) {
                    e1.printStackTrace();
                } catch (DialogCanceledException e1) {
                    e1.printStackTrace();
                }
            }
        });
        btRemove = new JButton(NewTheme.I().getIcon("remove", 16));
        btRemove.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                Object[] selected = lstPasswords.getSelectedValues();
                for (Object o : selected) {
                    model.removeElement(o);
                }

            }
        });

        p.add(head(_GUI._.PackagePropertiesDialog_layoutDialogContent_name()));
        p.add(txtName);
        p.add(head(_GUI._.PackagePropertiesDialog_layoutDialogContent_downloadfolder()));
        p.add(txtFolder);

        p.add(head(_GUI._.PackagePropertiesDialog_layoutDialogContent_comment()));
        p.add(new JScrollPane(txtComment), "height 40:80:n");
        p.add(head(_GUI._.PackagePropertiesDialog_layoutDialogContent_extract()));
        p.add(chbExtract);
        p.add(head(_GUI._.PackagePropertiesDialog_layoutDialogContent_passwords()));

        p.add(pwScroll = new JScrollPane(lstPasswords), "height 40:80:n,hidemode 3");
        // pwScroll.setVisible(model.size() > 0);
        p.add(Box.createHorizontalGlue(), "split 3,skip");
        p.add(btAdd, "width 20!,height 20!");
        p.add(btRemove, "width 20!,height 20!");

        p.add(head(_GUI._.LinkPropertiesDialog_layoutDialogContent_size(SizeFormatter.formatBytes(pkg.getTotalEstimatedPackageSize())), NewTheme.I().getIcon("batch", 16)), "spanx,newline,skip");
        for (String h : pkg.getHosterList()) {

            for (HostPluginWrapper hw : HostPluginWrapper.getHostWrapper()) {
                if (h.equalsIgnoreCase(hw.getHost())) {
                    ImageIcon icon = hw.getIconUnscaled();
                    ImageIcon ret = NewTheme.I().getScaledInstance(icon, 16);
                    p.add(head(_GUI._.LinkPropertiesDialog_layoutDialogContent_hoster(h), ret), "spanx,newline,skip");

                    break;
                }
            }

        }

        return p;
    }

    public static void main(String[] args) {
        try {
            FilePackage fp = FilePackage.getInstance();
            fp.setName("My Package");
            fp.setComment("Kommenztar");
            fp.setPassword("passwords");
            fp.setPostProcessing(true);
            fp.setDownloadDirectory("c:/");
            Dialog.getInstance().showDialog(new PackagePropertiesDialog(null));
        } catch (DialogClosedException e) {
            e.printStackTrace();
        } catch (DialogCanceledException e) {
            e.printStackTrace();
        }
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

}
