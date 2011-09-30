package org.jdownloader.gui.views.linkgrabber;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.util.ArrayList;

import jd.controlling.linkcollector.LinkCollector;
import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.CrawledPackage;

import org.appwork.utils.ClipboardUtils;

public class LinkGrabberTransferable implements Transferable {
    /**
     * this DataFlavor is only used to signal that we might have filepackages or
     * downloadlinks available
     */
    public static final DataFlavor    LinkGrabberTableFlavor = new DataFlavor(LinkGrabberTransferable.class, LinkGrabberTransferable.class.getName()) {
                                                                 @Override
                                                                 public boolean isFlavorSerializedObjectType() {
                                                                     return false;
                                                                 }
                                                             };
    private ArrayList<CrawledPackage> packages               = null;
    private ArrayList<CrawledLink>    links                  = null;
    private long                      controlledVersion;
    private DataFlavor[]              flavors;

    public LinkGrabberTransferable(ArrayList<CrawledPackage> packages, ArrayList<CrawledLink> links, long controlledVersion) {
        this.packages = packages;
        this.links = links;
        this.controlledVersion = controlledVersion;
        ArrayList<DataFlavor> availableFlavors = new ArrayList<DataFlavor>();
        if (packages != null && packages.size() > 0) {
            availableFlavors.add(CrawledPackagesDataFlavor.Flavor);
        }
        if (links != null && links.size() > 0) {
            availableFlavors.add(CrawledLinksDataFlavor.Flavor);
        }
        if (availableFlavors.size() > 0) {
            availableFlavors.add(LinkGrabberTableFlavor);
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
        if (flavor.equals(LinkGrabberTableFlavor)) return controlledVersion;
        if (flavor.equals(CrawledLinksDataFlavor.Flavor)) return links;
        if (flavor.equals(CrawledPackagesDataFlavor.Flavor)) return packages;
        return null;
    }

    @SuppressWarnings("unchecked")
    public static ArrayList<CrawledPackage> getPackages(Transferable info) {
        try {
            ArrayList<CrawledPackage> ret = (ArrayList<CrawledPackage>) info.getTransferData(CrawledPackagesDataFlavor.Flavor);
            return ret;
        } catch (Throwable e) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    public static ArrayList<CrawledLink> getChildren(Transferable info) {
        try {
            ArrayList<CrawledLink> ret = (ArrayList<CrawledLink>) info.getTransferData(CrawledLinksDataFlavor.Flavor);
            return ret;
        } catch (Throwable e) {
            return null;
        }
    }

    public static boolean isVersionOkay(Transferable info) {
        try {
            long version = (Long) info.getTransferData(LinkGrabberTableFlavor);
            return LinkCollector.getInstance().getPackageControllerChanges() == version;
        } catch (Throwable e) {
            return false;
        }
    }

}
