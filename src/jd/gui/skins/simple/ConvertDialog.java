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

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.logging.Logger;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import jd.utils.JDLocale;
import jd.utils.JDUtilities;

/**
 * Diese Klasse zeigt dem Nutzer Auswahldialoge beim Konvertieren von FLVs an
 */
public class ConvertDialog extends JFrame {
    @SuppressWarnings("unused")
    private static Logger logger = JDUtilities.getLogger();
    /**
     * 
     */
    private static final long serialVersionUID = -9146764850581039090L;
    
    public static final int CONVERT_ID_AUDIO_MP3 = 0; 
    public static final int CONVERT_ID_VIDEO_FLV = 1;
    public static final int CONVERT_ID_AUDIO_MP3_AND_VIDEO_FLV = 2;
    public static final int CONVERT_ID_MP4 = 3;
    public static final int CONVERT_ID_3GP = 4;
    
    
    
    // benutze keine int constanten sondern lieber ein Enum!
    public static enum ConversionMode {AudioMp3("Audio (MP3)"), Video_Flv("Video (FLV)"), AudioMp3andVideoFlv("Audio und Video (MP3 & FLV)"), Mp4("Video (MP4)"), ThreeGP("Video (3GP)");

    String text;
    
    ConversionMode(String text) {
        this.text = text;
    }
    
    @Override
    public String toString() {
        return this.text;
    }
        
    };
    

    public static void main(String[] args) {
        JCheckBox checkBox = new JCheckBox("Format für diese Sitzung beibehalten");
        Object selectedValue = JOptionPane.showInputDialog(null, checkBox, "Waehle das Dateiformat?", JOptionPane.QUESTION_MESSAGE, null, ConversionMode.values(), ConversionMode.values()[0]);
        if      (selectedValue == ConversionMode.AudioMp3); // do something
        else if (selectedValue == ConversionMode.Mp4); // do something
        else if (selectedValue == ConversionMode.ThreeGP); // do something
        else if (selectedValue == ConversionMode.Video_Flv); // do something
        else if (selectedValue == ConversionMode.AudioMp3andVideoFlv); // do something
        else ; // User pressed cancel       
        
        System.out.println("selectedValue: " + selectedValue);
    }
    
    

}
