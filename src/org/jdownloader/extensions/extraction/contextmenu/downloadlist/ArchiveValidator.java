package org.jdownloader.extensions.extraction.contextmenu.downloadlist;

import java.util.ArrayList;
import java.util.List;
import java.util.WeakHashMap;

import org.appwork.controlling.SingleReachableState;
import org.jdownloader.extensions.extraction.Archive;
import org.jdownloader.extensions.extraction.ExtractionExtension;
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
        if (extractor != null) {
            return extractor.getArchivesFromPackageChildren(nodes, null, maxArchives);
        } else {
            return new ArrayList<Archive>(0);
        }
    }
}
