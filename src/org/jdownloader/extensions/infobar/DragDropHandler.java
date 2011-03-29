package org.jdownloader.extensions.infobar;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.io.File;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.TransferHandler;

import jd.controlling.JDController;

import org.appwork.utils.Regex;

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
            if (flavor.equals(DataFlavor.javaFileListFlavor)) return true;
            if (flavor.equals(DataFlavor.stringFlavor)) return true;
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private static final boolean importTransferable(Transferable t) {
        try {

            if (t.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                String files = (String) t.getTransferData(DataFlavor.stringFlavor);

                String linuxfiles[] = new Regex(files, "file://(.*?)(\r\n|\r|\n)").getColumn(0);
                if (linuxfiles != null && linuxfiles.length > 0) {
                    for (String file : linuxfiles) {
                        JDController.loadContainerFile(new File(file.trim()));
                    }
                } else {
                    JDController.distributeLinks(files);
                }

                return true;
            } else if (t.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                List<File> files = (List<File>) t.getTransferData(DataFlavor.javaFileListFlavor);
                // TODO
                // OptionalPluginWrapper unrar =
                // JDUtilities.getOptionalPlugin("unrar");
                // boolean extract = (unrar != null && unrar.isEnabled());
                // if (extract)
                // JDController.getInstance().fireControlEvent(ControlEvent.CONTROL_ON_FILEOUTPUT,
                // files.toArray(new File[] {}));

                for (File file : files) {
                    JDController.loadContainerFile(file);
                }

                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

}
