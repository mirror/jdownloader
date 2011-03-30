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
import java.io.StringReader;

import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.text.Document;
import javax.swing.text.EditorKit;

import jd.controlling.JDLogger;
import jd.gui.UserIO;
import jd.gui.swing.jdgui.interfaces.SwitchPanel;
import jd.nutils.encoding.Encoding;
import jd.utils.Upload;
import jd.utils.locale.JDL;
import net.miginfocom.swing.MigLayout;

import org.appwork.utils.os.CrossSystem;

/**
 * The panel for the log file.
 */
public class LogPane extends SwitchPanel implements ActionListener {
    private static final long   serialVersionUID = -5753733398829409112L;

    private static final Object LOCK             = new Object();

    /**
     * JTextField where the log will be shown.
     */
    private JTextPane           logField;

    public LogPane() {
        this.setName("LOGDIALOG");
        this.setLayout(new MigLayout("ins 0", "[fill,grow]", "[fill,grow]"));

        logField = new JTextPane();
        logField.setEditable(true);
        logField.setAutoscrolls(true);

        add(new JScrollPane(logField));
    }

    /**
     * The eventhandler for uploading the log.
     */
    public void actionPerformed(ActionEvent e) {
        switch (e.getID()) {
        case LogInfoPanel.ACTION_UPLOAD:

            String content;
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
                CrossSystem.openURLOrShowMessage(url);
                append(JDL.L("gui.logupload.message", "Please send this loglink to your supporter") + "\r\n" + url);
            } else {
                UserIO.getInstance().requestConfirmDialog(UserIO.DONT_SHOW_AGAIN | UserIO.NO_CANCEL_OPTION, JDL.L("sys.warning.loguploadfailed", "Upload of logfile failed!"));
                append(JDL.L("gui.logDialog.warning.uploadFailed", "Upload failed"));
            }
            append("\r\n\r\n-------------------------------------------------------------\r\n\r\n");
            break;
        }

    }

    @Override
    public void onShow() {
        synchronized (LOCK) {
            logField.setText(JDLogger.getLog());
        }
    }

    @Override
    public void onHide() {
    }

    /**
     * Appends a String onto the log of the textfield.
     * 
     * @param sb
     *            The to appending text.
     */
    public void append(String sb) {
        /**
         * TODO: No autoscrolling anymore!
         */
        synchronized (LOCK) {
            Document doc = logField.getDocument();

            EditorKit editorkit = logField.getEditorKit();
            StringReader r = new StringReader(sb);
            try {
                editorkit.read(r, doc, doc.getEndPosition().getOffset() - 1);
            } catch (Exception e1) {
                e1.printStackTrace();
            }
        }
    }

}