package jd.gui.skins.simple.components.Linkgrabber;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

import jd.config.Configuration;
import jd.config.SubConfiguration;
import jd.gui.skins.simple.components.ComboBrowseFile;
import jd.gui.skins.simple.components.JDFileChooser;
import jd.gui.skins.simple.components.JDTextField;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;

public class LinkGrabberV2FilePackageInfo extends JPanel implements ActionListener {

    private ComboBrowseFile brwSaveTo;

    private JButton btnToggle;

    private JDTextField txtComment;

    private JDTextField txtName;

    private JDTextField txtPassword;

    private JCheckBox chbExtract;

    private JCheckBox chbUseSubdirectory;
    private SubConfiguration guiConfig;

    private static final String PROPERTY_HEADERVIEW = "PROPERTY_HEADERVIEW";

    public LinkGrabberV2FilePackageInfo(SubConfiguration guiConfig) {
        this.guiConfig = guiConfig;
        // setPreferredSize(new Dimension(700, 350));
        initGUIElements();
        buildGUI();
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == btnToggle) {
            rebuildGUI();
        }
    }

    public void updatePackage(LinkGrabberV2FilePackage fp) {
        txtName.setText(fp.getName());
        brwSaveTo.setText(fp.getDownloadDirectory());
    }

    private void initGUIElements() {
        btnToggle = new JButton();
        if (guiConfig.getBooleanProperty(PROPERTY_HEADERVIEW, true)) {
            btnToggle.setText(JDLocale.L("gui.linkgrabber.packagetab.toggleview2", "Simple"));
        } else {
            btnToggle.setText(JDLocale.L("gui.linkgrabber.packagetab.toggleview1", "Extended"));
        }
        btnToggle.setMinimumSize(new Dimension(80, 23));
        btnToggle.setPreferredSize(new Dimension(80, 23));
        btnToggle.setMaximumSize(new Dimension(80, 23));

        txtName = new JDTextField();
        txtName.setAutoSelect(true);
        txtPassword = new JDTextField();
        txtComment = new JDTextField();

        chbExtract = new JCheckBox(JDLocale.L("gui.linkgrabber.packagetab.chb.extractAfterdownload", "Extract"));
        chbExtract.setSelected(true);
        chbExtract.setHorizontalTextPosition(SwingConstants.LEFT);

        chbUseSubdirectory = new JCheckBox(JDLocale.L("gui.linkgrabber.packagetab.chb.useSubdirectory", "Use Subdirectory"));
        chbUseSubdirectory.setSelected(JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_USE_PACKETNAME_AS_SUBFOLDER, false));
        chbUseSubdirectory.setHorizontalTextPosition(SwingConstants.LEFT);

        brwSaveTo = new ComboBrowseFile("DownloadSaveTo");
        brwSaveTo.setEditable(true);
        brwSaveTo.setFileSelectionMode(JDFileChooser.DIRECTORIES_ONLY);
        brwSaveTo.setText(JDUtilities.getConfiguration().getDefaultDownloadDirectory());

        btnToggle.addActionListener(this);
    }

    private void buildGUI() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                int n = 10;
                setLayout(new BorderLayout(n, n));
                setBorder(new EmptyBorder(n, n, n, n));
                add(guiConfig.getBooleanProperty(PROPERTY_HEADERVIEW, true) ? buildExtendedHeader() : buildSimpleHeader(), BorderLayout.NORTH);
            }
        });
    }

    private void rebuildGUI() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                removeAll();
                add(changeHeader(), BorderLayout.NORTH);
            }
        });
    }

    private JPanel changeHeader() {
        if (guiConfig.getBooleanProperty(PROPERTY_HEADERVIEW, true)) {
            guiConfig.setProperty(PROPERTY_HEADERVIEW, false);
            guiConfig.save();
            btnToggle.setText(JDLocale.L("gui.linkgrabber.packagetab.toggleview1", "Extended"));
            return buildSimpleHeader();
        } else {
            guiConfig.setProperty(PROPERTY_HEADERVIEW, true);
            guiConfig.save();
            btnToggle.setText(JDLocale.L("gui.linkgrabber.packagetab.toggleview2", "Simple"));
            return buildExtendedHeader();
        }
    }

    private JPanel buildSimpleHeader() {
        int n = 10;

        JPanel titles = new JPanel(new GridLayout(0, 1, n / 2, n / 2));
        titles.add(new JLabel(JDLocale.L("gui.linkgrabber.packagetab.lbl.name", "Paketname")));
        titles.add(new JLabel(JDLocale.L("gui.linkgrabber.packagetab.lbl.saveto", "Speichern unter")));
        titles.add(new JLabel(JDLocale.L("gui.linkgrabber.packagetab.lbl.password", "Archivpasswort")));

        JPanel panel1 = new JPanel(new BorderLayout(n / 2, n / 2));
        panel1.add(txtName, BorderLayout.CENTER);
        panel1.add(btnToggle, BorderLayout.EAST);

        JPanel elements = new JPanel(new GridLayout(0, 1, n / 2, n / 2));
        elements.add(panel1);
        elements.add(brwSaveTo);
        elements.add(txtPassword);

        JPanel header = new JPanel(new BorderLayout(n, n));
        header.add(titles, BorderLayout.WEST);
        header.add(elements, BorderLayout.CENTER);
        return header;
    }

    private JPanel buildExtendedHeader() {
        int n = 10;

        JPanel titles = new JPanel(new GridLayout(0, 1, n / 2, n / 2));
        titles.add(new JLabel(JDLocale.L("gui.linkgrabber.packagetab.lbl.name", "Paketname")));
        titles.add(new JLabel(JDLocale.L("gui.linkgrabber.packagetab.lbl.saveto", "Speichern unter")));
        titles.add(new JLabel(JDLocale.L("gui.linkgrabber.packagetab.lbl.password", "Archivpasswort")));
        titles.add(new JLabel(JDLocale.L("gui.linkgrabber.packagetab.lbl.comment", "Kommentar")));

        JPanel panel1 = new JPanel(new BorderLayout(n / 2, n / 2));
        panel1.add(txtPassword, BorderLayout.CENTER);
        panel1.add(chbExtract, BorderLayout.EAST);

        JPanel panel2 = new JPanel(new BorderLayout(n / 2, n / 2));
        panel2.add(txtComment, BorderLayout.CENTER);
        panel2.add(chbUseSubdirectory, BorderLayout.EAST);

        JPanel panel3 = new JPanel(new BorderLayout(n / 2, n / 2));
        panel3.add(txtName, BorderLayout.CENTER);
        panel3.add(btnToggle, BorderLayout.EAST);

        JPanel elements = new JPanel(new GridLayout(0, 1, n / 2, n / 2));
        elements.add(panel3);
        elements.add(brwSaveTo);
        elements.add(panel1);
        elements.add(panel2);

        JPanel header = new JPanel(new BorderLayout(n, n));
        header.add(titles, BorderLayout.WEST);
        header.add(elements, BorderLayout.CENTER);
        return header;
    }

}
