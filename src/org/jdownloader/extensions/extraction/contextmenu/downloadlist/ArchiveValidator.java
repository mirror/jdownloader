package org.jdownloader.extensions.extraction.contextmenu.downloadlist;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.WeakHashMap;

import jd.controlling.linkcrawler.CrawledLink;
import jd.plugins.DownloadLink;

import org.appwork.controlling.SingleReachableState;
import org.jdownloader.extensions.extraction.Archive;
import org.jdownloader.extensions.extraction.ArchiveFactory;
import org.jdownloader.extensions.extraction.ExtractionExtension;
import org.jdownloader.extensions.extraction.bindings.crawledlink.CrawledLinkFactory;
import org.jdownloader.extensions.extraction.bindings.downloadlink.DownloadLinkArchiveFactory;
import org.jdownloader.extensions.extraction.bindings.file.FileArchiveFactory;
import org.jdownloader.gui.views.SelectionInfo;

public class ArchiveValidator {

    public static class ArchiveValidation {
        protected final SingleReachableState state    = new SingleReachableState("ArchiveValidation");
        protected volatile List<Archive>     archives = null;

        public void executeWhenReached(final Runnable run) {
            state.executeWhenReached(run);
        }

        public List<Archive> getArchives() {
            return archives;
        }

        public boolean isFinished() {
            return state.isReached();
        }

        public void waitForReached() throws InterruptedException {
            state.waitForReached();
        }
    }

    public static volatile ExtractionExtension                         EXTENSION;

    private static final WeakHashMap<SelectionInfo, ArchiveValidation> RESULTS = new WeakHashMap<SelectionInfo, ArchiveValidation>();

    public static ArchiveValidation validate(final SelectionInfo<?, ?> selection, final boolean async) {
        if (EXTENSION != null) {
            final ArchiveValidation result;
            final boolean newResult;
            synchronized (RESULTS) {
                final ArchiveValidation existingResult = RESULTS.get(selection);
                if (existingResult != null) {
                    newResult = false;
                    result = existingResult;
                } else {
                    newResult = true;
                    result = new ArchiveValidation();
                    RESULTS.put(selection, result);
                }
            }
            if (newResult) {
                if (async && selection.getChildren().size() > 0) {
                    final Thread thread = new Thread() {
                        public void run() {
                            try {
                                result.archives = getArchivesFromPackageChildren(selection.getChildren());
                            } finally {
                                result.state.setReached();
                            }
                        };
                    };
                    thread.setDaemon(true);
                    thread.setName("ArchiveValidation");
                    thread.start();
                } else {
                    try {
                        if (selection.getChildren().size() > 0) {
                            result.archives = getArchivesFromPackageChildren(selection.getChildren());
                        } else {
                            result.archives = new ArrayList<Archive>(0);
                        }
                    } finally {
                        result.state.setReached();
                    }
                }
                return result;
            } else {
                if (!async) {
                    try {
                        result.waitForReached();
                    } catch (final InterruptedException ignore) {
                    }
                }
                return result;
            }
        }
        final ArchiveValidation ret = new ArchiveValidation();
        ret.state.setReached();
        ret.archives = new ArrayList<Archive>(0);
        return ret;
    }

    public static List<Archive> getArchivesFromPackageChildren(List<? extends Object> nodes) {
        return getArchivesFromPackageChildren(nodes, -1);
    }

    public static List<Archive> getArchivesFromPackageChildren(List<? extends Object> nodes, int maxArchives) {
        final ExtractionExtension extractor = EXTENSION;
        final ArrayList<Archive> archives = new ArrayList<Archive>();
        HashSet<String> archiveIDs = null;
        if (extractor != null) {
            buildLoop: for (Object child : nodes) {
                if (child instanceof CrawledLink) {
                    final DownloadLink dlLink = ((CrawledLink) child).getDownloadLink();
                    if (dlLink != null && (Boolean.FALSE.equals(dlLink.isPartOfAnArchive()) || (archiveIDs != null && archiveIDs.contains(dlLink.getArchiveID())))) {
                        //
                        continue buildLoop;
                    }
                } else if (child instanceof DownloadLink) {
                    final DownloadLink dlLink = (DownloadLink) child;
                    if (Boolean.FALSE.equals(dlLink.isPartOfAnArchive()) || (archiveIDs != null && archiveIDs.contains(dlLink.getArchiveID()))) {
                        //
                        continue buildLoop;
                    }
                } else if (child instanceof ArchiveFactory) {
                    final ArchiveFactory af = ((ArchiveFactory) child);
                    if (Boolean.FALSE.equals(af.isPartOfAnArchive()) || (archiveIDs != null && archiveIDs.contains(af.getArchiveID()))) {
                        //
                        continue buildLoop;
                    }
                }
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
                    if (archiveIDs == null) {
                        archiveIDs = new HashSet<String>();
                    }
                    archiveIDs.add(archive.getArchiveID());
                    if (maxArchives > 0 && archives.size() >= maxArchives) {
                        return archives;
                    }
                }
            }
        }
        return archives;
    }
}
