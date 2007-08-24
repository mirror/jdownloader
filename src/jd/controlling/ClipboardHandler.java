package jd.controlling;


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
    private JDController controller;
    
    public ClipboardHandler(JDController controller){
        super("JD-ClipboardHandler");
        this.controller = controller;
    }
    public void run(){
//        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
//        synchronized (clipboard) {
//            try {
//                clipboard.wait(500);
//            }
//            catch (InterruptedException e) { }
//            String data="";
//            try {
//                data = (String)clipboard.getData(DataFlavor.stringFlavor);
//                distributeData = new DistributeData(data);
//                distributeData.addControlListener(guiInterface);
//                distributeData.start();
//            }
//            catch (UnsupportedFlavorException e1) {}
//            catch (IOException e1)                {}
//        }
    }
}

