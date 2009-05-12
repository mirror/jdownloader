package jd.gui.skins.simple.components.Linkgrabber;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.SwingConstants;

import jd.config.Configuration;
import jd.gui.skins.simple.JTabbedPanel;
import jd.gui.skins.simple.components.ComboBrowseFile;
import jd.gui.skins.simple.components.JDFileChooser;
import jd.gui.skins.simple.components.JDTextField;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;
import net.miginfocom.swing.MigLayout;

public class LinkGrabberFilePackageInfo extends JTabbedPanel implements ActionListener {

    private static final long serialVersionUID = 5410296068527460629L;

    private ComboBrowseFile brwSaveTo;

    private JDTextField txtComment;

    private JDTextField txtName;

    private JDTextField txtPassword;

    private JCheckBox chbExtract;

    private JCheckBox chbUseSubdirectory;

    private LinkGrabberFilePackage fp = null;

    private boolean notifyUpdate = true;

    public LinkGrabberFilePackageInfo() {
        buildGui();
        fp = null;
    }

    public void setPackage(LinkGrabberFilePackage fp) {
        if (this.fp != null && this.fp == fp) {
            update();
            return;
        }
        this.fp = fp;
        if (this.fp != null) {
            update();
        }
    }

    public void update() {
        if (fp == null) return;
        /*
         * wichtig: die set funktionen lösen eine action aus , welche ansonsten
         * wiederum ein updatevent aufrufen würden
         */
        notifyUpdate = false;
        if (!txtName.isFocusOwner()) txtName.setText(fp.getName());
        if (!txtComment.isFocusOwner()) txtComment.setText(fp.getComment());
        if (!txtPassword.isFocusOwner()) txtPassword.setText(fp.getPassword());
        if (!brwSaveTo.isFocusOwner()) brwSaveTo.setText(fp.getDownloadDirectory());
        if (!chbExtract.isFocusOwner()) chbExtract.setSelected(fp.isExtractAfterDownload());
        if (!chbUseSubdirectory.isFocusOwner()) chbUseSubdirectory.setSelected(fp.useSubDir());
        /* neuzeichnen */
        revalidate();
        notifyUpdate = true;
    }

    public LinkGrabberFilePackage getPackage() {
        return fp;
    }

    private void buildGui() {

        txtName = new JDTextField();
        txtName.setAutoSelect(true);
        txtName.addActionListener(this);

        brwSaveTo = new ComboBrowseFile("DownloadSaveTo");
        brwSaveTo.setEditable(true);
        brwSaveTo.setFileSelectionMode(JDFileChooser.DIRECTORIES_ONLY);
        brwSaveTo.setText(JDUtilities.getConfiguration().getDefaultDownloadDirectory());
        brwSaveTo.addActionListener(this);

        txtPassword = new JDTextField();
        txtPassword.addActionListener(this);
        txtComment = new JDTextField();
        txtComment.addActionListener(this);

        chbExtract = new JCheckBox(JDLocale.L("gui.linkgrabber.packagetab.chb.extractAfterdownload", "Extract"));
        chbExtract.setSelected(true);
        chbExtract.setHorizontalTextPosition(SwingConstants.LEFT);
        chbExtract.addActionListener(this);

        chbUseSubdirectory = new JCheckBox(JDLocale.L("gui.linkgrabber.packagetab.chb.useSubdirectory", "Use Subdirectory"));
        chbUseSubdirectory.setSelected(JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_USE_PACKETNAME_AS_SUBFOLDER, false));
        chbUseSubdirectory.setHorizontalTextPosition(SwingConstants.LEFT);
        chbUseSubdirectory.addActionListener(this);

        this.setLayout(new MigLayout("ins 10, wrap 2", "[]10[grow,fill]", "[]5[]5[]5[]"));

        this.add(new JLabel(JDLocale.L("gui.linkgrabber.packagetab.lbl.name", "Paketname")));
        this.add(txtName);
        this.add(new JLabel(JDLocale.L("gui.linkgrabber.packagetab.lbl.saveto", "Speichern unter")));
        this.add(brwSaveTo);
        this.add(new JLabel(JDLocale.L("gui.linkgrabber.packagetab.lbl.password", "Archivpasswort")));
        this.add(txtPassword, "split 2, gapright 10, growx");
        this.add(chbExtract);

        /*
         * bitte noch die Checkbox für Subdirectory auch einbauen TODO: wo soll sie hin?
         */
        this.add(new JLabel(JDLocale.L("gui.linkgrabber.packagetab.lbl.comment", "Kommentar")));
        this.add(txtComment);
    }

    public void actionPerformed(ActionEvent e) {
        if (fp == null || !notifyUpdate) return;
        if (e.getSource() == txtName) {
            fp.setName(txtName.getText());
        } else if (e.getSource() == brwSaveTo) {
            fp.setDownloadDirectory(brwSaveTo.getText());
        } else if (e.getSource() == txtComment) {
            fp.setComment(txtComment.getText());
        } else if (e.getSource() == txtPassword) {
            fp.setPassword(txtPassword.getText());
        } else if (e.getSource() == chbExtract) {
            fp.setExtractAfterDownload(chbExtract.isSelected());
        } else if (e.getSource() == chbUseSubdirectory) {
            fp.setUseSubDir(chbUseSubdirectory.isSelected());
        }
    }

    // @Override
    public void onDisplay() {
        update();
    }

    // @Override
    public void onHide() {
        if (this.fp == null) return;
        fp = null;
    }

}
