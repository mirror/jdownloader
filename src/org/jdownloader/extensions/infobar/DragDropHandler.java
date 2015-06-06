package org.jdownloader.extensions.infobar;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;

import javax.swing.JComponent;
import javax.swing.TransferHandler;

import jd.controlling.ClipboardMonitoring;
import jd.controlling.linkcollector.LinkOrigin;

public class DragDropHandler extends TransferHandler {

    private static final long serialVersionUID = 2254473504071024802L;

    @Override
    public boolean canImport(TransferSupport support) {
        return isDataFlavorSupported(support.getDataFlavors());
    }

    @Override
    public boolean canImport(JComponent comp, DataFlavor[] transferFlavors) {
        return isDataFlavorSupported(transferFlavors);
    }

    @Override
    public boolean importData(TransferSupport support) {
        return importTransferable(support.getTransferable());
    }

    @Override
    public boolean importData(JComponent comp, Transferable t) {
        return importTransferable(t);
    }

    private static final boolean isDataFlavorSupported(DataFlavor[] transferFlavors) {
        for (DataFlavor flavor : transferFlavors) {
            if (flavor.equals(DataFlavor.javaFileListFlavor)) {
                return true;
            }
            if (flavor.equals(DataFlavor.stringFlavor)) {
                return true;
            }
        }
        return false;
    }

    private static final boolean importTransferable(Transferable t) {
        try {
            ClipboardMonitoring.processSupportedTransferData(t, LinkOrigin.TOOLBAR);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

}
