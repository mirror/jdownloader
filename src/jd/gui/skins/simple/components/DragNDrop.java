package jd.gui.skins.simple.components;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.util.Iterator;
import java.util.Vector;
import java.util.logging.Logger;

import javax.swing.JComponent;

import jd.event.UIEvent;
import jd.event.UIListener;
import jd.plugins.Plugin;

/**
 * Diese Komponente ist ein Droptarget. und nimmt gedroppte STrings auf
 * 
 * @author coalado
 */
public class DragNDrop extends JComponent implements DropTargetListener  {
    /**
     * 
     */
    private static final long serialVersionUID = -3280613281656283625L;
    /**
     * 
     */
    private Logger logger= Plugin.getLogger();
    private Image image;
    /**
     * Hiermit wird der Eventmechanismus realisiert. Alle hier eingetragenen
     * Listener werden benachrichtigt, wenn mittels
     * {@link #fireUIEvent(UIEvent)} ein Event losgeschickt wird.
     */
    public Vector<UIListener> uiListener = null;
    /**
     * @param image
     */
    public DragNDrop(Image image) {
       new DropTarget(this, this);
       uiListener=new Vector<UIListener>();
        this.image = image;
        if(image!=null){
        setPreferredSize(new Dimension(image.getWidth(null), image
                .getHeight(null)));
        }
    }

    /**
     * 
     * @return ImageHeight
     */
    public int getImageHeight() {
        return image.getHeight(this);

    }

    /**
     * 
     * @return imagewidth
     */
    public int getImageWidth() {
        return image.getWidth(this);

    }

    /**
     * zeichnet Bild
     * @param g 
     */
    public void paintComponent(Graphics g) {
        g.drawImage(image, 0, 0, null);
    }

    public void dragEnter(DropTargetDragEvent arg0) {
  
        
    }

    public void dragExit(DropTargetEvent arg0) {
    
        
    }

    public void dragOver(DropTargetDragEvent arg0) {
       
        
    }
/**
 * Wird aufgerufen sobald etwas gedropt wurde. Die Funktion liest den Inhalt des Drops aus und benachrichtigt die Listener
 */
    public void drop(DropTargetDropEvent e) {
        logger.info("Drag: DROP "+e.getDropAction()+" : "+e.getSourceActions()+" - "+e.getSource()+" - ");
        
        try {
            Transferable tr = e.getTransferable();
            e.acceptDrop(e.getDropAction());
            if (e.isDataFlavorSupported(DataFlavor.stringFlavor)) {

                    String files = (String) tr
                                    .getTransferData(DataFlavor.stringFlavor);                   
                  
                   logger.info(files);
                        fireUIEvent(new UIEvent(this,UIEvent.UI_DRAG_AND_DROP,files));
                
            } else {
                logger.info("UU");
            }
//            e.dropComplete(true);
    } catch (Exception exc) {
//            e.rejectDrop();
            exc.printStackTrace();
    } 
    }
/**
 * UI Add LIstener Funktion. Fügt einen Listener hinzu
 * @param listener
 */
    public void addUIListener(UIListener listener) {    
        synchronized (uiListener) {
            uiListener.add(listener);
        }
    }
    /**
     * UI Remove Listener Funktion. Entfernt  einen Listener
     * @param listener
     */
    public void removeUIListener(UIListener listener) {    
        synchronized (uiListener) {
            uiListener.remove(listener);
        }
    }
    
    /**
     * Benachrichtigt alle Listener über ein Event
     * @param uiEvent
     */
    public void fireUIEvent(UIEvent uiEvent) {    
        synchronized (uiListener) {
            Iterator<UIListener> recIt = uiListener.iterator();

            while (recIt.hasNext()) {
                ((UIListener) recIt.next()).uiEvent(uiEvent);
            }
        }
    }

    public void dropActionChanged(DropTargetDragEvent dtde) {
      
        
    }
    
    

   
}