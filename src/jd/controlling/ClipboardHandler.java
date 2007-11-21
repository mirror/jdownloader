package jd.controlling;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;

import jd.config.Configuration;
import jd.utils.JDUtilities;


/**
 * Diese Klasse ist dafür da, zeitverzögert die Zwischenablage zu untersuchen
 * 
 * @author astaldo/coalado
 */
public class ClipboardHandler extends Thread {
    /**
     * Der Thread, der den Inhalt der Zwischenablage verteilt
     */
    private DistributeData distributeData = null;

    /**
     * Die Schnittstelle zur GUI
     */
    private String         lastString     = null;

    private JDController   controller;

    private boolean        enabled        = false;

    private Clipboard clipboard;

    private String data;

    /**
     * @param controller
     */
    public ClipboardHandler(JDController controller) {
        super("JD-ClipboardHandler");
        this.controller = controller;
        clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        this.start();
    }

    /* (non-Javadoc)
     * @see java.lang.Thread#run()
     */
    public void run() {

        try {
            //Verhindert dass beim starten die zwischenablage ausgewertet wird
            lastString = (String) clipboard.getData(DataFlavor.stringFlavor);
        
            synchronized (clipboard) {
                while (true) {
                    try {
                        clipboard.wait(500);
                    }
                    catch (InterruptedException e) {
                    }
                    
                    if (isEnabled()) {
                        
                        try {
                            data = (String) clipboard.getData(DataFlavor.stringFlavor);
                            
                            if (data != null && !data.equalsIgnoreCase(lastString)) {
                                distributeData = new DistributeData(data);
                                distributeData.addControlListener(controller);
                                distributeData.start();
                            }
                            lastString = data;
                        }
                        catch (UnsupportedFlavorException e1) {
                        }
                        catch (Exception e1) {
                        }
                    }
                }

            }
        }
        catch (Exception e) {}
    }
/**
 * Gibt an ob die clipboard überwachung aktiv ist
 * @return true/false
 */
    public boolean isEnabled() {
        return enabled||JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_CLIPBOARD_ALWAYS_ACTIVE, false);
    }
/**
 * Schaltet die clipboardüberwachung an/aus
 * @param enabled
 */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        //Verhindert dass beim starten die zwischenablage ausgewertet wird
        try {
            lastString = (String) clipboard.getData(DataFlavor.stringFlavor);
        }
        catch (UnsupportedFlavorException e) {
            e.printStackTrace();
        }
        catch (IOException e) {
            e.printStackTrace();
        }  catch (Exception e) {
            e.printStackTrace();
        }
    }
}
