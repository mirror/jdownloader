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

package jd.gui.skins.simple;

import java.util.logging.Logger;

import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

import jd.utils.JDLocale;
import jd.utils.JDUtilities;

/**
 * Diese Klasse zeigt dem Nutzer Auswahldialoge beim Konvertieren von FLVs an
 */
public class ConvertDialog extends JFrame {

    private static Logger logger = JDUtilities.getLogger();
    /**
     * 
     */
    private static final long serialVersionUID = -9146764850581039090L;
    
    
    public static enum ConversionMode { 
    	AUDIOMP3("Audio (MP3)",new String[]{".mp3"}), 
    	VIDEOFLV("Video (FLV)",new String[]{".flv"}), 
    	AUDIOMP3_AND_VIDEOFLV("Audio & Video (MP3 & FLV)",new String[]{".mp3",".flv"}), 
    	VIDEOMP4("Video (MP4)",new String[]{".mp4"}), 
    	VIDEO3GP("Video (3GP)",new String[]{".3gp"});

    String text;
    String[] ext;
    
    ConversionMode(String text,String[] ext) {
        this.text = text;
        this.ext = ext;
    }
    
    
    @Override
    public String toString() {
        return this.text;
    }
    
    public String GetText() {
        return this.text;
    }
    
    public String[] GetExtArray() {
        return this.ext;
    }
    
    public String GetExt(int i) {
        return this.ext[i];
    }
    
    public String GetExtFirst() {
        return this.ext[0];
    }
        
    };
    
    private static boolean keepformat = false;
    private static ConversionMode keeped;
    
    public static ConversionMode DisplayDialog(Object[] displaymodes) {
    	logger.fine(displaymodes.length + " Convertmodi zur Auswahl.");
    	if(keepformat)
    	{
    		//Es muss überprüft werden, ob das Format überhaupt zur Auswahl steht.	
    		for(int i=0;i<displaymodes.length;i++)
    		{
    			if(displaymodes[i].equals(keeped))
    			{
    				return keeped;
    				
    			}
    		}
    	}
    	if(displaymodes.length == 1) // Nur eine Auswahl
    	{
    		return (ConversionMode) displaymodes[0];
    	}
    	
        JCheckBox checkBox = new JCheckBox(JDLocale.L("convert.dialog.keepformat", "Format für diese Sitzung beibehalten"));
        checkBox.setSelected(false);
        ConversionMode selectedValue = (ConversionMode) JOptionPane.showInputDialog(null, checkBox, JDLocale.L("convert.dialog.chooseformat", "Wähle das Dateiformat:"), JOptionPane.QUESTION_MESSAGE, null, displaymodes, displaymodes[0]);
        
        System.out.println("selectedValue: " + selectedValue);
        if(checkBox.isSelected())
        {
        	keepformat = true;
        	keeped = selectedValue;
        }
        else
        {
        	keepformat = false;
        }
        return selectedValue;
        
        /*
        if      (selectedValue == ConversionMode.AUDIOMP3); // do something
        else if (selectedValue == ConversionMode.VIDEOMP4); // do something
        else if (selectedValue == ConversionMode.THREEGP); // do something
        else if (selectedValue == ConversionMode.VIDEOFLV); // do something
        else if (selectedValue == ConversionMode.AUDIOMP3_AND_VIDEOFLV); // do something
        else ; // User pressed cancel       
        */
       
    }
    
    /*public static void main(String[] args)
    {
    	//ConversionMode[] mode = {ConversionMode.AudioMp3,ConversionMode.Video_Flv,ConversionMode.AudioMp3andVideoFlv};
    	DisplayDialog(new ConversionMode[]{ConversionMode.AUDIOMP3,ConversionMode.VIDEOFLV,ConversionMode.AUDIOMP3_AND_VIDEOFLV});
        DisplayDialog(new ConversionMode[]{ConversionMode.VIDEOFLV,ConversionMode.AUDIOMP3_AND_VIDEOFLV});
        DisplayDialog(new ConversionMode[]{ConversionMode.AUDIOMP3_AND_VIDEOFLV});
    }/**/
    
    

}
