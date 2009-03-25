package jd.gui.skins.simple.components.Linkgrabber;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;

import jd.config.Configuration;
import jd.config.SubConfiguration;
import jd.gui.skins.simple.components.ComboBrowseFile;
import jd.gui.skins.simple.components.JDFileChooser;
import jd.gui.skins.simple.components.JDTextField;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;
import net.miginfocom.swing.MigLayout;

public class LinkGrabberV2FilePackageInfo extends JPanel implements UpdateListener, ActionListener {

    private static final long serialVersionUID = 5410296068527460629L;

    private ComboBrowseFile brwSaveTo;

    private JDTextField txtComment;

    private JDTextField txtName;

    private JDTextField txtPassword;
    
    private JDTextField dlPassword;

    private JCheckBox chbExtract;

    private JCheckBox chbUseSubdirectory;

    private JTabbedPane tabbedPane;

    private JPanel simplePanel, extendedPanel;

    private LinkGrabberV2FilePackage fp = null;

    public LinkGrabberV2FilePackageInfo() {
        buildGui();
        fp = null;
    }

    public void setPackage(LinkGrabberV2FilePackage fp) {
        if (this.fp != null) this.fp.getUpdateBroadcaster().removeUpdateListener(this);
        this.fp = fp;
        if (this.fp != null) {
            fp.getUpdateBroadcaster().addUpdateListener(this);
            txtName.setText(fp.getName());
            brwSaveTo.setText(fp.getDownloadDirectory());
        }
    }

    public LinkGrabberV2FilePackage getPackage() {
        return fp;
    }

    private void buildGui() {
        tabbedPane = new JTabbedPane();

        simplePanel = new JPanel();
        extendedPanel = new JPanel();

        txtName = new JDTextField();
        txtName.setAutoSelect(true);
        txtName.addActionListener(this);

        brwSaveTo = new ComboBrowseFile("DownloadSaveTo");
        brwSaveTo.setEditable(false);
        brwSaveTo.setFileSelectionMode(JDFileChooser.DIRECTORIES_ONLY);
        brwSaveTo.setText(JDUtilities.getConfiguration().getDefaultDownloadDirectory());
        brwSaveTo.addActionListener(this);

        txtPassword = new JDTextField();
        txtComment = new JDTextField();

        chbExtract = new JCheckBox(JDLocale.L("gui.linkgrabber.packagetab.chb.extractAfterdownload", "Extract"));
        chbExtract.setSelected(true);
        chbExtract.setHorizontalTextPosition(SwingConstants.LEFT);

        chbUseSubdirectory = new JCheckBox(JDLocale.L("gui.linkgrabber.packagetab.chb.useSubdirectory", "Use Subdirectory"));
        chbUseSubdirectory.setSelected(JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_USE_PACKETNAME_AS_SUBFOLDER, false));
        chbUseSubdirectory.setHorizontalTextPosition(SwingConstants.LEFT);
        
        dlPassword = new JDTextField();

        simplePanel.setLayout(new MigLayout("", "[]10px[grow]", "[][]"));

        simplePanel.add(new JLabel(JDLocale.L("gui.linkgrabber.packagetab.lbl.name", "Paketname")));
        simplePanel.add(txtName, "growx, wrap");
        simplePanel.add(new JLabel(JDLocale.L("gui.linkgrabber.packagetab.lbl.saveto", "Speichern unter")));
        simplePanel.add(brwSaveTo, "growx");

        tabbedPane.add(JDLocale.L("gui.linkgrabber.packagetab.toggleview1", "Simple"), simplePanel);

        extendedPanel.setLayout(new MigLayout("", "[]10px[grow][right]", "[][]"));

        extendedPanel.add(new JLabel(JDLocale.L("gui.linkgrabber.packagetab.lbl.password", "Archivpasswort")));
        extendedPanel.add(txtPassword, "growx");
        extendedPanel.add(chbExtract, "wrap");
        extendedPanel.add(new JLabel(JDLocale.L("gui.linkgrabber.packagetab.lbl.comment", "Kommentar")));
        extendedPanel.add(txtComment, "grow");
        extendedPanel.add(chbUseSubdirectory, "wrap");
        extendedPanel.add(new JLabel("Download Passwort"));
        extendedPanel.add(dlPassword, "growx");

        tabbedPane.add(JDLocale.L("gui.linkgrabber.packagetab.toggleview2", "Extended"), extendedPanel);

        this.setLayout(new MigLayout("", "[grow]", "[]"));
        this.add(tabbedPane, "grow");
    }

    public void UpdateEvent(UpdateEvent event) {
        if (event.getSource() instanceof LinkGrabberV2FilePackage && this.fp != null && event.getSource() == fp && event.getID() == UpdateEvent.EMPTY_EVENT) {
            fp.getUpdateBroadcaster().removeUpdateListener(this);
        } else if (event.getSource() instanceof LinkGrabberV2FilePackage && this.fp != null && event.getSource() == fp && event.getID() == UpdateEvent.UPDATE_EVENT) {
            txtName.setText(fp.getName());
            brwSaveTo.setText(fp.getDownloadDirectory());
        }
    }

    public void actionPerformed(ActionEvent e) {
        if (fp != null) {
            if (e.getSource() == txtName) fp.setName(txtName.getText());
            if (e.getSource() == brwSaveTo) fp.setDownloadDirectory(brwSaveTo.getText());
        }
    }

}
