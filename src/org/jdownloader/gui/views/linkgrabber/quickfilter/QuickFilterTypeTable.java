package org.jdownloader.gui.views.linkgrabber.quickfilter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;

import jd.SecondLevelLaunch;
import jd.controlling.linkcrawler.CrawledLink;

import org.jdownloader.controlling.filter.CompiledFiletypeFilter.ArchiveExtensions;
import org.jdownloader.controlling.filter.CompiledFiletypeFilter.AudioExtensions;
import org.jdownloader.controlling.filter.CompiledFiletypeFilter.ImageExtensions;
import org.jdownloader.controlling.filter.CompiledFiletypeFilter.VideoExtensions;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.components.Header;
import org.jdownloader.gui.views.linkgrabber.LinkGrabberTable;
import org.jdownloader.images.NewTheme;
import org.jdownloader.translate._JDT;

public class QuickFilterTypeTable extends FilterTable {
    private static final long            serialVersionUID = 2109715691047942399L;
    private CopyOnWriteArrayList<Filter> allFilters       = new CopyOnWriteArrayList<Filter>();
    private CopyOnWriteArraySet<Filter>  enabledFilters   = new CopyOnWriteArraySet<Filter>();
    private HashMap<String, Filter>      fastCheck        = new HashMap<String, Filter>();

    public QuickFilterTypeTable(Header filetypeFilter, LinkGrabberTable table2Filter) {
        super(filetypeFilter, table2Filter, org.jdownloader.settings.staticreferences.CFG_LINKFILTER.LINKGRABBER_FILETYPE_QUICKFILTER_ENABLED);
        init();
    }

    @Override
    protected FilterTableDataUpdater getFilterTableDataUpdater() {
        final List<Filter> availableFilters = new ArrayList<Filter>(allFilters);
        return new FilterTableDataUpdater() {
            private Set<Filter> usedFilters = new HashSet<Filter>();

            @Override
            public void updateVisible(CrawledLink link) {
                for (Filter filter : availableFilters) {
                    if (((ExtensionFilter) filter).isFiltered(link)) {
                        usedFilters.add(filter);
                        filter.increaseCounter();
                        break;
                    }
                }
            }

            @Override
            public void reset() {
                for (Filter filter : availableFilters) {
                    filter.resetCounter();
                }
            }

            @Override
            public void afterVisible() {
                availableFilters.removeAll(usedFilters);
            }

            @Override
            public void updateFiltered(CrawledLink link) {
                if (availableFilters.size() > 0) {
                    Iterator<Filter> it = availableFilters.iterator();
                    while (it.hasNext()) {
                        Filter filter = it.next();
                        if (((ExtensionFilter) filter).isFiltered(link)) {
                            usedFilters.add(filter);
                            it.remove();
                            break;
                        }
                    }
                }
            }

            @Override
            public List<Filter> finalizeUpdater() {
                return new ArrayList<Filter>(usedFilters);
            }

            @Override
            public FilterTable getFilterTable() {
                return QuickFilterTypeTable.this;
            }

            @Override
            public boolean hasNewDisabledFilters() {
                return false;
            }

        };
    }

    private void setEnabled(boolean enabled, ExtensionFilter filter) {
        if (!enabled) {
            enabledFilters.add(filter);
        } else {
            enabledFilters.remove(filter);
        }
        synchronized (fastCheck) {
            fastCheck.clear();
        }
        getLinkgrabberTable().getModel().recreateModel(false);
    }

    public void init() {
        SecondLevelLaunch.INIT_COMPLETE.executeWhenReached(new Runnable() {
            public void run() {
                new Thread("QuickFilterTypeTable") {
                    @Override
                    public void run() {
                        final ArrayList<ExtensionFilter> knownExtensionFilters = new ArrayList<ExtensionFilter>();
                        ExtensionFilter filter = null;
                        allFilters.add(filter = new ExtensionFilter(AudioExtensions.AA) {
                            private String description = _JDT._.audiofilter_description();

                            protected String getID() {
                                return "Type_Audio";
                            }

                            public String getDescription() {
                                return description;
                            }

                            @Override
                            public void setEnabled(boolean enabled) {
                                super.setEnabled(enabled);
                                QuickFilterTypeTable.this.setEnabled(enabled, this);
                            }

                        });
                        knownExtensionFilters.add(filter);
                        allFilters.add(filter = new ExtensionFilter(VideoExtensions.ASF) {
                            private String description = _JDT._.video_description();

                            protected String getID() {
                                return "Type_Video";
                            }

                            public String getDescription() {
                                return description;
                            }

                            @Override
                            public void setEnabled(boolean enabled) {
                                super.setEnabled(enabled);
                                QuickFilterTypeTable.this.setEnabled(enabled, this);

                            }
                        });
                        knownExtensionFilters.add(filter);
                        allFilters.add(filter = new ExtensionFilter(ImageExtensions.BMP) {
                            private String description = _JDT._.image_description();

                            protected String getID() {
                                return "Type_Image";
                            }

                            public String getDescription() {
                                return description;
                            }

                            @Override
                            public void setEnabled(boolean enabled) {
                                super.setEnabled(enabled);
                                QuickFilterTypeTable.this.setEnabled(enabled, this);
                            }
                        });
                        knownExtensionFilters.add(filter);
                        allFilters.add(filter = new ExtensionFilter(ArchiveExtensions.ACE) {
                            private String description = _JDT._.archive_description();

                            protected String getID() {
                                return "Type_Archive";
                            }

                            public String getDescription() {
                                return description;
                            }

                            @Override
                            public void setEnabled(boolean enabled) {
                                super.setEnabled(enabled);
                                QuickFilterTypeTable.this.setEnabled(enabled, this);
                            }
                        });
                        knownExtensionFilters.add(filter);
                        knownExtensionFilters.trimToSize();
                        /*
                         * now we add special extensionfilter which will handle all unknown extensions
                         */
                        allFilters.add(filter = new ExtensionFilter(_GUI._.settings_linkgrabber_filter_others(), NewTheme.I().getIcon("file", 16), false) {
                            private String description = _JDT._.other_files_description();

                            protected String getID() {
                                return "Type_Others";

                            }

                            public String getDescription() {
                                return description;
                            }

                            @Override
                            protected boolean isFiltered(String ext) {
                                if (ext == null) return true;
                                for (ExtensionFilter filter : knownExtensionFilters) {
                                    if (filter.isFiltered(ext)) return false;
                                }
                                return true;
                            }

                            @Override
                            public void setEnabled(boolean enabled) {
                                super.setEnabled(enabled);
                                QuickFilterTypeTable.this.setEnabled(enabled, this);
                            }
                        });
                        for (Filter filterCheck : allFilters) {
                            if (!filterCheck.isEnabled()) enabledFilters.add(filterCheck);
                        }
                        requestUpdate();
                    }
                }.start();
            }
        });
    }

    @Override
    public int getComplexity() {
        return 1;
    }

    @Override
    public boolean isFiltered(CrawledLink v) {
        Filter exception = getFilterException();
        String ext = v.getExtension();
        Filter filter = null;
        synchronized (fastCheck) {
            filter = fastCheck.get(ext);
        }
        if (filter != null) return filter != exception && filter.isFiltered(v);
        for (Filter enabledFilter : enabledFilters) {
            if (enabledFilter == exception) continue;
            if (enabledFilter.isFiltered(v)) {
                synchronized (fastCheck) {
                    fastCheck.put(ext, enabledFilter);
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isFilteringChildrenNodes() {
        return isEnabled() && enabledFilters.size() > 0;
    }

}
