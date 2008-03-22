package jd;

//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team jdownloader@freenet.de
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program  is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSSee the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://wnu.org/licenses/>.





import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import jd.gui.skins.simple.components.BrowseFile;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;

/**
 * Der Installer erscheint nur beim ersten mal STarten der Webstartversion und
 * beim neuinstallieren der webstartversion der User kann Basiceinstellungen
 * festlegen
 * 
 * @author JD-Team
 */
public class Installer extends JDialog implements ActionListener, WindowListener {

    /**
     * 8764525546298642601L
     */
    private static final long serialVersionUID = 8764525546298642601L;

    //private Logger            logger           = JDUtilities.getLogger();

    private JPanel            panel;

    protected Insets          insets           = new Insets(0, 0, 0, 0);

    private BrowseFile        homeDir;

    private BrowseFile        downloadDir;

    private JButton           btnOK;

    private  boolean    aborted          = false;

    /**
     * 
     */
    public Installer() {
        
        super();
        
        File downloadPath=JDUtilities.getJDHomeDirectoryFromEnvironment();
        File installPath=downloadPath;
        setModal(true);
        setLayout(new BorderLayout());

        this.setTitle(JDLocale.L("installer.title","JDownloader Installation"));
        this.setAlwaysOnTop(true);

        setLocation(20, 20);
        panel = new JPanel(new GridBagLayout());

        homeDir = new BrowseFile();
        homeDir.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        homeDir.setEditable(true);
        homeDir.setText(installPath.getAbsolutePath());

        downloadDir = new BrowseFile();
        downloadDir.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        downloadDir.setEditable(true);
        downloadDir.setText(downloadPath.getAbsolutePath());
        addWindowListener(this);
        //JDUtilities.addToGridBag(panel, new JLabel(JDLocale.L("installer.installDir","Install Directory")), GridBagConstraints.RELATIVE, GridBagConstraints.RELATIVE, GridBagConstraints.RELATIVE, 1, 0, 0, insets, GridBagConstraints.NONE, GridBagConstraints.WEST);
        //JDUtilities.addToGridBag(panel, homeDir, GridBagConstraints.RELATIVE, GridBagConstraints.RELATIVE, GridBagConstraints.REMAINDER, 1, 1, 0, insets, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
        JDUtilities.addToGridBag(panel, new JLabel(JDLocale.L("installer.downloadDir","Downloaddirectory")), GridBagConstraints.RELATIVE, GridBagConstraints.RELATIVE, GridBagConstraints.RELATIVE, 1, 0, 0, insets, GridBagConstraints.NONE, GridBagConstraints.WEST);
        JDUtilities.addToGridBag(panel, downloadDir, GridBagConstraints.RELATIVE, GridBagConstraints.RELATIVE, GridBagConstraints.REMAINDER, 1, 1, 0, insets, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
        btnOK = new JButton(JDLocale.L("gui.btn_continue","Continue..."));
        btnOK.addActionListener(this);
        JDUtilities.addToGridBag(panel, btnOK, GridBagConstraints.RELATIVE, GridBagConstraints.RELATIVE, GridBagConstraints.REMAINDER, 1, 0, 0, insets, GridBagConstraints.NONE, GridBagConstraints.WEST);
        this.add(panel, BorderLayout.CENTER);
        this.pack();

        this.setVisible(true);
    }

    public void windowClosing(WindowEvent e) {
        this.aborted=true;
        JOptionPane.showMessageDialog(this, JDLocale.L("installer.aborted","Installation aborted!"));
        homeDir.setText("");
        downloadDir.setText("");
  
        
    }

    public void windowActivated(WindowEvent e) {}

    public void windowClosed(WindowEvent e) {}

    public void windowDeactivated(WindowEvent e) {}

    public void windowDeiconified(WindowEvent e) {}

    public void windowIconified(WindowEvent e) {}

    public void windowOpened(WindowEvent e) {}

    public String getHomeDir() {
      
        new File(homeDir.getText()).mkdirs();
        if (!new File(homeDir.getText()).exists() || !new File(homeDir.getText()).canRead()) {
            this.aborted=true;
            JOptionPane.showMessageDialog(this, JDLocale.L("installer.aborted.dirNotValid","Installation aborted!\r\nInstallation Directory is not valid: ") + homeDir.getText());
            homeDir.setText("");
            downloadDir.setText("");
            this.dispose();
            System.exit(1);
            return null;
        }
        return homeDir.getText();
    }

    public String getDownloadDir() {
        new File(downloadDir.getText()).mkdirs();
        if (!new File(downloadDir.getText()).exists() || !new File(downloadDir.getText()).canWrite()) {
            this.aborted=true;
            JOptionPane.showMessageDialog(this, JDLocale.L("installer.aborted.dirNotValid","Installation aborted!\r\nDownload Directory is not valid: ") + homeDir.getText());
            homeDir.setText("");
            downloadDir.setText("");
            this.dispose();
            System.exit(1);
            return null;
        }

        return downloadDir.getText();
    }

    public void actionPerformed(ActionEvent e) {
  this.setVisible(false);

    }

    public  boolean isAborted() {
        return aborted;
    }

}
