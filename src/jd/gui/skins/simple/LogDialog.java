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
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.StreamHandler;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;

import jd.gui.skins.simple.components.JDFileChooser;
import jd.gui.skins.simple.components.JLinkButton;
import jd.gui.skins.simple.components.TextAreaDialog;
import jd.http.Encoding;
import jd.utils.JDLocale;
import jd.utils.JDTheme;
import jd.utils.JDUtilities;
import jd.utils.LogFormatter;
import jd.utils.Upload;
import edu.stanford.ejalbert.exception.BrowserLaunchingInitializingException;
import edu.stanford.ejalbert.exception.UnsupportedOperatingSystemException;

/**
 * Ein Dialog, der Logger-Output anzeigen kann.
 * 
 * @author Tom
 */
public class LogDialog extends JFrame implements ActionListener {

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
            // super();
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
     * Knopf zum schliessen des Fensters
     */
    private JButton btnOK;

    private JButton btnSave;

    private JButton btnUpload;

    /**
     * JTextField wo der Logger Output eingetragen wird
     */
    private JTextArea logField;

    private JFrame owner;

    /**
     * Primary Constructor
     * 
     * @param owner
     *            The owning Frame
     * @param logger
     *            The connected Logger
     */
    public LogDialog(JFrame owner, Logger logger) {
        this.owner = owner;
        this.setIconImage(JDUtilities.getImage(JDTheme.V("gui.images.terminal")));
        this.setTitle(JDLocale.L("gui.logDialog.title", "jDownloader Logausgabe"));
        this.setLayout(new GridBagLayout());
        this.setName("LOGDIALOG");
        this.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        Handler streamHandler = new LogStreamHandler(new PrintStream(new LogStream()));
        streamHandler.setLevel(Level.ALL);
        streamHandler.setFormatter(new LogFormatter());
        logger.addHandler(streamHandler);

        logField = new JTextArea(10, 60);
        logField.setEditable(true);

        this.addWindowListener(new LocationListener());

        int n = 10;
        JPanel bpanel = new JPanel(new FlowLayout(FlowLayout.CENTER, n, 0));
        bpanel.add(btnOK = new JButton(JDLocale.L("gui.btn_ok", "OK")));
        bpanel.add(btnSave = new JButton(JDLocale.L("gui.btn_save", "Save")));
        bpanel.add(btnUpload = new JButton(JDLocale.L("gui.logDialog.btn_uploadLog", "Upload Log")));

        btnOK.addActionListener(this);
        btnSave.addActionListener(this);
        btnUpload.addActionListener(this);

        JPanel panel = new JPanel(new BorderLayout(n, n));
        panel.setBorder(new EmptyBorder(n, n, n, n));
        panel.add(new JScrollPane(logField), BorderLayout.CENTER);
        panel.add(bpanel, BorderLayout.SOUTH);

        this.getRootPane().setDefaultButton(btnOK);
        this.setContentPane(panel);
        this.setPreferredSize(new Dimension(640, 480));
        this.pack();
        this.setLocationRelativeTo(null);
        SimpleGUI.restoreWindow(null, this);
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == btnOK) {
            dispose();
        } else if (e.getSource() == btnUpload) {
            String content = logField.getSelectedText();
            if (content == null || content.length() == 0) {
                content = Encoding.UTF8Encode(logField.getText());
            }
            content = TextAreaDialog.showDialog(owner, "Log", JDLocale.L("gui.logdialog.yourlog", "Hochgeladener Log: Editieren möglich!"), content);

            if (content == null || content.length() == 0) { return; }

            String name = JDUtilities.getController().getUiInterface().showUserInputDialog(JDLocale.L("gui.askName", "Your name?"));
            String question = JDUtilities.getController().getUiInterface().showUserInputDialog(JDLocale.L("gui.logger.askQuestion", "Please describe your Problem/Bug/Question!"));

            String url = Upload.toJDownloader(content, name + "\r\n\r\n" + question);
            if (url != null) {
                String res = null;

                res = JOptionPane.showInputDialog(this, JDLocale.L("gui.logDialog.logLink", "Log-Link (click ok to open)"), url);

                if (res != null) {

                    try {
                        JLinkButton.openURL(url);
                  
                    } catch (Exception e1) {
                        // TODO Auto-generated catch block
                        e1.printStackTrace();
                    }

                }

            } else {
                JOptionPane.showMessageDialog(this, JDLocale.L("gui.logDialog.warning.uploadFailed", "Upload failed"));
            }

        } else if (e.getSource() == btnSave) {
            JFileChooser fc = new JFileChooser();
            fc.setApproveButtonText(JDLocale.L("gui.btn_save", "Save"));
            fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
            if (fc.showOpenDialog(this) == JDFileChooser.APPROVE_OPTION) {
                File ret = fc.getSelectedFile();
                if (ret != null) {
                    String content = logField.getSelectedText();
                    if (content == null || content.length() == 0) {
                        content = logField.getText();
                    }
                    JDUtilities.writeLocalFile(ret, content);
                    JDUtilities.getLogger().info("Log saved to file: " + ret.getAbsolutePath());
                }
            }
        }
    }

}
