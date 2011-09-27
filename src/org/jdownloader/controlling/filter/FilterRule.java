package org.jdownloader.controlling.filter;

import java.util.ArrayList;

import org.appwork.storage.Storable;
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

    public String toString() {
        StringBuilder sb = new StringBuilder();
        ArrayList<String> cond = new ArrayList<String>();
        if (filenameFilter.isEnabled()) {
            cond.add(_GUI._.FilterRule_toString_name(filenameFilter.toString()));
        }
        if (filesizeFilter.isEnabled()) {

            cond.add(_GUI._.FilterRule_toString_size(filesizeFilter.toString()));
        }
        if (filetypeFilter.isEnabled()) {

            cond.add(_GUI._.FilterRule_toString_type(filetypeFilter.toString()));
        }
        if (hosterURLFilter.isEnabled()) {

            cond.add(_GUI._.FilterRule_toString_hoster(hosterURLFilter.toString()));
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
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
