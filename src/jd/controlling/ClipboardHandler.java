package jd.controlling;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;

//TODO Überarbeiten
/**
 * Diese Klasse ist dafür da, zeitverzögert die Zwischenablage zu untersuchen
 * 
 * @author astaldo
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

    public ClipboardHandler(JDController controller) {
        super("JD-ClipboardHandler");
        this.controller = controller;
        clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        this.start();
    }

    public void run() {

        
        
   
        try {
            //Verhindert dass beim starten die zwischenablage ausgewertet wird
        data = (String) clipboard.getData(DataFlavor.stringFlavor);
        
            synchronized (clipboard) {
                while (true) {
                    try {
                        clipboard.wait(500);
                    }
                    catch (InterruptedException e) {
                    }
                    if (enabled) {
                        
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
                        catch (IOException e1) {
                        }
                    }
                }

            }
        }
        catch (Exception e) {}
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        //Verhindert dass beim starten die zwischenablage ausgewertet wird
        try {
            data = (String) clipboard.getData(DataFlavor.stringFlavor);
        }
        catch (UnsupportedFlavorException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }  catch (Exception e) {
            e.printStackTrace();
        }
    }
}
