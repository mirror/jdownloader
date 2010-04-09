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

package jd.gui.swing.jdgui.views.log;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import javax.swing.JFileChooser;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.text.Document;
import javax.swing.text.EditorKit;

import jd.controlling.JDLogHandler;
import jd.controlling.JDLogger;
import jd.controlling.LogFormatter;
import jd.event.ControlEvent;
import jd.event.ControlListener;
import jd.gui.UserIO;
import jd.gui.swing.components.JDFileChooser;
import jd.gui.swing.components.linkbutton.JLink;
import jd.gui.swing.jdgui.interfaces.SwitchPanel;
import jd.nutils.JDFlags;
import jd.nutils.encoding.Encoding;
import jd.nutils.io.JDIO;
import jd.utils.JDUtilities;
import jd.utils.Upload;
import jd.utils.locale.JDL;
import net.miginfocom.swing.MigLayout;

public class LogPane extends SwitchPanel implements ActionListener, ControlListener {

    private static final long serialVersionUID = -5753733398829409112L;
    private static final Object LOCK = new Object();

    /**
     * JTextField wo der Logger Output eingetragen wird
     */
    private JTextPane logField;

    public LogPane() {
        this.setName("LOGDIALOG");
        this.setLayout(new MigLayout("ins 0", "[fill,grow]", "[fill,grow]"));

        logField = new JTextPane();
        logField.setEditable(true);
        logField.setAutoscrolls(true);

        add(new JScrollPane(logField));
    }

    public void actionPerformed(ActionEvent e) {

        switch (e.getID()) {

        case LogInfoPanel.ACTION_SAVE:
            JDFileChooser fc = new JDFileChooser();
            fc.setApproveButtonText(JDL.L("gui.btn_save", "Save"));
            fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
            if (fc.showOpenDialog(this) == JDFileChooser.APPROVE_OPTION) {
                File ret = fc.getSelectedFile();
                if (ret != null) {
                    String content = toString();
                    JDIO.writeLocalFile(ret, content);
                    JDLogger.getLogger().info("Log saved to file: " + ret.getAbsolutePath());
                }
            }
            break;
        case LogInfoPanel.ACTION_UPLOAD:
            Level level = JDLogger.getLogger().getLevel();

            if (!level.equals(Level.ALL)) {
                int status = UserIO.getInstance().requestHelpDialog(UserIO.NO_COUNTDOWN, JDL.L("gui.logdialog.loglevelwarning.title", "Wrong Loglevel for Uploading selected!"), JDL.LF("gui.logdialog.loglevelwarning", "The selected loglevel (%s) isn't preferred to upload a log! Please change it to ALL and create a new log!", level.getName()), null, "http://jdownloader.org/knowledge/wiki/support/create-a-jd-log");
                if (JDFlags.hasSomeFlags(status, UserIO.RETURN_CANCEL, UserIO.RETURN_COUNTDOWN_TIMEOUT)) return;
            }
            String content = null;
            synchronized (LOCK) {
                content = logField.getSelectedText();
                if (content == null || content.length() == 0) {
                    content = Encoding.UTF8Encode(logField.getText());
                }
            }
            if (content == null || content.length() == 0) return;

            String question = UserIO.getInstance().requestInputDialog(UserIO.NO_COUNTDOWN, JDL.L("userio.input.title", "Please enter!"), JDL.L("gui.logger.askQuestion", "Please describe your Problem/Bug/Question!"), null, null, null, null);
            if (question == null) question = "";
            append("\r\n\r\n-------------------------------------------------------------\r\n\r\n");
            String url = Upload.toJDownloader(content, question);
            if (url != null) {
                try {
                    JLink.openURL(url);
                    append(JDL.L("gui.logupload.message", "Please send this loglink to your supporter") + "\r\n");
                    append(url);
                } catch (Exception e1) {
                    JDLogger.exception(e1);
                }
            } else {
                UserIO.getInstance().requestConfirmDialog(UserIO.DONT_SHOW_AGAIN | UserIO.NO_CANCEL_OPTION, JDL.L("sys.warning.loguploadfailed", "Upload of logfile failed!"));
                append(JDL.L("gui.logDialog.warning.uploadFailed", "Upload failed"));
            }
            append("\r\n\r\n-------------------------------------------------------------\r\n\r\n");
            break;
        }

    }

    @Override
    public String toString() {
        synchronized (LOCK) {
            String content = logField.getSelectedText();
            if (content == null || content.length() == 0) {
                content = logField.getText();
            }
            return content;
        }
    }

    @Override
    public void onShow() {
        /*
         * enable autoscrolling by setting the caret to the last position
         */
        /**
         * TODO: not synchronized properbly in loop.
         */
        try {

            JDUtilities.getController().addControlListener(this);
            ArrayList<LogRecord> buff = new ArrayList<LogRecord>();

            buff.addAll(JDLogHandler.getHandler().getBuffer());

            LogFormatter formater = (LogFormatter) JDLogHandler.getHandler().getFormatter();
            StringBuilder sb = new StringBuilder();

            for (LogRecord lr : buff) {
                sb.append(format(lr, formater));
            }
            synchronized (LOCK) {
                logField.setText(sb.toString());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String format(LogRecord lr, LogFormatter formater) {
        if (lr.getThrown() != null) {
            return "EXCEPTION   " + formater.format(lr);
        } else {
            return formater.format(lr);
        }
    }

    @Override
    public void onHide() {
        JDUtilities.getController().removeControlListener(this);
    }

    public void append(String sb) {
        synchronized (LOCK) {
            Document doc = logField.getDocument();

            EditorKit editorkit = logField.getEditorKit();
            Reader r = new StringReader(sb);
            try {
                editorkit.read(r, doc, doc.getEndPosition().getOffset() - 1);
            } catch (Exception e1) {
                e1.printStackTrace();
            }
        }
    }

    public void controlEvent(ControlEvent event) {
        if (event.getID() == ControlEvent.CONTROL_LOG_OCCURED) {
            append(format((LogRecord) event.getParameter(), (LogFormatter) JDLogHandler.getHandler().getFormatter()));
        }
    }

}
