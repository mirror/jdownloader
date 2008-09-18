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
 *         Ein Wrapper um JFileChooser
 */
public class JDFileChooser extends JFileChooser {

    private static final long serialVersionUID = 3315263822025280362L;
    private String fcID;
    @SuppressWarnings("unused")
    private Logger logger = JDUtilities.getLogger();

    public JDFileChooser() {
        super();
        setCurrentDirectory(JDUtilities.getCurrentWorkingDirectory(null));
    }

    /**
     * Über die id kann eine ID für den filechooser ausgewählt werden . JD
     * Fielchooser merkt sich für diese id den zuletzt verwendeten pfad
     * 
     * @param id
     */
    public JDFileChooser(String id) {
        super();
        fcID = id;
        setCurrentDirectory(JDUtilities.getCurrentWorkingDirectory(fcID));
    }

    @Override
    public File getSelectedFile() {
        File ret = super.getSelectedFile();
   
        if (ret == null) return null;
        if (ret.isDirectory()) {
            JDUtilities.setCurrentWorkingDirectory(ret, fcID);
        } else {
            JDUtilities.setCurrentWorkingDirectory(ret.getParentFile(), fcID);
        }
        return ret;
    }
    @Override
    public File[] getSelectedFiles() {
        File[] ret = super.getSelectedFiles();
   
        if (ret == null||ret.length==0) return null;
        if (ret[0].isDirectory()) {
            JDUtilities.setCurrentWorkingDirectory(ret[0], fcID);
        } else {
            JDUtilities.setCurrentWorkingDirectory(ret[0].getParentFile(), fcID);
        }
        return ret;
    }
}