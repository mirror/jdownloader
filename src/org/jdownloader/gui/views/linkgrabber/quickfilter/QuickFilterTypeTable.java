package org.jdownloader.gui.views.linkgrabber.quickfilter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.concurrent.CopyOnWriteArrayList;

import jd.SecondLevelLaunch;
import jd.controlling.linkcollector.LinkCollector;
import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.CrawledPackage;

import org.appwork.utils.Files;
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

    public QuickFilterTypeTable(Header filetypeFilter, LinkGrabberTable table2Filter) {
        super(filetypeFilter, table2Filter, org.jdownloader.settings.staticreferences.CFG_LINKFILTER.LINKGRABBER_FILETYPE_QUICKFILTER_ENABLED);
        init();
    }

    protected java.util.List<Filter> updateQuickFilerTableData() {

        // synchronized (LOCK) {
        /* reset existing filter counters */
        for (Filter filter : allFilters) {
            filter.setCounter(0);
        }
        /* update filter list */
        HashSet<Filter> filtersInUse = new HashSet<Filter>();
        HashSet<CrawledLink> map = new HashSet<CrawledLink>();
        java.util.List<CrawledLink> filteredLinks = new ArrayList<CrawledLink>();
        for (CrawledLink link : getVisibleLinks()) {
            map.add(link);
            /*
             * speed optimization, we dont want to get extension several times
             */
            String ext = Files.getExtension(link.getName());
            for (Filter filter : allFilters) {
                if (((ExtensionFilter) filter).isFiltered(ext)) {
                    filtersInUse.add(filter);
                    filter.setCounter(filter.getCounter() + 1);
                    break;
                }
            }
        }

        for (CrawledPackage pkg : LinkCollector.getInstance().getPackagesCopy()) {
            boolean readL = pkg.getModifyLock().readLock();
            try {
                for (CrawledLink link : pkg.getChildren()) {
                    if (map.add(link)) {
                        filteredLinks.add(link);
                    }
                    String ext = Files.getExtension(link.getName());
                    for (Filter filter : allFilters) {
                        if (((ExtensionFilter) filter).isFiltered(ext)) {
                            filtersInUse.add(filter);
                            break;
                        }
                    }
                }
            } finally {
                pkg.getModifyLock().readUnlock(readL);
            }
        }

        for (Filter filter : filtersInUse) {
            if (filter.getCounter() == 0) {
                filter.setCounter(getCountWithout(filter, filteredLinks));
            }
        }
        /* update FilterTableModel */
        // java.util.List<Filter> newfilters = new
        // java.util.List<Filter>();

        return new ArrayList<Filter>(filtersInUse);

    }

    public void init() {
        SecondLevelLaunch.INIT_COMPLETE.executeWhenReached(new Runnable() {
            public void run() {
                final ArrayList<ExtensionFilter> knownExtensionFilters = new ArrayList<ExtensionFilter>();
                ExtensionFilter filter = new ExtensionFilter(AudioExtensions.AA) {
                    protected String getID() {
                        return "Type_Audio";
                    }

                    public String getDescription() {
                        return _JDT._.audiofilter_description();
                    }

                    @Override
                    public void setEnabled(boolean enabled) {
                        super.setEnabled(enabled);
                        getLinkgrabberTable().getModel().recreateModel(false);
                        updateAllFiltersInstant();
                    }

                };
                allFilters.add(filter);
                knownExtensionFilters.add(filter);
                allFilters.add(filter = new ExtensionFilter(VideoExtensions.ASF) {
                    protected String getID() {
                        return "Type_Video";
                    }

                    public String getDescription() {
                        return _JDT._.video_description();
                    }

                    @Override
                    public void setEnabled(boolean enabled) {
                        super.setEnabled(enabled);
                        getLinkgrabberTable().getModel().recreateModel(false);
                        updateAllFiltersInstant();

                    }
                });
                knownExtensionFilters.add(filter);
                allFilters.add(filter = new ExtensionFilter(ImageExtensions.BMP) {
                    protected String getID() {
                        return "Type_Image";
                    }

                    public String getDescription() {
                        return _JDT._.image_description();
                    }

                    @Override
                    public void setEnabled(boolean enabled) {
                        super.setEnabled(enabled);
                        getLinkgrabberTable().getModel().recreateModel(false);
                        updateAllFiltersInstant();
                    }
                });
                knownExtensionFilters.add(filter);
                allFilters.add(filter = new ExtensionFilter(ArchiveExtensions.ACE) {
                    protected String getID() {
                        return "Type_Archive";
                    }

                    public String getDescription() {
                        return _JDT._.archive_description();
                    }

                    @Override
                    public void setEnabled(boolean enabled) {
                        super.setEnabled(enabled);
                        getLinkgrabberTable().getModel().recreateModel(false);
                        updateAllFiltersInstant();
                    }
                });
                knownExtensionFilters.add(filter);
                knownExtensionFilters.trimToSize();
                /*
                 * now we add special extensionfilter which will handle all unknown extensions
                 */
                allFilters.add(new ExtensionFilter(_GUI._.settings_linkgrabber_filter_others(), NewTheme.I().getIcon("file", 16), false) {
                    protected String getID() {
                        return "Type_Others";

                    }

                    public String getDescription() {
                        return _JDT._.other_files_description();
                    }

                    @Override
                    public boolean isFiltered(String ext) {
                        if (ext == null) return true;
                        for (ExtensionFilter filter : knownExtensionFilters) {
                            if (filter.isFiltered(ext)) return false;
                        }
                        return true;
                    }

                    @Override
                    public void setEnabled(boolean enabled) {
                        super.setEnabled(enabled);
                        getLinkgrabberTable().getModel().recreateModel(false);
                        updateAllFiltersInstant();
                    }
                });
            }
        });
    }

    public void reset() {
        Collection<Filter> lfilters = filters;
        for (Filter filter : lfilters) {
            filter.setCounter(0);
        }
    }

    @Override
    java.util.List<Filter> getAllFilters() {
        return allFilters;
    }

    public boolean highlightFilter() {
        return false;
    }

}
