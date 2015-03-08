package org.jdownloader.extensions.extraction.contextmenu.downloadlist;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.WeakHashMap;

import jd.controlling.linkcrawler.CrawledLink;
import jd.plugins.DownloadLink;

import org.jdownloader.extensions.extraction.Archive;
import org.jdownloader.extensions.extraction.ArchiveFactory;
import org.jdownloader.extensions.extraction.ExtractionExtension;
import org.jdownloader.extensions.extraction.bindings.crawledlink.CrawledLinkFactory;
import org.jdownloader.extensions.extraction.bindings.downloadlink.DownloadLinkArchiveFactory;
import org.jdownloader.extensions.extraction.bindings.file.FileArchiveFactory;
import org.jdownloader.gui.views.SelectionInfo;

public class ArchiveValidator {

    public static ExtractionExtension                        EXTENSION;

    private static WeakHashMap<SelectionInfo, List<Archive>> VALIDATIONCACHE = new WeakHashMap<SelectionInfo, List<Archive>>();
    private static WeakHashMap<SelectionInfo, Object>        VALIDATIONLOCKS = new WeakHashMap<SelectionInfo, Object>();

    public static List<Archive> validate(SelectionInfo<?, ?> selection) {
        if (EXTENSION == null) {
            return null;
        }
        Object lock = null;
        List<Archive> validation = null;
        synchronized (VALIDATIONCACHE) {
            validation = VALIDATIONCACHE.get(selection);
            if (validation != null) {
                return validation;
            }
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
                    if (validation != null) {
                        return validation;
                    }
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
        return getArchivesFromPackageChildren(si.getChildren());
    }

    public static List<Archive> getArchivesFromPackageChildren(List<? extends Object> nodes) {
        final ExtractionExtension extractor = EXTENSION;
        final ArrayList<Archive> archives = new ArrayList<Archive>();
        if (extractor != null) {
            buildLoop: for (Object child : nodes) {
                for (Archive archive : archives) {
                    if (archive.contains(child)) {
                        continue buildLoop;
                    }
                }
                final ArchiveFactory af;
                if (child instanceof CrawledLink) {
                    af = new CrawledLinkFactory(((CrawledLink) child));
                } else if (child instanceof DownloadLink) {
                    af = new DownloadLinkArchiveFactory(((DownloadLink) child));
                } else if (child instanceof File) {
                    af = new FileArchiveFactory(((File) child));
                } else if (child instanceof ArchiveFactory) {
                    af = (ArchiveFactory) child;
                } else {
                    continue buildLoop;
                }
                final Archive archive = extractor.getArchiveByFactory(af);
                if (archive != null) {
                    archives.add(archive);
                }
            }
        }
        return archives;
    }

}
