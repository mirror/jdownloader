package org.jdownloader.gui.views.linkgrabber.quickfilter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.regex.Pattern;

import jd.SecondLevelLaunch;
import jd.controlling.linkcrawler.CrawledLink;

import org.jdownloader.controlling.filter.CompiledFiletypeFilter.ArchiveExtensions;
import org.jdownloader.controlling.filter.CompiledFiletypeFilter.AudioExtensions;
import org.jdownloader.controlling.filter.CompiledFiletypeFilter.ExtensionsFilterInterface;
import org.jdownloader.controlling.filter.CompiledFiletypeFilter.ImageExtensions;
import org.jdownloader.controlling.filter.CompiledFiletypeFilter.VideoExtensions;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.components.Header;
import org.jdownloader.gui.views.linkgrabber.LinkGrabberTable;
import org.jdownloader.translate._JDT;

public class QuickFilterTypeTable extends FilterTable {
    private static final long            serialVersionUID = 2109715691047942399L;
    private CopyOnWriteArrayList<Filter> allFilters       = new CopyOnWriteArrayList<Filter>();
    private CopyOnWriteArraySet<Filter>  enabledFilters   = new CopyOnWriteArraySet<Filter>();

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
                            final private String description = _JDT._.audiofilter_description();

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
                            final private String description = _JDT._.video_description();

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
                            final private String description = _JDT._.image_description();

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
                            final private String description = _JDT._.archive_description();

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
                        final ExtensionsFilterInterface other = new ExtensionsFilterInterface() {

                            @Override
                            public String name() {
                                return "OTHERS";
                            }

                            @Override
                            public boolean isSameExtensionGroup(ExtensionsFilterInterface extension) {
                                return !(extension instanceof Enum);
                            }

                            @Override
                            public Pattern getPattern() {
                                return null;
                            }

                            @Override
                            public String getIconID() {
                                return "file";
                            }

                            @Override
                            public String getDesc() {
                                return _GUI._.settings_linkgrabber_filter_others();
                            }

                            @Override
                            public Pattern compiledAllPattern() {
                                return null;
                            }
                        };
                        allFilters.add(filter = new ExtensionFilter(other) {
                            final private String description = _JDT._.other_files_description();

                            protected String getID() {
                                return "Type_Others";

                            }

                            public String getDescription() {
                                return description;
                            }

                            @Override
                            public boolean isFiltered(CrawledLink link) {
                                final ExtensionsFilterInterface extension = link.getLinkInfo().getExtension();
                                return extension == null || other.isSameExtensionGroup(extension);
                            }

                            @Override
                            public void setEnabled(boolean enabled) {
                                super.setEnabled(enabled);
                                QuickFilterTypeTable.this.setEnabled(enabled, this);
                            }
                        });
                        for (Filter filterCheck : allFilters) {
                            if (!filterCheck.isEnabled()) {
                                enabledFilters.add(filterCheck);
                            }
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
        final Filter exception = getFilterException();
        for (final Filter enabledFilter : enabledFilters) {
            if (enabledFilter == exception) {
                continue;
            }
            if (enabledFilter.isFiltered(v)) {
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
