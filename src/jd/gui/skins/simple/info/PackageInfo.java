package jd.gui.skins.simple.info;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;

import jd.config.Configuration;
import jd.config.SubConfiguration;
import jd.gui.skins.simple.components.ComboBrowseFile;
import jd.gui.skins.simple.components.JDFileChooser;
import jd.gui.skins.simple.components.JDTextField;
import jd.gui.skins.simple.components.Linkgrabber.LinkGrabberV2FilePackage;
import jd.gui.skins.simple.components.Linkgrabber.UpdateEvent;
import jd.gui.skins.simple.components.Linkgrabber.UpdateListener;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;
import net.miginfocom.swing.MigLayout;

public class PackageInfo extends JPanel implements UpdateListener {

    private static final long serialVersionUID = 5410296068527460629L;

    private JLabel brwSaveTo;

    private JLabel txtComment;

    private JLabel txtName;

    private JLabel txtPassword;

    private JCheckBox chbExtract;

    private JComboBox cbPrio;
    
    private JTabbedPane tabbedPane;

    private JPanel simplePanel, extendedPanel;

    private boolean isRemoved = false;
    
    private LinkGrabberV2FilePackage fp = null;

    public PackageInfo(SubConfiguration guiConfig) {
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
        simplePanel.setLayout(new MigLayout("", "[]10px[grow, left]", "[][]"));
        
        txtName = new JLabel(fp.getName());
        brwSaveTo = new JLabel(fp.getDownloadDirectory());

        simplePanel.add(new JLabel(JDLocale.L("gui.linkgrabber.packagetab.lbl.name", "Paketname")));
        simplePanel.add(txtName, "growx, wrap");
        simplePanel.add(new JLabel(JDLocale.L("gui.linkgrabber.packagetab.lbl.saveto", "Speichern unter")));
        simplePanel.add(brwSaveTo, "growx");

        tabbedPane.add(JDLocale.L("gui.linkgrabber.packagetab.toggleview1", "Simple"), simplePanel);

        extendedPanel = new JPanel();
        
        extendedPanel.setLayout(new MigLayout("", "[]10px[grow][right]", "[][]"));
      
        txtPassword = new JLabel(fp.getPassword());
        txtComment = new JLabel(fp.getComment());
        
        chbExtract = new JCheckBox(JDLocale.L("gui.linkgrabber.packagetab.chb.extractAfterdownload", "Extract"));
        chbExtract.setSelected(true);
        chbExtract.setEnabled(false);
        chbExtract.setHorizontalTextPosition(SwingConstants.LEFT);

        cbPrio = new JComboBox(new Integer[] { 4, 3, 2, 1, 0, -1, -2, -3, -4});
                
        extendedPanel.add(new JLabel(JDLocale.L("gui.linkgrabber.packagetab.lbl.password", "Archivpasswort")));
        extendedPanel.add(txtPassword, "growx");
        extendedPanel.add(chbExtract, "wrap");
        extendedPanel.add(new JLabel(JDLocale.L("gui.linkgrabber.packagetab.lbl.comment", "Kommentar")));
        extendedPanel.add(txtComment, "grow");
        extendedPanel.add(cbPrio, "wrap");

        tabbedPane.add(JDLocale.L("gui.linkgrabber.packagetab.toggleview2", "Extended"), extendedPanel);

        this.setLayout(new MigLayout("", "[grow]", "[]"));
        this.add(tabbedPane, "grow");
    }

    public void UpdateEvent(UpdateEvent event) {
        if (event.getSource() instanceof LinkGrabberV2FilePackage && this.fp != null && event.getSource() == fp && event.getID() == UpdateEvent.EMPTY_EVENT) {
            fp.getUpdateBroadcaster().removeUpdateListener(this);
            fp = null;
        } else if (event.getSource() instanceof LinkGrabberV2FilePackage && this.fp != null && event.getSource() == fp && event.getID() == UpdateEvent.UPDATE_EVENT) {
            txtName.setText(fp.getName());
            brwSaveTo.setText(fp.getDownloadDirectory());
        }
    }

}
