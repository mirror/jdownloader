package jd.gui.skins.simple;

import java.awt.GridBagConstraints;
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
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import jd.config.Configuration;
import jd.plugins.LogFormatter;
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
   private JTextArea logField;

   /**
    * JScrollPane fuer das logField
    */
   private JScrollPane logScrollPane;

   /**
    * Knopf zum schliessen des Fensters
    */
   private JButton btnOK;

private JButton btnSave;

   /**
    * Primary Constructor
    * 
    * @param owner
    *           The owning Frame
    * @param logger
    *           The connected Logger
    */
   public LogDialog(JFrame owner, Logger logger) {
      super(owner);
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
      getRootPane().setDefaultButton(btnOK);
      setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

      logField = new JTextArea(10, 60);
      logScrollPane = new JScrollPane(logField);
      logField.setEditable(true);

      JDUtilities.addToGridBag(this, logScrollPane, 0, 0, 1, 1, 1, 1, null,
            GridBagConstraints.BOTH, GridBagConstraints.EAST);
      JDUtilities.addToGridBag(this, btnOK, 0, 1, 1, 1, 1, 0, null, GridBagConstraints.NONE,
            GridBagConstraints.CENTER);
      JDUtilities.addToGridBag(this, btnSave, 0, 1, 1, 1, 1, 0, null, GridBagConstraints.NONE,
              GridBagConstraints.WEST);
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
      
      if (e.getSource() == btnSave) {
          JFileChooser fc = new JFileChooser();
          fc.setApproveButtonText("Save");
          if (JDUtilities.getConfiguration().getStringProperty(Configuration.PARAM_CURRENT_BROWSE_PATH, null) != null) fc.setCurrentDirectory(new File(JDUtilities.getConfiguration().getStringProperty(Configuration.PARAM_CURRENT_BROWSE_PATH, null)));
          fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
          fc.showOpenDialog(this);
          File ret = fc.getSelectedFile();
          if(ret!=null){
              String content=logField.getSelectedText();
              if(content==null ||content.length()==0){
                  content=logField.getText();
              }
              JDUtilities.writeLocalFile(ret, content);
              JDUtilities.getLogger().info("Log saved to file: "+ret.getAbsolutePath());
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
    * Handler der einen OutputStream unterstuetzt basierend auf einem ConsoleHandler
    */
   private class LogStreamHandler extends StreamHandler {

      public LogStreamHandler(OutputStream stream) {
         // super();
         setOutputStream(stream);
      }

      /**
       * Publish a <tt>LogRecord</tt>.
       * <p>
       * The logging request was made initially to a <tt>Logger</tt> object, which initialized the
       * <tt>LogRecord</tt> and forwarded it here.
       * <p>
       * 
       * @param record
       *           description of the log event. A null record is silently ignored and is not
       *           published
       */
      public void publish(LogRecord record) {
         super.publish(record);
         flush();
      }

   }

}
