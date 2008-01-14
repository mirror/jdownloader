package jd.gui.skins.simple.components;

import java.io.File;
import java.util.logging.Logger;

import javax.swing.JFileChooser;

import jd.utils.JDUtilities;

/**
 * 
 * @author coalado
 * 
 * EinWrapper um JFileChooser
 */
public class JDFileChooser extends JFileChooser {
    /**
	 * 
	 */
	private static final long serialVersionUID = 3315263822025280362L;
	private Logger logger;
    private String fcID;

    public JDFileChooser() {
        super();
        logger = JDUtilities.getLogger();
   
        this.setCurrentDirectory(JDUtilities.getCurrentWorkingDirectory(null));

    }
    /**
     * Über die id kann eine ID für den filechooser ausgewählt werden . JD Fielchooser merkt sich für diese id den zuletzt verwendeten pfad
     * @param id
     */
    public JDFileChooser(String id) {
        super();
        this.fcID=id;
        logger = JDUtilities.getLogger();
   
        this.setCurrentDirectory(JDUtilities.getCurrentWorkingDirectory(fcID));

    }
    // public int showOpenDialog(JFrame frame){
    // int ret= super.showOpenDialog(frame);
    //    
    // return ret;
    //    
    //    
    // }
    public File getSelectedFile() {
       
        File ret = super.getSelectedFile();
        if (ret == null) return null;
        if (ret.isDirectory()) {
            JDUtilities.setCurrentWorkingDirectory(ret,fcID);
            logger.info("Save working path in :" + ret);
        }
        else {
            JDUtilities.setCurrentWorkingDirectory(ret.getParentFile(),fcID);

            logger.info("Save working path in :" + ret.getParentFile());
        }
        logger.info("Get Working path to: " + JDUtilities.getCurrentWorkingDirectory(fcID));
        JDUtilities.saveConfig();
        return ret;
    }

}