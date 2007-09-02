package jd.gui.skins.simple.config;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.logging.Level;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import jd.Configuration;
import jd.JDUtilities;
import jd.captcha.JAntiCaptcha;
import jd.plugins.Plugin;

public class ConfigPanelGeneral extends JPanel implements ActionListener{
    /**
     * serialVersionUID
     */
    private static final long serialVersionUID = 3383448498625377495L;
    private JLabel lblDownloadDir;
    private JLabel lblHomeDir;
    private JLabel lblUpdate;
    private JLabel lblLoggerLevel;
    private JTextField txtDownloadDir;
    private JTextField txtHomeDir;
    private JComboBox cboLoggerLevel;
    private JButton btnUpdate;
    private JPanel panel;
    
    private Configuration configuration;
    
    ConfigPanelGeneral(Configuration configuration){
        this.configuration = configuration;
        panel = new JPanel(new GridBagLayout());
        
        lblDownloadDir = new JLabel("Download Verzeichnis");
        lblHomeDir     = new JLabel("JD Home");
        lblUpdate      = new JLabel("Java Anti Captcha updaten");
        lblLoggerLevel = new JLabel("Level für's Logging");
        txtHomeDir     = new JTextField(JDUtilities.getHomeDirectory(), 30);
        txtDownloadDir = new JTextField(configuration.getDownloadDirectory(),30);
        cboLoggerLevel = new JComboBox();
        btnUpdate = new JButton("Update");
        btnUpdate.addActionListener(this);
        Insets insets = new Insets(1,5,1,5);
        
        cboLoggerLevel.addItem(Level.ALL);
        cboLoggerLevel.addItem(Level.FINEST);
        cboLoggerLevel.addItem(Level.FINER);
        cboLoggerLevel.addItem(Level.FINE);
        cboLoggerLevel.addItem(Level.INFO);
        cboLoggerLevel.addItem(Level.WARNING);
        cboLoggerLevel.addItem(Level.SEVERE);
        cboLoggerLevel.addItem(Level.OFF);
        cboLoggerLevel.setSelectedItem(Plugin.getLogger().getLevel());

        int row=0;
        JDUtilities.addToGridBag(panel, lblLoggerLevel, 0, row, 1, 1, 1, 1, insets, GridBagConstraints.NONE, GridBagConstraints.EAST);
        JDUtilities.addToGridBag(panel, cboLoggerLevel, 1, row, 1, 1, 1, 1, insets, GridBagConstraints.NONE, GridBagConstraints.WEST);
        row++;
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
        add(panel);
    }
    void save(){
        Level loggerLevel = (Level)cboLoggerLevel.getSelectedItem();
        configuration.setDownloadDirectory(txtDownloadDir.getText().trim());
        configuration.setLoggerLevel(loggerLevel.getName());
        Plugin.getLogger().setLevel(loggerLevel);
        if(JDUtilities.getHomeDirectory() != null)
            JDUtilities.writeJDHomeDirectoryToWebStartCookie(txtHomeDir.getText().trim());
    }
    public void actionPerformed(ActionEvent e) {
        if(e.getSource() == btnUpdate){
            JAntiCaptcha.updateMethods();
        }
    }
    
}
