package org.jdownloader.extensions.jdanywhere.api;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.zip.GZIPOutputStream;

import jd.controlling.downloadcontroller.DownloadController;
import jd.controlling.packagecontroller.AbstractPackageChildrenNodeFilter;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

public class Helper {

    public static DownloadLink getDownloadLinkFromID(long ID) {
        DownloadController dlc = DownloadController.getInstance();
        boolean b = dlc.readLock();
        try {
            for (FilePackage fpkg : dlc.getPackages()) {
                synchronized (fpkg) {
                    for (DownloadLink link : fpkg.getChildren()) {
                        if (link.getUniqueID().getID() == ID) { return link; }
                    }
                }
            }
        } finally {
            dlc.readUnlock(b);
        }
        return null;
    }

    public static FilePackage getFilePackageFromID(long ID) {

        DownloadController dlc = DownloadController.getInstance();
        boolean b = dlc.readLock();
        try {
            for (FilePackage fpkg : dlc.getPackages()) {
                if (fpkg.getUniqueID().getID() == ID) {
                    synchronized (fpkg) {
                        return fpkg;
                    }
                }
            }
        } finally {
            dlc.readUnlock(b);
        }
        return null;
    }

    public static List<DownloadLink> getFilteredDownloadLinks(final List<Long> linkIds) {
        if (linkIds == null) return null;

        DownloadController dlc = DownloadController.getInstance();
        List<DownloadLink> sdl;

        boolean b = dlc.readLock();
        try {
            sdl = dlc.getChildrenByFilter(new AbstractPackageChildrenNodeFilter<DownloadLink>() {
                @Override
                public int returnMaxResults() {
                    return 0;
                }

                @Override
                public boolean acceptNode(DownloadLink node) {
                    if (linkIds.contains(node.getUniqueID().getID())) return true;
                    return false;
                }
            });
        } finally {
            dlc.readUnlock(b);
        }
        return sdl;
    }

    public static byte[] compress(String str) throws IOException {
        if (str == null || str.length() == 0) { return str.getBytes(); }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        GZIPOutputStream gzip = new GZIPOutputStream(out);
        gzip.write(str.getBytes());
        gzip.close();
        return out.toByteArray();
    }
}
