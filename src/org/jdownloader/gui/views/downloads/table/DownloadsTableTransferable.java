package org.jdownloader.gui.views.downloads.table;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.util.ArrayList;

import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

import org.appwork.utils.ClipboardUtils;

public class DownloadsTableTransferable implements Transferable {
    /**
     * this DataFlavor is only used to signal that we might have filepackages or
     * downloadlinks available
     */
    public static final DataFlavor  DownloadsTableFlavor = new DataFlavor(DownloadsTableTransferable.class, DownloadsTableTransferable.class.getName());
    private ArrayList<FilePackage>  packages             = null;
    private ArrayList<DownloadLink> links                = null;
    private long                    controlledVersion;
    private DataFlavor[]            flavors;

    public DownloadsTableTransferable(ArrayList<FilePackage> packages, ArrayList<DownloadLink> links, long controlledVersion) {
        this.packages = packages;
        this.links = links;
        this.controlledVersion = controlledVersion;
        ArrayList<DataFlavor> availableFlavors = new ArrayList<DataFlavor>();
        if (packages != null && packages.size() > 0) {
            availableFlavors.add(FilePackagesDataFlavor.Flavor);
        }
        if (links != null && links.size() > 0) {
            availableFlavors.add(DownloadLinksDataFlavor.Flavor);
        }
        if (availableFlavors.size() > 0) {
            availableFlavors.add(DownloadsTableFlavor);
            availableFlavors.add(ClipboardUtils.uriListFlavor);
            availableFlavors.add(ClipboardUtils.stringFlavor);
        }
        this.flavors = availableFlavors.toArray(new DataFlavor[] {});
    }

    public DataFlavor[] getTransferDataFlavors() {
        return this.flavors.clone();
    }

    public boolean isDataFlavorSupported(DataFlavor wished) {
        if (wished != null) {
            for (DataFlavor exist : flavors) {
                if (exist.equals(wished)) return true;
            }
        }
        return false;
    }

    public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
        if (!this.isDataFlavorSupported(flavor)) throw new UnsupportedFlavorException(flavor);
        if (flavor.equals(DownloadsTableFlavor)) return controlledVersion;
        if (flavor.equals(DownloadLinksDataFlavor.Flavor)) return links;
        if (flavor.equals(FilePackagesDataFlavor.Flavor)) return packages;
        return null;
    }

    @SuppressWarnings("unchecked")
    public static ArrayList<FilePackage> getFilePackages(Transferable info) {
        try {
            ArrayList<FilePackage> ret = (ArrayList<FilePackage>) info.getTransferData(FilePackagesDataFlavor.Flavor);
            return ret;
        } catch (Throwable e) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    public static ArrayList<DownloadLink> getDownloadLinks(Transferable info) {
        try {
            ArrayList<DownloadLink> ret = (ArrayList<DownloadLink>) info.getTransferData(DownloadLinksDataFlavor.Flavor);
            return ret;
        } catch (Throwable e) {
            return null;
        }
    }

}
