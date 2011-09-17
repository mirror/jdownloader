package org.jdownloader.controlling.filter;

import org.appwork.storage.Storable;

public class FilterRule implements Storable {

    private FilesizeFilter filesizeFilter;
    private RegexFilter    hosterURLFilter;
    private RegexFilter    sourceURLFilter;

    public FilesizeFilter getFilesizeFilter() {
        return filesizeFilter;
    }

    public void setFilesizeFilter(FilesizeFilter size) {
        this.filesizeFilter = size;
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

    public FilterRule() {
        // required by Storable
    }

    private boolean enabled;

    private String  name;
    private boolean accept;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void setAccept(boolean b) {
        accept = b;
    }

    public boolean isAccept() {
        return accept;
    }

}
