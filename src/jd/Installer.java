package jd;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;

import jd.gui.skins.simple.components.BrowseFile;
import jd.plugins.Plugin;
import jd.update.WebUpdater;
import jd.utils.JDUtilities;

/**
 * Der Installer erscheint nur beim ersten mal STarten der Webstartversion und beim neuinstallieren der webstartversion
 *  der User kann Basiceinstellungen festlegen
 * @author Coalado
 */
public class Installer extends JDialog implements ActionListener, WindowListener {

    /**
     * 8764525546298642601L
     */
    private static final long  serialVersionUID = 8764525546298642601L;
    private Logger             logger           = Plugin.getLogger();

    private JLabel lblMessage;
    private BrowseFile browseFile;
    private JButton btnOK;
    private static boolean aborted = false;
    /**
     * 
     */
    public Installer() {
        super();
        setModal(true);
        setLayout(new GridBagLayout());

        this.setTitle("JDownloader Installation");
        this.setAlwaysOnTop(true);

        setLocation(20, 20);
        lblMessage = new JLabel();
        browseFile = new BrowseFile(50);
        btnOK = new JButton("Weiter");

        btnOK.addActionListener(this);
        addWindowListener(this);
        JDUtilities.addToGridBag(this, lblMessage, 0, 0, 1, 1, 0, 0, null, GridBagConstraints.CENTER, GridBagConstraints.CENTER);
        JDUtilities.addToGridBag(this, browseFile, 0, 1, 1, 1, 0, 0, null, GridBagConstraints.CENTER, GridBagConstraints.CENTER);
        JDUtilities.addToGridBag(this, btnOK,      0, 2, 1, 1, 0, 0, null, GridBagConstraints.CENTER, GridBagConstraints.CENTER);
    }

    /**
     * Fragt einen Ordner beim User ab.  mit str kann man gezielt nach einem ordner fragen
     * 
     * @param message Die Nachricht, die dem Benutzer gezeigt werden soll
     * @param path Standardpfad
     * @return Dateipfad 
     */
    public String getDirectory(String message,File path) {
        logger.info("getHome");
        lblMessage.setText(message);
        browseFile.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        browseFile.setCurrentPath(path);
        pack();

        setVisible(true);
        if(aborted)
            return null;
        else
            return browseFile.getText();

    }

    /**
     * FÃ¼hrt ein Webupdate nach der Installation aus und gibt im erfolgsfall true zurÃ¼ck, sonst false
     * @return true/Fasle
     */
    public boolean doUpdate() {
        logger.info("Update");
        JLabel label = new JLabel("FÃ¼hre nun das Webupdate aus. BenÃ¶tigte Daten werden geladen.");
        JDUtilities.addToGridBag(this, label, 0, 0, 1, 1, 0, 0, null, GridBagConstraints.CENTER, GridBagConstraints.CENTER);
        final JLabel ticker = new JLabel("/");
        JDUtilities.addToGridBag(this, ticker, 0, 2, 1, 1, 0, 0, null, GridBagConstraints.CENTER, GridBagConstraints.CENTER);
        pack();
        setVisible(true);
        Thread th = new Thread() {
            public void run() {
                while (true) {
                    try {
                        Thread.sleep(300);
                    }
                    catch (InterruptedException e) {
                    }

                    if (ticker.getText().equals("\\")) {
                        ticker.setText("/");

                    }
                    else if (ticker.getText().equals("-")) {
                        ticker.setText("\\");
                    }
                    else if (ticker.getText().equals("/")) {
                        ticker.setText("-");
                    }

                }
            }
        };
        th.start();

        WebUpdater updater = new WebUpdater(null);
        updater.run();

        ticker.setText("Aktualisierte Dateien: " + updater.getUpdatedFiles());

        try {
            Thread.sleep(2000);
        }
        catch (InterruptedException e) { }
        this.setVisible(false);
        return true;
    }


    /**
     * Bricht die Installation ab
     */
    public void abortInstallation() {
        aborted = true;
        this.setVisible(false);
    }
    /**
     * actionPerformed fÃ¼r die buttons
     * @param e ActionEvent
     */
    public void actionPerformed(ActionEvent e) {
        aborted= false;
        this.setVisible(false);

    }
    public void windowClosing(WindowEvent e)     { abortInstallation(); }
    public void windowActivated(WindowEvent e)   {  }
    public void windowClosed(WindowEvent e)      {  }
    public void windowDeactivated(WindowEvent e) {  }
    public void windowDeiconified(WindowEvent e) {  }
    public void windowIconified(WindowEvent e)   {  }
    public void windowOpened(WindowEvent e)      {  }

}
