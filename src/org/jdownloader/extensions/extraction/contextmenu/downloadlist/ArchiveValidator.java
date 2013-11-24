package org.jdownloader.extensions.extraction.contextmenu.downloadlist;

import java.util.ArrayList;
import java.util.List;
import java.util.WeakHashMap;

import jd.controlling.linkcrawler.CrawledLink;
import jd.plugins.DownloadLink;

import org.jdownloader.extensions.extraction.Archive;
import org.jdownloader.extensions.extraction.ExtractionExtension;
import org.jdownloader.extensions.extraction.bindings.crawledlink.CrawledLinkFactory;
import org.jdownloader.extensions.extraction.bindings.downloadlink.DownloadLinkArchiveFactory;
import org.jdownloader.gui.views.SelectionInfo;

public class ArchiveValidator {

    public static ExtractionExtension                        EXTENSION;

    private static WeakHashMap<SelectionInfo, List<Archive>> VALIDATIONCACHE = new WeakHashMap<SelectionInfo, List<Archive>>();
    private static WeakHashMap<SelectionInfo, Object>        VALIDATIONLOCKS = new WeakHashMap<SelectionInfo, Object>();

    public static List<Archive> validate(SelectionInfo<?, ?> selection) {
        if (EXTENSION == null) return null;
        Object lock = null;
        List<Archive> validation = null;
        synchronized (VALIDATIONCACHE) {
            validation = VALIDATIONCACHE.get(selection);
            if (validation != null) return validation;
            synchronized (VALIDATIONLOCKS) {
                lock = VALIDATIONLOCKS.get(selection);
                if (lock == null) {
                    lock = new Object();
                    VALIDATIONLOCKS.put(selection, lock);
                }
            }
        }
        synchronized (lock) {
            try {
                synchronized (VALIDATIONCACHE) {
                    validation = VALIDATIONCACHE.get(selection);
                    if (validation != null) return validation;
                }
                validation = getArchives(selection);
                synchronized (VALIDATIONCACHE) {
                    VALIDATIONCACHE.put(selection, validation);
                }
                return validation;
            } finally {
                synchronized (VALIDATIONLOCKS) {
                    VALIDATIONLOCKS.remove(selection);
                }
            }
        }
    }

    public static List<Archive> getArchives(SelectionInfo<?, ?> si) {
        ExtractionExtension extractor = EXTENSION;
        ArrayList<Archive> archives = new ArrayList<Archive>();
        if (extractor == null) return archives;

        nextLink: for (Object l : si.getChildren()) {
            if (l instanceof CrawledLink) {
                // if (((CrawledLink) l).getLinkState() != LinkState.OFFLINE) {
                CrawledLinkFactory clf = new CrawledLinkFactory(((CrawledLink) l));
                if (extractor.isLinkSupported(clf)) {

                    for (Archive a : archives) {
                        if (a.contains(clf)) continue nextLink;
                    }

                    Archive archive = extractor.getArchiveByFactory(clf);
                    if (archive != null) {
                        archives.add(archive);
                    }

                    // }
                }
            } else if (l instanceof DownloadLink) {
                // if (((DownloadLink) l).isAvailable() || new File(((DownloadLink) l).getFileOutput()).exists()) {
                DownloadLinkArchiveFactory clf = new DownloadLinkArchiveFactory(((DownloadLink) l));
                if (extractor.isLinkSupported(clf)) {

                    for (Archive a : archives) {
                        if (a.contains(clf)) continue nextLink;
                    }

                    Archive archive = extractor.getArchiveByFactory(clf);
                    if (archive != null) {
                        archives.add(archive);
                    }

                }
                // }

            }
        }
        return archives;
    }
}
