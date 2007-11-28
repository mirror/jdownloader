package jd.gui.skins.simple;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.StreamHandler;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import jd.config.Configuration;
import jd.gui.skins.simple.components.TextAreaDialog;
import jd.plugins.LogFormatter;
import jd.plugins.Plugin;
import jd.plugins.RequestInfo;
import jd.utils.JDUtilities;

/**
 * Ein Dialog, der Logger-Output anzeigen kann.
 * 
 * @author Tom
 */
public class LogDialog extends JDialog implements ActionListener {

    private static final long serialVersionUID = -5753733398829409112L;

    /**
     * JTextField wo der Logger Output eingetragen wird
     */
    private JTextArea         logField;

    /**
     * JScrollPane fuer das logField
     */
    private JScrollPane       logScrollPane;

    /**
     * Knopf zum schliessen des Fensters
     */
    private JButton           btnOK;

    private JButton           btnSave;

    private JButton           btnCensor;

    private JFrame            owner;

    private JButton           btnUpload;

    /**
     * Primary Constructor
     * 
     * @param owner The owning Frame
     * @param logger The connected Logger
     */
    public LogDialog(JFrame owner, Logger logger) {
        super(owner);
        this.owner = owner;
        setModal(false);
        setLayout(new GridBagLayout());

        Handler streamHandler = new LogStreamHandler(new PrintStream(new LogStream()));
        streamHandler.setLevel(Level.ALL);
        streamHandler.setFormatter(new LogFormatter());
        logger.addHandler(streamHandler);

        btnOK = new JButton("OK");
        btnOK.addActionListener(this);
        btnSave = new JButton("Save to file");
        btnSave.addActionListener(this);

        btnCensor = new JButton("Censor Log");
        btnCensor.addActionListener(this);

        btnUpload = new JButton("Upload Log");
        btnUpload.addActionListener(this);
        getRootPane().setDefaultButton(btnOK);
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        logField = new JTextArea(10, 60);
        logScrollPane = new JScrollPane(logField);
        logField.setEditable(true);

        JDUtilities.addToGridBag(this, logScrollPane, 0, 0, 5, 1, 1, 1, null, GridBagConstraints.BOTH, GridBagConstraints.EAST);
        JDUtilities.addToGridBag(this, btnOK, 0, 1, 1, 1, 1, 0, null, GridBagConstraints.NONE, GridBagConstraints.WEST);
        JDUtilities.addToGridBag(this, btnSave, 1, 1, 1, 1, 1, 0, null, GridBagConstraints.NONE, GridBagConstraints.EAST);

        JDUtilities.addToGridBag(this, btnCensor, 2, 1, 1, 1, 1, 0, null, GridBagConstraints.NONE, GridBagConstraints.EAST);
        JDUtilities.addToGridBag(this, btnUpload, 3, 1, 1, 1, 1, 0, null, GridBagConstraints.NONE, GridBagConstraints.EAST);
        pack();
        setLocation(JDUtilities.getCenterOfComponent(null, this));
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == btnOK) {
            dispose();
        }

        if (e.getSource() == btnCensor) {
            String txt;
            String[] censor = JDUtilities.splitByNewline(txt = TextAreaDialog.showDialog(owner, "Censor Log!", "Add Elements to censor. Use 'replaceme==replacement' or just 'deleteme' in a line. Regexes ar possible!", JDUtilities.getConfiguration().getStringProperty(Configuration.PARAM_CENSOR_FIELD, "PREMIUM\\_USER\\=(.*?)\\,==PREMIUMUSER" + System.getProperty("line.separator") + "PREMIUM\\_PASS\\=(.*?)\\,==PREMIUMPASS")));

            if (censor.length > 0) {
                JDUtilities.getConfiguration().setProperty(Configuration.PARAM_CENSOR_FIELD, txt);
                JDUtilities.saveObject(null, JDUtilities.getConfiguration(), JDUtilities.getJDHomeDirectory(), JDUtilities.CONFIG_PATH.split("\\.")[0], "." + JDUtilities.CONFIG_PATH.split("\\.")[1], Configuration.saveAsXML);
                
                String content = logField.getSelectedText();
                if (content == null || content.length() == 0) {
                    content = logField.getText();
                }
                for (int i = 0; i < censor.length; i++) {
                    content = content.replace(censor[i], "[********]");
                    content = content.replaceAll(censor[i], "[********]");
                    String[] tmp = content.split("\\=\\=");
                    if (tmp.length == 2) {
                        content = content.replaceAll(tmp[0], tmp[1]);
                        content = content.replace(tmp[0], tmp[1]);
                    }
                }
                logField.setText(content);
             

            }
        }
        if (e.getSource() == btnUpload) {
          String name=JDUtilities.getController().getUiInterface().showUserInputDialog("Your name?");
          if(name!=null){
              String content = logField.getSelectedText();
              if (content == null || content.length() == 0) {
                  content = logField.getText();
              }
              RequestInfo requestInfo=null;
              try {
                requestInfo = Plugin.postRequestWithoutHtmlCode(new URL("http://jd_"+JDUtilities.getMD5(content)+".pastebin.com/pastebin.php"), null, null, "parent_pid=&format=text&code2="+URLEncoder.encode(content,"UTF-8")+"&poster="+URLEncoder.encode(name,"UTF-8")+"&paste=Send&expiry=m&email=", false);
            }
            catch (MalformedURLException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }
            catch (IOException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }
            if(requestInfo!=null &&requestInfo.isOK()){
            JOptionPane.showInputDialog(this, "Log-Link", requestInfo.getLocation());
            }else{
                JOptionPane.showMessageDialog(this, "Upload failed");
            }
              
          }
          
        }
        if (e.getSource() == btnSave) {
            JFileChooser fc = new JFileChooser();
            fc.setApproveButtonText("Save");
            if (JDUtilities.getConfiguration().getStringProperty(Configuration.PARAM_CURRENT_BROWSE_PATH, null) != null) fc.setCurrentDirectory(new File(JDUtilities.getConfiguration().getStringProperty(Configuration.PARAM_CURRENT_BROWSE_PATH, null)));
            fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
            fc.showOpenDialog(this);
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

    /**
     * Ein OutputStream, der die Daten an das log field weiterleitet
     */
    private class LogStream extends OutputStream {

        @Override
        public void write(int b) throws IOException {
            // den character an das text control anhaengen
            logField.append(String.valueOf((char) b));
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
         * @param record description of the log event. A null record is silently
         *            ignored and is not published
         */
        public void publish(LogRecord record) {
            super.publish(record);
            flush();
        }

    }

}
