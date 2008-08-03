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

package jd.plugins.optional.jdchat;

import java.awt.BorderLayout;
import java.awt.Insets;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.WindowConstants;

import jd.gui.skins.simple.LocationListener;
import jd.gui.skins.simple.components.TextAreaDialog;
import jd.gui.skins.simple.SimpleGUI;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;

/**
 * Diese Klasse kümmert sich um alle Teamviewer Handlings im JDChat
 */
public class TeamViewer {

    //private static Logger logger = JDUtilities.getLogger();
    /**
     * 
     */
    private static final long serialVersionUID = -9146764850581039090L;
    


    protected Logger logger = JDUtilities.getLogger();
    private String id, pw;
    
    
    public static String[] handleTeamviewer(){	
    		return AskForTeamviewerIDPW(); 		
    }
    
    public static String[] AskForTeamviewerIDPW() {
    	System.out.println("Ask for Teamviewer ID & PW.");
    	
    	
    	
        String[] abc = SimpleGUI.CURRENTGUI.showTextAreaDialog(JDLocale.L("plugin.optional.jdchat.teamviewer.yourtvdata", "Deine Teamviewer Daten:"), "ID:", "PW:", "", "");
    	 
        

        return abc;
       
    }
    
    public static void main(String[] args)
    {
    	//ConversionMode[] mode = {ConversionMode.AudioMp3,ConversionMode.Video_Flv,ConversionMode.AudioMp3andVideoFlv};
    	AskForTeamviewerIDPW();

    }

}
