package jd.gui.skins.simple.config;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JTextField;

import jd.Configuration;
import jd.JDUtilities;

public class ConfigPanelGeneral extends JComponent{
    /**
     * serialVersionUID
     */
    private static final long serialVersionUID = 3383448498625377495L;
    private JLabel lblDownloadDir;
    private JTextField txtDownloaderDir;
    
    private Configuration configuration;
    
    ConfigPanelGeneral(Configuration configuration){
        this.configuration = configuration;
        setLayout(new GridBagLayout());
        
        lblDownloadDir = new JLabel("Download Verzeichnis");
        txtDownloaderDir = new JTextField(configuration.getDownloadDirectory());
        
        JDUtilities.addToGridBag(this, lblDownloadDir,   0, 0, 1, 1, 1, 1, null, GridBagConstraints.NONE,       GridBagConstraints.EAST);
        JDUtilities.addToGridBag(this, txtDownloaderDir, 1, 0, 1, 1, 1, 1, null, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    }
    void save(){
        configuration.setDownloadDirectory(txtDownloaderDir.getText());
    }
}
