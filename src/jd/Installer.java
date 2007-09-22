package jd;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.Vector;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JComponent;
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
public class Installer extends JDialog implements ActionListener {

    /**
     * 8764525546298642601L
     */
    private static final long  serialVersionUID = 8764525546298642601L;

    private Logger             logger           = Plugin.getLogger();

    private boolean            continueInstall  = false;

    private boolean            cancelInstall    = false;

    private Vector<JComponent> panels           = new Vector<JComponent>();

    /**
     * 
     */
    public Installer() {
        super();
        setModal(false);
        setLayout(new GridBagLayout());

        this.setTitle("JDownloader Installation");

        // this.setResizable(false);

        setLocation(20, 20);
        this.setAlwaysOnTop(true);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent event) {

                abortInstallation();
            }

        });
        pack();
    }

    /**
     * @param str
     * @param path
     * @return Fragt einen Ordner beim User ab.  mit str kann man gezielt nach einem ordner fragen
     */
    public String getDirectory(String str,File path) {
        clearPanel();
        logger.info("getHome");
        continueInstall = false;
        cancelInstall = false;
        JLabel label1 = new JLabel(str);
        JDUtilities.addToGridBag(this, label1, 0, 0, 1, 1, 0, 0, null, GridBagConstraints.CENTER, GridBagConstraints.CENTER);
        panels.add(label1);
        BrowseFile browse1 = new BrowseFile(50);
        panels.add(browse1);
        browse1.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        browse1.setCurrentPath(path);

        JDUtilities.addToGridBag(this, browse1, 0, 1, 1, 1, 0, 0, null, GridBagConstraints.CENTER, GridBagConstraints.CENTER);
        JButton button1 = new JButton("Weiter");
        panels.add(button1);
        button1.addActionListener(this);
        JDUtilities.addToGridBag(this, button1, 0, 2, 1, 1, 0, 0, null, GridBagConstraints.CENTER, GridBagConstraints.CENTER);
        pack();
        setVisible(true);
        if (waitFor()) {
            return browse1.getText();
        }
        return null;

    }
/**
 * Wartet auf Userinput
 * @return true/false
 */
    private boolean waitFor() {
        while (true) {
            try {
                Thread.sleep(500);

            }
            catch (InterruptedException e) {}
            if (continueInstall) {
                return true;
            }

            if (cancelInstall) {
                return false;
            }

        }

    }

    /**
     * Führt ein Webupdate nach der Installation aus und gibt im erfolgsfall true zurück, sonst false
     * @return true/Fasle
     */
    public boolean doUpdate() {
        logger.info("Update");
        clearPanel();

        continueInstall = false;
        cancelInstall = false;
        JLabel label = new JLabel("Führe nun das Webupdate aus. Benötigte Daten werden geladen.");
        panels.add(label);
        JDUtilities.addToGridBag(this, label, 0, 0, 1, 1, 0, 0, null, GridBagConstraints.CENTER, GridBagConstraints.CENTER);
        final JLabel ticker = new JLabel("/");
        panels.add(ticker);
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
        catch (InterruptedException e) {
        }
        this.setVisible(false);
return true;
    }
    
    
   

    private void clearPanel() {

        while (panels.size() > 0) {
            remove(panels.remove(0));
        }

    }

    /**
     * bricht die INstallation ab
     */
    public void abortInstallation() {
        cancelInstall = true;
        this.setVisible(false);
        clearPanel();

    }
/**
 * actionPerformed für die buttons
 * @param e ActionEvent
 */
    public void actionPerformed(ActionEvent e) {
        continueInstall = true;
        this.setVisible(false);

    }

}
