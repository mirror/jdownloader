package org.jdownloader.api.jdanywhere.api;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.zip.GZIPOutputStream;

import jd.controlling.downloadcontroller.DownloadController;
import jd.controlling.packagecontroller.AbstractPackageChildrenNodeFilter;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginProgress;

import org.jdownloader.extensions.extraction.ExtractionStatus;
import org.jdownloader.plugins.ConditionalSkipReason;
import org.jdownloader.plugins.FinalLinkState;
import org.jdownloader.plugins.SkipReason;

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
        if (str == null || str.length() == 0) { return str.getBytes("UTF-8"); }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        GZIPOutputStream gzip = new GZIPOutputStream(out);
        gzip.write(str.getBytes("UTF-8"));
        gzip.close();
        return out.toByteArray();
    }

    public static String getMessage(DownloadLink link) {

        PluginProgress prog = link.getPluginProgress();
        if (prog != null) { return prog.getMessage(null); }
        ConditionalSkipReason conditionalSkipReason = link.getConditionalSkipReason();
        if (conditionalSkipReason != null && !conditionalSkipReason.isConditionReached()) { return conditionalSkipReason.getMessage(null, null); }
        SkipReason skipReason = link.getSkipReason();
        if (skipReason != null) { return skipReason.getExplanation(null, link); }
        FinalLinkState finalLinkState = link.getFinalLinkState();
        if (finalLinkState != null) {
            if (FinalLinkState.CheckFailed(finalLinkState)) { return finalLinkState.getExplanation(null, link); }
            ExtractionStatus extractionStatus = link.getExtractionStatus();
            if (extractionStatus != null) {
                switch (extractionStatus) {
                case ERROR:
                case ERROR_PW:
                case ERROR_CRC:
                case ERROR_NOT_ENOUGH_SPACE:
                case ERRROR_FILE_NOT_FOUND:
                    return extractionStatus.getExplanation();
                case SUCCESSFUL:
                    return extractionStatus.getExplanation();
                case RUNNING:
                    return extractionStatus.getExplanation();
                case IDLE:
                    break;
                default:
                    break;
                }
            }
            return finalLinkState.getExplanation(null, link);
        }
        return "";
    }
}
