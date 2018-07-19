package org.jdownloader.gui.views.downloads.table;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

import org.appwork.utils.ClipboardUtils;
import org.jdownloader.gui.views.components.packagetable.dragdrop.PackageControllerTableTransferable;

public class DownloadsTransferable extends PackageControllerTableTransferable<FilePackage, DownloadLink> {
    public DownloadsTransferable(PackageControllerTableTransferable<FilePackage, DownloadLink> transferable) {
        super(transferable);
        final List<DataFlavor> availableFlavors = new ArrayList<DataFlavor>();
        availableFlavors.add(FLAVOR);
        availableFlavors.add(ClipboardUtils.stringFlavor);
        flavors = availableFlavors.toArray(new DataFlavor[] {});
    }

    @Override
    public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
        if (!isDataFlavorSupported(flavor)) {
            throw new UnsupportedFlavorException(flavor);
        }
        if (flavor.equals(FLAVOR)) {
            return getSelectionInfo();
        }
        if (flavor.equals(ClipboardUtils.stringFlavor)) {
            if (stringContent != null) {
                return stringContent;
            } else {
                final StringBuilder sb = new StringBuilder();
                final Set<String> urls = getURLs();
                if (urls != null) {
                    final Iterator<String> it = urls.iterator();
                    while (it.hasNext()) {
                        if (sb.length() > 0) {
                            sb.append("\r\n");
                        }
                        sb.append(it.next());
                    }
                }
                return sb.toString();
            }
        }
        throw new UnsupportedFlavorException(flavor);
    }
}
