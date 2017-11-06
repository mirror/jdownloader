package org.jdownloader.gui.views.linkgrabber.addlinksdialog;

import java.awt.Image;
import java.awt.Point;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.InputEvent;
import java.io.IOException;
import java.lang.reflect.Method;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.TransferHandler;

import jd.controlling.ClipboardMonitoring;
import jd.controlling.ClipboardMonitoring.HTMLFragment;
import jd.parser.html.HTMLParser;

import org.appwork.swing.components.ExtTextArea;

public class DragAndDropDelegater extends TransferHandler {
    private final TransferHandler org;

    public DragAndDropDelegater(ExtTextArea input) {
        org = input.getTransferHandler();
    }

    public void setDragImage(Image img) {
        org.setDragImage(img);
    }

    public Image getDragImage() {
        return org.getDragImage();
    }

    public void setDragImageOffset(Point p) {
        org.setDragImageOffset(p);
    }

    public Point getDragImageOffset() {
        return org.getDragImageOffset();
    }

    @Override
    public void exportAsDrag(JComponent comp, InputEvent e, int action) {
        org.exportAsDrag(comp, e, action);
    }

    @Override
    public void exportToClipboard(JComponent comp, Clipboard clip, int action) throws IllegalStateException {
        org.exportToClipboard(comp, clip, action);
    }

    @Override
    public boolean importData(TransferSupport support) {
        try {
            final HTMLFragment htmlFragment = ClipboardMonitoring.getHTMLFragment(support.getTransferable(), support.getDataFlavors());
            if (htmlFragment != null) {
                final String[] links = HTMLParser.getHttpLinks(htmlFragment.getFragment(), htmlFragment.getSourceURL());
                if (links != null && links.length > 0) {
                    final StringBuilder sb = new StringBuilder();
                    for (final String link : links) {
                        if (sb.length() > 0) {
                            sb.append("\r\n");
                        }
                        sb.append(link);
                    }
                    final TransferSupport ret = new TransferSupport(support.getComponent(), new StringSelection(sb.toString()));
                    if (support.isDrop()) {
                        ret.setDropAction(support.getDropAction());
                    }
                    return org.importData(ret);
                }
            }
        } catch (UnsupportedFlavorException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return org.importData(support);
    }

    @Override
    public boolean importData(JComponent comp, Transferable t) {
        return org.importData(comp, t);
    }

    @Override
    public boolean canImport(TransferSupport support) {
        return org.canImport(support);
    }

    @Override
    public boolean canImport(JComponent comp, DataFlavor[] transferFlavors) {
        return org.canImport(comp, transferFlavors);
    }

    @Override
    public int getSourceActions(JComponent c) {
        return org.getSourceActions(c);
    }

    @Override
    public Icon getVisualRepresentation(Transferable t) {
        return org.getVisualRepresentation(t);
    }

    @Override
    protected Transferable createTransferable(JComponent c) {
        try {
            final Method method = TransferHandler.class.getDeclaredMethod("createTransferable", new Class[] { JComponent.class });
            method.setAccessible(true);
            return (Transferable) method.invoke(org, new Object[] { c });
        } catch (Throwable e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    protected void exportDone(JComponent source, Transferable data, int action) {
        try {
            final Method method = TransferHandler.class.getDeclaredMethod("exportDone", new Class[] { JComponent.class, Transferable.class, int.class });
            method.setAccessible(true);
            method.invoke(org, new Object[] { source, data, action });
        } catch (Throwable e) {
            e.printStackTrace();
            return;
        }
    }
}
