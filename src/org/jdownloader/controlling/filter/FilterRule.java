package org.jdownloader.controlling.filter;

import java.util.ArrayList;

import jd.controlling.linkcrawler.CrawledLink;
import jd.gui.swing.jdgui.views.settings.panels.linkgrabberfilter.editdialog.OnlineStatusFilter;
import jd.gui.swing.jdgui.views.settings.panels.linkgrabberfilter.editdialog.PluginStatusFilter;

import org.appwork.storage.Storable;
import org.appwork.utils.Files;
import org.appwork.utils.formatter.SizeFormatter;
import org.jdownloader.gui.translate._GUI;

public abstract class FilterRule implements Storable {

    private FilesizeFilter     filesizeFilter;
    private RegexFilter        hosterURLFilter;
    private RegexFilter        sourceURLFilter;
    private OnlineStatusFilter onlineStatusFilter;
    private BooleanFilter      matchesAlwaysFilter;
    private String             iconKey;
    private String             testUrl;
    private long               created = System.currentTimeMillis();

    public long getCreated() {
        return created;
    }

    public void setCreated(long created) {
        this.created = created;
    }

    public String getTestUrl() {
        return testUrl;
    }

    public void setTestUrl(String testUrl) {
        this.testUrl = testUrl;
    }

    private PluginStatusFilter pluginStatusFilter;

    public String getIconKey() {
        return iconKey;
    }

    public void setIconKey(String iconKey) {
        this.iconKey = iconKey;
    }

    public BooleanFilter getMatchAlwaysFilter() {
        if (matchesAlwaysFilter == null) matchesAlwaysFilter = new BooleanFilter(false);
        return matchesAlwaysFilter;
    }

    public void setMatchAlwaysFilter(BooleanFilter match) {
        this.matchesAlwaysFilter = match;
    }

    public FilesizeFilter getFilesizeFilter() {
        if (filesizeFilter == null) filesizeFilter = new FilesizeFilter();
        return filesizeFilter;
    }

    public void setFilesizeFilter(FilesizeFilter size) {
        this.filesizeFilter = size;
    }

    /**
     * Returns false if now filterrule is enabled
     * 
     * @return
     */
    public boolean isValid() {
        return getMatchAlwaysFilter().isEnabled() || getFilenameFilter().isEnabled() || getFilesizeFilter().isEnabled() || getFiletypeFilter().isEnabled() || getHosterURLFilter().isEnabled() || getSourceURLFilter().isEnabled() || getOnlineStatusFilter().isEnabled() || getPluginStatusFilter().isEnabled();
    }

    public String toString(CrawledLink link) {
        StringBuilder sb = new StringBuilder();
        ArrayList<String> cond = new ArrayList<String>();
        if (getMatchAlwaysFilter().isEnabled()) {
            cond.add(getMatchAlwaysFilter().toString());
        } else {
            if (getOnlineStatusFilter().isEnabled()) {
                cond.add(onlineStatusFilter.toString());
            }

            if (getPluginStatusFilter().isEnabled()) {
                cond.add(pluginStatusFilter.toString());
            }
            if (getFilenameFilter().isEnabled()) {
                if (link != null && link.getName() != null) {
                    cond.add(_GUI._.FilterRule_toString_name2(link.getName(), filenameFilter.toString()));
                } else {
                    cond.add(_GUI._.FilterRule_toString_name(filenameFilter.toString()));
                }

            }
            if (getFilesizeFilter().isEnabled()) {
                if (link != null && link.getSize() > 0) {
                    cond.add(_GUI._.FilterRule_toString_size2(SizeFormatter.formatBytes(link.getSize()), filesizeFilter.toString()));
                } else {
                    cond.add(_GUI._.FilterRule_toString_size(filesizeFilter.toString()));
                }

            }
            if (getFiletypeFilter().isEnabled()) {
                if (link != null && link.getName() != null && Files.getExtension(link.getName()) != null) {
                    String ext = Files.getExtension(link.getName());
                    cond.add(_GUI._.FilterRule_toString_type2(ext, filetypeFilter.toString()));
                } else {
                    cond.add(_GUI._.FilterRule_toString_type(filetypeFilter.toString()));
                }

            }
            if (getHosterURLFilter().isEnabled()) {
                if (link != null) {
                    cond.add(_GUI._.FilterRule_toString_hoster2(link.getURL(), hosterURLFilter.toString()));
                } else {
                    cond.add(_GUI._.FilterRule_toString_hoster(hosterURLFilter.toString()));
                }

            }
            if (getSourceURLFilter().isEnabled()) {

                cond.add(_GUI._.FilterRule_toString_source(sourceURLFilter.toString()));

            }
        }
        for (int i = 0; i < cond.size(); i++) {
            if (i > 0) {
                if (i < cond.size() - 1) {
                    sb.append(_GUI._.FilterRule_toString_comma(cond.get(i)));
                } else {
                    sb.append(" " + _GUI._.FilterRule_toString_and(cond.get(i)).trim());
                }

            } else {
                sb.append(cond.get(i));
            }

        }
        return sb.toString();
    }

    public String toString() {
        return toString(null);
    }

    public RegexFilter getHosterURLFilter() {
        if (hosterURLFilter == null) hosterURLFilter = new RegexFilter();
        return hosterURLFilter;
    }

    public void setHosterURLFilter(RegexFilter hoster) {
        this.hosterURLFilter = hoster;
    }

    public RegexFilter getSourceURLFilter() {
        if (sourceURLFilter == null) sourceURLFilter = new RegexFilter();
        return sourceURLFilter;
    }

    public void setSourceURLFilter(RegexFilter source) {
        this.sourceURLFilter = source;
    }

    public FiletypeFilter getFiletypeFilter() {
        if (filetypeFilter == null) filetypeFilter = new FiletypeFilter();
        return filetypeFilter;
    }

    public void setFiletypeFilter(FiletypeFilter type) {
        this.filetypeFilter = type;
    }

    public void setOnlineStatusFilter(OnlineStatusFilter onlineStatusFilter) {
        this.onlineStatusFilter = onlineStatusFilter;
    }

    public OnlineStatusFilter getOnlineStatusFilter() {
        if (onlineStatusFilter == null) onlineStatusFilter = new OnlineStatusFilter();
        return onlineStatusFilter;
    }

    public void setPluginStatusFilter(PluginStatusFilter pluginStatusFilter) {
        this.pluginStatusFilter = pluginStatusFilter;
    }

    public PluginStatusFilter getPluginStatusFilter() {
        if (pluginStatusFilter == null) pluginStatusFilter = new PluginStatusFilter();
        return pluginStatusFilter;
    }

    public RegexFilter getFilenameFilter() {
        if (filenameFilter == null) filenameFilter = new RegexFilter();
        return filenameFilter;
    }

    public void setFilenameFilter(RegexFilter filename) {
        this.filenameFilter = filename;
    }

    private FiletypeFilter filetypeFilter;
    private RegexFilter    filenameFilter;

    private boolean        enabled;

    private String         name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isEnabled() {
        return enabled && isValid();
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;

    }
}
