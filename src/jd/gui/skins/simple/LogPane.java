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
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.StreamHandler;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import jd.gui.skins.simple.components.JDFileChooser;
import jd.gui.skins.simple.components.JHelpDialog;
import jd.gui.skins.simple.components.JLinkButton;
import jd.gui.skins.simple.components.TextAreaDialog;
import jd.gui.skins.simple.tasks.LogTaskPane;
import jd.http.Encoding;
import jd.nutils.io.JDIO;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;
import jd.utils.LogFormatter;
import jd.utils.Upload;
import net.miginfocom.swing.MigLayout;

/**
 * Ein Dialog, der Logger-Output anzeigen kann.
 * 
 * @author Tom
 */
public class LogPane extends JTabbedPanel implements ActionListener {

    /**
     * Ein OutputStream, der die Daten an das log field weiterleitet
     */
    private class LogStream extends OutputStream {

        @Override
        public void write(final int b) throws IOException {
            // Another example where some non-EDT Thread accesses calls a Swing
            // method. This is forbidden and might bring the whole app down.
            // more info:
            // http://java.sun.com/products/jfc/tsc/articles/threads/threads1.html
            // and: http://en.wikipedia.org/wiki/Event_dispatching_thread
            if (logField != null) {
                EventQueue.invokeLater(new Runnable() {
                    public void run() {
                        logField.append(String.valueOf((char) b));
                    }
                });
            }
        }

    }

    /**
     * Handler der einen OutputStream unterstuetzt basierend auf einem
     * ConsoleHandler
     */
    private class LogStreamHandler extends StreamHandler {

        public LogStreamHandler(OutputStream stream) {
            setOutputStream(stream);
        }

        /**
         * Publish a <tt>LogRecord</tt>.
         * <p>
         * The logging request was made initially to a <tt>Logger</tt> object,
         * which initialized the <tt>LogRecord</tt> and forwarded it here.
         * <p>
         * 
         * @param record
         *            description of the log event. A null record is silently
         *            ignored and is not published
         */
        @Override
        public void publish(LogRecord record) {
            super.publish(record);
            flush();
        }

    }

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

        LogStreamHandler streamHandler = new LogStreamHandler(new PrintStream(new LogStream()));
        streamHandler.setLevel(Level.ALL);
        streamHandler.setFormatter(new LogFormatter());
        logger.addHandler(streamHandler);

        logField = new JTextArea(10, 60);

        add(new JScrollPane(logField));
    }

    public void actionPerformed(ActionEvent e) {

        switch (e.getID()) {
        case LogTaskPane.ACTION_SAVE:
            JFileChooser fc = new JFileChooser();
            fc.setApproveButtonText(JDLocale.L("gui.btn_save", "Save"));
            fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
            if (fc.showOpenDialog(this) == JDFileChooser.APPROVE_OPTION) {
                File ret = fc.getSelectedFile();
                if (ret != null) {
                    String content = toString();
                    JDIO.writeLocalFile(ret, content);
                    JDUtilities.getLogger().info("Log saved to file: " + ret.getAbsolutePath());
                }
            }
            break;
        case LogTaskPane.ACTION_UPLOAD:
            Level level = JDUtilities.getLogger().getLevel();
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
        logField.setCaretPosition(logField.getText().length());
    }

    @Override
    public void onHide() {
    }

}
