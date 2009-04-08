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

import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.StreamHandler;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import jd.controlling.JDLogHandler;
import jd.controlling.JDLogger;
import jd.controlling.LogFormatter;
import jd.event.ControlEvent;
import jd.event.ControlListener;
import jd.gui.skins.simple.components.JDFileChooser;
import jd.gui.skins.simple.components.JHelpDialog;
import jd.gui.skins.simple.components.JLinkButton;
import jd.gui.skins.simple.components.TextAreaDialog;
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
public class LogPane extends JTabbedPanel implements ActionListener, ControlListener {

 
    private static final long serialVersionUID = -5753733398829409112L;

    /**
     * JTextField wo der Logger Output eingetragen wird
     */
    private JTextArea logField;

    /**
     * Primary Constructor
     * 
     * @param logger
     *            The connected Logger
     */
    public LogPane(Logger logger) {
        this.setName("LOGDIALOG");
        this.setLayout(new MigLayout("ins 3", "[fill,grow]", "[fill,grow]"));

  

        logField = new JTextArea(10, 60);

        add(new JScrollPane(logField));
    }

    public void actionPerformed(ActionEvent e) {

        switch (e.getID()) {
        case LogTaskPane.ACTION_LEVEL:
            onHide();
            onDisplay();
            break;
        case LogTaskPane.ACTION_SAVE:
            JFileChooser fc = new JFileChooser();
            fc.setApproveButtonText(JDLocale.L("gui.btn_save", "Save"));
            fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
            if (fc.showOpenDialog(this) == JDFileChooser.APPROVE_OPTION) {
                File ret = fc.getSelectedFile();
                if (ret != null) {
                    String content = toString();
                    JDIO.writeLocalFile(ret, content);
                    jd.controlling.JDLogger.getLogger().info("Log saved to file: " + ret.getAbsolutePath());
                }
            }
            break;
        case LogTaskPane.ACTION_UPLOAD:
            Level level = jd.controlling.JDLogger.getLogger().getLevel();
            if (!level.equals(Level.ALL)) {
                try {
                    JHelpDialog.showHelpMessage(SimpleGUI.CURRENTGUI, null, JDLocale.LF("gui.logdialog.loglevelwarning", "The selected loglevel (%s) isn't preferred to upload a log! Please change it to ALL and create a new log!", level.getName()), new URL("http://jdownloader.org/knowledge/wiki/support/create-a-jd-log"), null, 30);
                } catch (MalformedURLException e1) {
                    e1.printStackTrace();
                }
            }
            String content = logField.getSelectedText();
            if (content == null || content.length() == 0) {
                content = Encoding.UTF8Encode(logField.getText());
            }
            content = TextAreaDialog.showDialog(SimpleGUI.CURRENTGUI, JDLocale.L("gui.logdialog.edittitle", "Edit Log"), JDLocale.L("gui.logdialog.yourlog", "Hochgeladener Log: Editieren möglich!"), content);

            if (content == null || content.length() == 0) return;

            String name = JOptionPane.showInputDialog(this, JDLocale.L("gui.askName", "Your name?"));
            if (name == null) return;
            String question = JOptionPane.showInputDialog(this, JDLocale.L("gui.logger.askQuestion", "Please describe your Problem/Bug/Question!"));
            if (question == null) return;

            String url = Upload.toJDownloader(content, name + "\r\n\r\n" + question);
            if (url != null) {
                if (JOptionPane.showInputDialog(this, JDLocale.L("gui.logDialog.logLink", "Log-Link (click ok to open)"), url) != null) {
                    try {
                        JLinkButton.openURL(url);
                    } catch (Exception e1) {
                        e1.printStackTrace();
                    }
                }
            } else {
                JOptionPane.showMessageDialog(this, JDLocale.L("gui.logDialog.warning.uploadFailed", "Upload failed"));
            }

            break;
        }

    }

    @Override
    public String toString() {
        String content = logField.getSelectedText();
        if (content == null || content.length() == 0) {
            content = logField.getText();
        }
        return content;
    }

    @Override
    public void onDisplay() {
        /*
         * enable autoscrolling by setting the caret to the last position
         */
        SimpleGUI.CURRENTGUI.setWaiting(true);
       JDUtilities.getController().addControlListener(this);
       ArrayList<LogRecord> buff = JDLogHandler.getHandler().getBuffer();
       StringBuilder sb=new StringBuilder();
       for(LogRecord lr:buff){
           if(lr.getLevel().intValue()>=JDLogger.getLogger().getLevel().intValue())
           sb.append( JDLogHandler.getHandler().getFormatter().format(lr));
//           sb.append("\r\n");
       }
       logField.setText(sb.toString());
       SimpleGUI.CURRENTGUI.setWaiting(false);
       
    }

    @Override
    public void onHide() {
        JDUtilities.getController().removeControlListener(this);
    }

    public void controlEvent(ControlEvent event) {
       if(event.getID()==ControlEvent.CONTROL_LOG_OCCURED){
  
           logField.append(JDLogHandler.getHandler().getFormatter().format((LogRecord)event.getParameter()));
//           logField.append("\r\n");
       }
        
    }

}
