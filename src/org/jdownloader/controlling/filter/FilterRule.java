package org.jdownloader.controlling.filter;

import java.util.ArrayList;

import jd.controlling.linkcrawler.CrawledLink;

import org.appwork.storage.Storable;
import org.appwork.utils.Files;
import org.appwork.utils.formatter.SizeFormatter;
import org.jdownloader.gui.translate._GUI;

public abstract class FilterRule implements Storable {
    private FilesizeFilter filesizeFilter;
    private RegexFilter    hosterURLFilter;
    private RegexFilter    sourceURLFilter;

    public FilesizeFilter getFilesizeFilter() {
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
        return getFilenameFilter().isEnabled() || getFilesizeFilter().isEnabled() || getFiletypeFilter().isEnabled() || getHosterURLFilter().isEnabled() || getSourceURLFilter().isEnabled();
    }

    public String toString(CrawledLink link) {
        StringBuilder sb = new StringBuilder();
        ArrayList<String> cond = new ArrayList<String>();
        if (filenameFilter.isEnabled()) {
            if (link != null && link.getName() != null) {
                cond.add(_GUI._.FilterRule_toString_name2(link.getName(), filenameFilter.toString()));
            } else {
                cond.add(_GUI._.FilterRule_toString_name(filenameFilter.toString()));
            }

        }
        if (filesizeFilter.isEnabled()) {
            if (link != null && link.getSize() > 0) {
                cond.add(_GUI._.FilterRule_toString_size2(SizeFormatter.formatBytes(link.getSize()), filesizeFilter.toString()));
            } else {
                cond.add(_GUI._.FilterRule_toString_size(filesizeFilter.toString()));
            }

        }
        if (filetypeFilter.isEnabled()) {
            if (link != null && link.getName() != null && Files.getExtension(link.getName()) != null) {
                String ext = Files.getExtension(link.getName());
                cond.add(_GUI._.FilterRule_toString_type2(ext, filetypeFilter.toString()));
            } else {
                cond.add(_GUI._.FilterRule_toString_type(filetypeFilter.toString()));
            }

        }
        if (hosterURLFilter.isEnabled()) {
            if (link != null) {
                cond.add(_GUI._.FilterRule_toString_hoster2(link.getURL(), hosterURLFilter.toString()));
            } else {
                cond.add(_GUI._.FilterRule_toString_hoster(hosterURLFilter.toString()));
            }

        }
        if (sourceURLFilter.isEnabled()) {

            cond.add(_GUI._.FilterRule_toString_source(sourceURLFilter.toString()));

        }

        for (int i = 0; i < cond.size(); i++) {
            if (i > 0) {
                if (i < cond.size() - 1) {
                    sb.append(_GUI._.FilterRule_toString_comma(cond.get(i)));
                } else {
                    sb.append(_GUI._.FilterRule_toString_and(cond.get(i)));
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
        return hosterURLFilter;
    }

    public void setHosterURLFilter(RegexFilter hoster) {
        this.hosterURLFilter = hoster;
    }

    public RegexFilter getSourceURLFilter() {
        return sourceURLFilter;
    }

    public void setSourceURLFilter(RegexFilter source) {
        this.sourceURLFilter = source;
    }

    public FiletypeFilter getFiletypeFilter() {
        return filetypeFilter;
    }

    public void setFiletypeFilter(FiletypeFilter type) {
        this.filetypeFilter = type;
    }

    public RegexFilter getFilenameFilter() {
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
