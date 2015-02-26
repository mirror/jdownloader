package org.jdownloader.extensions.extraction.contextmenu.downloadlist;

import java.util.ArrayList;
import java.util.List;
import java.util.WeakHashMap;

import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.packagecontroller.AbstractPackageChildrenNode;
import jd.plugins.DownloadLink;

import org.jdownloader.extensions.extraction.Archive;
import org.jdownloader.extensions.extraction.ArchiveFactory;
import org.jdownloader.extensions.extraction.ExtractionExtension;
import org.jdownloader.extensions.extraction.bindings.crawledlink.CrawledLinkFactory;
import org.jdownloader.extensions.extraction.bindings.downloadlink.DownloadLinkArchiveFactory;
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
        final ExtractionExtension extractor = EXTENSION;
        if (extractor != null) {
            final List<AbstractPackageChildrenNode> children = new ArrayList<AbstractPackageChildrenNode>();
            for (Object l : si.getChildren()) {
                if (l instanceof AbstractPackageChildrenNode) {
                    children.add((AbstractPackageChildrenNode) l);
                }
            }
            return getArchivesFromPackageChildren(children);
        }
        return new ArrayList<Archive>();
    }

    public static List<Archive> getArchivesFromArchiveFactories(List<ArchiveFactory> archiveFactories) {
        final ExtractionExtension extractor = EXTENSION;
        final ArrayList<Archive> archives = new ArrayList<Archive>();
        if (extractor != null && archiveFactories != null) {
            for (ArchiveFactory af : archiveFactories) {
                for (Archive archive : archives) {
                    if (archive.contains(af)) {
                        continue;
                    }
                }
                final Archive archive = extractor.getArchiveByFactory(af);
                if (archive != null) {
                    archives.add(archive);
                }
            }
        }
        return archives;
    }

    public static List<Archive> getArchivesFromPackageChildren(List<AbstractPackageChildrenNode> childrenNodes) {
        final ExtractionExtension extractor = EXTENSION;
        if (extractor != null && childrenNodes != null) {
            final List<ArchiveFactory> archiveFactories = new ArrayList<ArchiveFactory>();
            for (AbstractPackageChildrenNode l : childrenNodes) {
                if (l instanceof CrawledLink) {
                    archiveFactories.add(new CrawledLinkFactory(((CrawledLink) l)));
                } else if (l instanceof DownloadLink) {
                    archiveFactories.add(new DownloadLinkArchiveFactory(((DownloadLink) l)));
                } else {
                    continue;
                }
            }
            return getArchivesFromArchiveFactories(archiveFactories);
        }
        return new ArrayList<Archive>();
    }
}
