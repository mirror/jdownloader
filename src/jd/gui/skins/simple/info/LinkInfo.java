package jd.gui.skins.simple.info;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;

import jd.gui.skins.simple.components.ComboBrowseFile;
import jd.gui.skins.simple.components.JDTextField;
import jd.plugins.FilePackage;
import jd.utils.JDLocale;
import net.miginfocom.swing.MigLayout;

public class LinkInfo extends JPanel {

    private static final long serialVersionUID = 5410296068527460629L;

    private JDTextField brwSaveTo;

    private JDTextField txtComment;

    private JDTextField txtName;

    private JDTextField txtPassword;

    private JCheckBox chbExtract;

    private JComboBox cbPrio;
    
    private JTabbedPane tabbedPane;

    private JPanel simplePanel, extendedPanel;    
    
    private FilePackage fp = null;

    public LinkInfo() {
        buildGui();
        fp = null;
    }

    public void setPackage(FilePackage fp) {
        //if (this.fp != null) this.fp.getUpdateBroadcaster().removeUpdateListener(this);
        this.fp = fp;
        if (this.fp != null) {
            //fp.getUpdateBroadcaster().addUpdateListener(this);
            txtName.setText(fp.getName());
            brwSaveTo.setText(fp.getDownloadDirectory());
            txtPassword.setText(fp.getPassword());
            txtComment.setText(fp.getComment());
//            chbExtract.setSelected(fp.get);

        }
    }

    public FilePackage getPackage() {
        return fp;
    }

    private void buildGui() {
        tabbedPane = new JTabbedPane();

        simplePanel = new JPanel();
        simplePanel.setLayout(new MigLayout("", "[]10px[grow, left]", "[][]"));
        
        txtName = new JDTextField();
        txtName.setEnabled(false);
        brwSaveTo = new JDTextField();
        brwSaveTo.setEnabled(false);

        simplePanel.add(new JLabel(JDLocale.L("gui.linkgrabber.packagetab.lbl.name", "Paketname")));
        simplePanel.add(txtName, "growx, wrap");
        simplePanel.add(new JLabel(JDLocale.L("gui.linkgrabber.packagetab.lbl.saveto", "Speichern unter")));
        simplePanel.add(brwSaveTo, "growx");

        tabbedPane.add(JDLocale.L("gui.linkgrabber.packagetab.toggleview1", "Simple"), simplePanel);


        cbPrio = new JComboBox(new Integer[] { 4, 3, 2, 1, 0, -1, -2, -3, -4});
        cbPrio.setSelectedIndex(4);
                

        this.setLayout(new MigLayout("", "[grow]", "[]"));
        this.add(tabbedPane, "grow");
    }

//    public void UpdateEvent(UpdateEvent event) {
//        if (event.getSource() instanceof LinkGrabberV2FilePackage && this.fp != null && event.getSource() == fp && event.getID() == UpdateEvent.EMPTY_EVENT) {
//            //fp.getUpdateBroadcaster().removeUpdateListener(this);
//            fp = null;
//        } else if (event.getSource() instanceof LinkGrabberV2FilePackage && this.fp != null && event.getSource() == fp && event.getID() == UpdateEvent.UPDATE_EVENT) {
//            txtName.setText(fp.getName());
//            brwSaveTo.setText(fp.getDownloadDirectory());
//        }
//    }

}
