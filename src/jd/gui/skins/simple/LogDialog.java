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

import jd.gui.skins.simple.Link.JLinkButton;
import jd.gui.skins.simple.components.TextAreaDialog;
import jd.plugins.LogFormatter;
import jd.plugins.Plugin;
import jd.plugins.RequestInfo;
import jd.utils.JDLocale;
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

   // private JButton           btnCensor;

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

        btnOK = new JButton(JDLocale.L("gui.btn_ok","OK"));
        btnOK.addActionListener(this);
        btnSave = new JButton(JDLocale.L("gui.btn_saveToFile","Save to file"));
        btnSave.addActionListener(this);

//        btnCensor = new JButton(JDLocale.L("gui.logDialog.btn_censor","Censor Log"));
//        btnCensor.addActionListener(this);

        btnUpload = new JButton(JDLocale.L("gui.logDialog.btn_uploadLog","Upload Log"));
        btnUpload.addActionListener(this);
        getRootPane().setDefaultButton(btnOK);
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        logField = new JTextArea(10, 60);
        logScrollPane = new JScrollPane(logField);
        logField.setEditable(true);

        JDUtilities.addToGridBag(this, logScrollPane, 0, 0, 5, 1, 1, 1, null, GridBagConstraints.BOTH, GridBagConstraints.EAST);
        JDUtilities.addToGridBag(this, btnOK, 0, 1, 1, 1, 1, 0, null, GridBagConstraints.NONE, GridBagConstraints.WEST);
        JDUtilities.addToGridBag(this, btnSave, 1, 1, 1, 1, 1, 0, null, GridBagConstraints.NONE, GridBagConstraints.EAST);

//        JDUtilities.addToGridBag(this, btnCensor, 2, 1, 1, 1, 1, 0, null, GridBagConstraints.NONE, GridBagConstraints.EAST);
        JDUtilities.addToGridBag(this, btnUpload, 2, 1, 1, 1, 1, 0, null, GridBagConstraints.NONE, GridBagConstraints.EAST);
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

//        if (e.getSource() == btnCensor) {
//            String txt;
//            String[] censor = JDUtilities.splitByNewline(txt = TextAreaDialog.showDialog(owner, JDLocale.L("gui.logDialog.censordialog.title","Censor Log!"), JDLocale.L("gui.logDialog.censorDialog.text","Add Elements to censor. Use 'replaceme==replacement' or just 'deleteme' in a line. Regexes ar possible!"), JDUtilities.getSubConfig(SimpleGUI.GUICONFIGNAME).getStringProperty(SimpleGUI.PARAM_CENSOR_FIELD, "" + System.getProperty("line.separator") + "")));
//
//            if (censor.length > 0) {
//                JDUtilities.getSubConfig(SimpleGUI.GUICONFIGNAME).setProperty(SimpleGUI.PARAM_CENSOR_FIELD, txt);
//               JDUtilities.getSubConfig(SimpleGUI.GUICONFIGNAME).save();
//                String content = logField.getSelectedText();
//                if (content == null || content.length() == 0) {
//                    content = logField.getText();
//                }
//                for (int i = 0; i < censor.length; i++) {
//                    content = content.replace(censor[i], "[********]");
//                    content = content.replaceAll(censor[i], "[********]");
//                    String[] tmp = content.split("\\=\\=");
//                    if (tmp.length == 2) {
//                        content = content.replaceAll(tmp[0], tmp[1]);
//                        content = content.replace(tmp[0], tmp[1]);
//                    }
//                }
//                logField.setText(content);
//             
//
//            }
//        }
        if (e.getSource() == btnUpload) {
            String txt;
          
           
          
                String content = logField.getSelectedText();
                if (content == null || content.length() == 0) {
                    content =JDUtilities.UTF8Encode( logField.getText());
                }
            content=TextAreaDialog.showDialog(owner, "Log", JDLocale.L("gui.logdialog.yourlog","Hochgeladener Log: Editieren möglich!"), content);
                
           
             
if(content==null||content.length()==0)return;
          
          String name=JDUtilities.getController().getUiInterface().showUserInputDialog(JDLocale.L("gui.askName","Your name?"));
          if(name!=null){
            
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
                try {
                    JLinkButton.OpenURL(requestInfo.getLocation());
                }
                catch (MalformedURLException e1) {
                    JOptionPane.showInputDialog(this, JDLocale.L("gui.logDialog.logLink","Log-Link"), requestInfo.getLocation());
                    
                }
           

            }else{
                JOptionPane.showMessageDialog(this, JDLocale.L("gui.logDialog.warning.uploadFailed","Upload failed"));
            }
              
          }
          
        }
        if (e.getSource() == btnSave) {
            JFileChooser fc = new JFileChooser();
            fc.setApproveButtonText(JDLocale.L("gui.logDialog.btn_save","Save"));
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
            if(logField!=null)
            logField.append((String.valueOf((char) b)));
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
