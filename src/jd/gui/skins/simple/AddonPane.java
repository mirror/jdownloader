//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import jd.controlling.JDLogHandler;
import jd.controlling.JDLogger;
import jd.event.ControlEvent;
import jd.event.ControlListener;
import jd.gui.skins.simple.components.JDFileChooser;
import jd.gui.skins.simple.components.JHelpDialog;
import jd.gui.skins.simple.components.JLinkButton;
import jd.gui.skins.simple.tasks.LogTaskPane;
import jd.http.Encoding;
import jd.nutils.io.JDIO;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;
import jd.utils.Upload;
import net.miginfocom.swing.MigLayout;

/**
 * Ein Dialog, der Logger-Output anzeigen kann.
 * 
 */
public class AddonPane extends JTabbedPanel implements ActionListener {

    
    public AddonPane(Logger logger) {
       
        this.setLayout(new MigLayout("ins 3", "[fill,grow]", "[fill,grow]"));

    }

    public void actionPerformed(ActionEvent e) {

     

    }


    @Override
    public void onDisplay() {
      
    }

    @Override
    public void onHide() {
        
    }

   

}
