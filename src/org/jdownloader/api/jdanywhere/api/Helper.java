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
                final boolean readL = fpkg.getModifyLock().readLock();
                try {
                    for (DownloadLink link : fpkg.getChildren()) {
                        if (link.getUniqueID().getID() == ID) {
                            return link;
                        }
                    }
                } finally {
                    fpkg.getModifyLock().readUnlock(readL);
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
                    return fpkg;
                }
            }
        } finally {
            dlc.readUnlock(b);
        }
        return null;
    }

    public static List<DownloadLink> getFilteredDownloadLinks(final List<Long> linkIds) {
        if (linkIds != null && linkIds.size() > 0) {
            DownloadController dlc = DownloadController.getInstance();
            boolean b = dlc.readLock();
            try {
                final int size = linkIds.size();
                List<DownloadLink> sdl = dlc.getChildrenByFilter(new AbstractPackageChildrenNodeFilter<DownloadLink>() {
                    @Override
                    public int returnMaxResults() {
                        return size;
                    }

                    @Override
                    public boolean acceptNode(DownloadLink node) {
                        return linkIds.contains(node.getUniqueID().getID());
                    }
                });
                return sdl;
            } finally {
                dlc.readUnlock(b);
            }
        }
        return null;
    }

    public static byte[] compress(String str) throws IOException {
        if (str == null || str.length() == 0) {
            return new byte[0];
        }
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final GZIPOutputStream gzip = new GZIPOutputStream(out);
        gzip.write(str.getBytes("UTF-8"));
        gzip.close();
        return out.toByteArray();
    }

    public static final Object REQUESTOR = new Object();

    public static String getMessage(DownloadLink link) {
        PluginProgress prog = link.getPluginProgress();
        if (prog != null) {
            return prog.getMessage(REQUESTOR);
        }
        ConditionalSkipReason conditionalSkipReason = link.getConditionalSkipReason();
        if (conditionalSkipReason != null && !conditionalSkipReason.isConditionReached()) {
            return conditionalSkipReason.getMessage(null, null);
        }
        SkipReason skipReason = link.getSkipReason();
        if (skipReason != null) {
            return skipReason.getExplanation(null);
        }
        FinalLinkState finalLinkState = link.getFinalLinkState();
        if (finalLinkState != null) {
            if (FinalLinkState.CheckFailed(finalLinkState)) {
                return finalLinkState.getExplanation(null, link);
            }
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
