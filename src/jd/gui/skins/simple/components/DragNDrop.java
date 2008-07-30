//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team jdownloader@freenet.de
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.

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
import java.util.List;
import java.util.Vector;
import java.util.logging.Logger;

import javax.swing.JComponent;

import jd.event.UIEvent;
import jd.event.UIListener;
import jd.utils.JDTheme;
import jd.utils.JDUtilities;

/**
 * Diese Komponente ist ein Droptarget. und nimmt gedroppte STrings auf
 * 
 * @author JD-Team
 */
public class DragNDrop extends JComponent implements DropTargetListener {
    /**
     * 
     */
    private static final long serialVersionUID = -3280613281656283625L;

    private boolean filled = false;

    private Image imageEmpty;

    private Image imageFilled;

    /**
     * 
     */
    private Logger logger = JDUtilities.getLogger();

    /**
     * Hiermit wird der Eventmechanismus realisiert. Alle hier eingetragenen
     * Listener werden benachrichtigt, wenn mittels
     * {@link #fireUIEvent(UIEvent)} ein Event losgeschickt wird.
     */
    public Vector<UIListener> uiListener = null;

    /**
     * Erzeugt ein neues Drag&Drop Objekt
     */
    public DragNDrop() {
        new DropTarget(this, this);
        uiListener = new Vector<UIListener>();
        imageEmpty = JDUtilities.getImage(JDTheme.V("gui.images.clipboard"));
        imageFilled = JDUtilities.getImage(JDTheme.V("gui.images.clipboard"));
        if (imageEmpty != null) {
            setPreferredSize(new Dimension(imageEmpty.getWidth(null), imageEmpty.getHeight(null)));
        }
    }

    /**
     * UI Add LIstener Funktion. Fügt einen Listener hinzu
     * 
     * @param listener
     */
    public void addUIListener(UIListener listener) {
        synchronized (uiListener) {
            uiListener.add(listener);
        }
    }

    public void dragEnter(DropTargetDragEvent arg0) {
    }

    public void dragExit(DropTargetEvent arg0) {
    }

    public void dragOver(DropTargetDragEvent arg0) {
    }

    /**
     * Wird aufgerufen sobald etwas gedropt wurde. Die Funktion liest den Inhalt
     * des Drops aus und benachrichtigt die Listener
     */
    @SuppressWarnings("unchecked")
    public void drop(DropTargetDropEvent e) {
        logger.info("Drag: DROP " + e.getDropAction() + " : " + e.getSourceActions() + " - " + e.getSource() + " - ");
        filled = true;
        try {
            Transferable tr = e.getTransferable();
            e.acceptDrop(e.getDropAction());
            if (e.isDataFlavorSupported(DataFlavor.stringFlavor)) {

                String files = (String) tr.getTransferData(DataFlavor.stringFlavor);

                fireUIEvent(new UIEvent(this, UIEvent.UI_DRAG_AND_DROP, files));

            } else if (e.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {

                List list = (List) tr.getTransferData(DataFlavor.javaFileListFlavor);

                for (int t = 0; t < list.size(); t++) {
                    // JDUtilities.getController().loadContainerFile((File)
                    // list.get(t));
                    fireUIEvent(new UIEvent(this, UIEvent.UI_DRAG_AND_DROP, list.get(t)));

                }

            } else {
                logger.info("UU");
            }
            // e.dropComplete(true);
        } catch (Exception exc) {
            // e.rejectDrop();
            exc.printStackTrace();
        }
        repaint();
    }

    public void dropActionChanged(DropTargetDragEvent dtde) {
    }

    /**
     * Benachrichtigt alle Listener über ein Event
     * 
     * @param uiEvent
     */
    public void fireUIEvent(UIEvent uiEvent) {
        synchronized (uiListener) {
            Iterator<UIListener> recIt = uiListener.iterator();

            while (recIt.hasNext()) {
                recIt.next().uiEvent(uiEvent);
            }
        }
    }

    /**
     * Liefert die Höhe des Bildes zurück
     * 
     * @return Höhe des Bildes
     */
    public int getImageHeight() {
        return imageEmpty.getHeight(this);

    }

    /**
     * Liefert die Breite des Bildes zurück
     * 
     * @return Breite des Bildes
     */
    public int getImageWidth() {
        return imageEmpty.getWidth(this);
    }

    /**
     * Zeichnet die Komponente neu
     * 
     * @param g
     *            Graphicobjekt
     */
    @Override
    public void paintComponent(Graphics g) {
        if (filled) {
            g.drawImage(imageFilled, 0, 0, null);
        } else {
            g.drawImage(imageEmpty, 0, 0, null);
        }
    }

    /**
     * UI Remove Listener Funktion. Entfernt einen Listener
     * 
     * @param listener
     */
    public void removeUIListener(UIListener listener) {
        synchronized (uiListener) {
            uiListener.remove(listener);
        }
    }
}