//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
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
import java.util.List;
import java.util.Vector;
import java.util.logging.Logger;

import javax.swing.JComponent;

import jd.event.UIEvent;
import jd.event.UIListener;
import jd.utils.JDTheme;
import jd.utils.JDUtilities;

/**
 * Diese Komponente ist ein Droptarget. und nimmt gedroppte Strings auf
 * 
 * @author JD-Team
 */
public class DragNDrop extends JComponent implements DropTargetListener {

    private static final long serialVersionUID = -3280613281656283625L;

    private Image image;

    private Logger logger = JDUtilities.getLogger();

    /**
     * Hiermit wird der Eventmechanismus realisiert. Alle hier eingetragenen
     * Listener werden benachrichtigt, wenn mittels
     * {@link #fireUIEvent(UIEvent)} ein Event losgeschickt wird.
     */
    public Vector<UIListener> uiListener = null;

    public DragNDrop() {
        new DropTarget(this, this);
        uiListener = new Vector<UIListener>();
        image = JDUtilities.getImage(JDTheme.V("gui.images.clipboard"));
        if (image != null) {
            setPreferredSize(new Dimension(image.getWidth(null), image.getHeight(null)));
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

    public void dragEnter(DropTargetDragEvent dtde) {
    }

    public void dragExit(DropTargetEvent dte) {
    }

    public void dragOver(DropTargetDragEvent dtde) {
    }

    /**
     * Wird aufgerufen sobald etwas gedropt wurde. Die Funktion liest den Inhalt
     * des Drops aus und benachrichtigt die Listener
     */
    @SuppressWarnings("unchecked")
    public void drop(DropTargetDropEvent dtde) {
        logger.info("Drag: DROP " + dtde.getDropAction() + " : " + dtde.getSourceActions() + " - " + dtde.getSource());
        try {
            Transferable tr = dtde.getTransferable();
            dtde.acceptDrop(dtde.getDropAction());
            if (dtde.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                String files = (String) tr.getTransferData(DataFlavor.stringFlavor);
                fireUIEvent(new UIEvent(this, UIEvent.UI_DRAG_AND_DROP, files));
            } else if (dtde.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                List list = (List) tr.getTransferData(DataFlavor.javaFileListFlavor);
                for (int t = 0; t < list.size(); t++) {
                    fireUIEvent(new UIEvent(this, UIEvent.UI_DRAG_AND_DROP, list.get(t)));
                }
            } else {
                logger.info("Unsupported Drop-Type");
            }
        } catch (Exception e) {
            e.printStackTrace();
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
            for (UIListener lstn : uiListener) {
                lstn.uiEvent(uiEvent);
            }
        }
    }

    /**
     * Zeichnet die Komponente neu
     * 
     * @param g
     *            Graphicobjekt
     */
    @Override
    public void paintComponent(Graphics g) {
        g.drawImage(image, 0, 0, null);
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