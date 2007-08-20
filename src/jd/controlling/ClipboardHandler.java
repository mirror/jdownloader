package jd.controlling;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;

import jd.gui.GUIInterface;

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
    private GUIInterface guiInterface;
    
    public ClipboardHandler(GUIInterface guiInterface){
        super("JD-ClipboardHandler");
        this.guiInterface = guiInterface;
    }
    public void run(){
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        synchronized (clipboard) {
            try {
                clipboard.wait(500);
            }
            catch (InterruptedException e) { }
            String data="";
            try {
                data = (String)clipboard.getData(DataFlavor.stringFlavor);
                distributeData = new DistributeData(data);
                distributeData.addControlListener(guiInterface);
                distributeData.start();
            }
            catch (UnsupportedFlavorException e1) {}
            catch (IOException e1)                {}

            clipboard.setContents(new StringSelection(data), guiInterface);
        }
    }
}

