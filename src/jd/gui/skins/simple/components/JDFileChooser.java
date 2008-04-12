//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team jdownloader@freenet.de
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.


package jd.gui.skins.simple.components;

import java.io.File;
import java.util.logging.Logger;

import javax.swing.JFileChooser;

import jd.utils.JDUtilities;

/**
 * 
 * @author JD-Team
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