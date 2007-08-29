package jd.gui.skins.simple.config;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import jd.Configuration;
import jd.JDUtilities;

public class ConfigPanelGeneral extends JPanel{
    /**
     * serialVersionUID
     */
    private static final long serialVersionUID = 3383448498625377495L;
    private JLabel lblDownloadDir;
    private JLabel lblHomeDir;
    private JLabel lblUpdate;
    private JTextField txtDownloadDir;
    private JTextField txtHomeDir;
    private JButton btnUpdate;
    private JPanel panel;
    
    private Configuration configuration;
    
    ConfigPanelGeneral(Configuration configuration){
        this.configuration = configuration;
        panel = new JPanel(new GridBagLayout());
        
        lblDownloadDir = new JLabel("Download Verzeichnis");
        lblHomeDir     = new JLabel("JD Home");
        lblUpdate      = new JLabel("Java Anti Captcha updaten");
        txtHomeDir     = new JTextField(JDUtilities.getHomeDirectory(), 30);
        txtDownloadDir = new JTextField(configuration.getDownloadDirectory(),30);
        btnUpdate = new JButton("Update");
        Insets insets = new Insets(1,5,1,5);
        
        int row=0;
        JDUtilities.addToGridBag(panel, lblDownloadDir, 0, row, 1, 1, 1, 1, insets, GridBagConstraints.NONE, GridBagConstraints.EAST);
        JDUtilities.addToGridBag(panel, txtDownloadDir, 1, row, 1, 1, 1, 1, insets, GridBagConstraints.NONE, GridBagConstraints.WEST);
        row++;
        if(JDUtilities.getHomeDirectory() != null){
            JDUtilities.addToGridBag(panel, lblHomeDir, 0, row, 1, 1, 1, 1, insets, GridBagConstraints.NONE, GridBagConstraints.EAST);
            JDUtilities.addToGridBag(panel, txtHomeDir, 1, row, 1, 1, 1, 1, insets, GridBagConstraints.NONE, GridBagConstraints.WEST);
            row++;
        }
        JDUtilities.addToGridBag(panel, lblUpdate, 0, row, 1, 1, 1, 1, insets, GridBagConstraints.NONE, GridBagConstraints.EAST);
        JDUtilities.addToGridBag(panel, btnUpdate, 1, row, 1, 1, 1, 1, insets, GridBagConstraints.NONE, GridBagConstraints.WEST);
        row++;
        add(panel);
    }
    void save(){
        configuration.setDownloadDirectory(txtDownloadDir.getText().trim());
        if(JDUtilities.getHomeDirectory() != null)
            JDUtilities.writeJDHomeDirectoryToWebStartCookie(txtHomeDir.getText().trim());
    }
}
