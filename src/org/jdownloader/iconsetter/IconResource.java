package org.jdownloader.iconsetter;

import java.io.File;
import java.util.HashSet;

public class IconResource {

    private String path;
    private String standardMd5;

    public String getPath() {
        return path;
    }

    public IconResource(String rel, String md5) {
        this.path = rel;
        this.standardMd5 = md5;
    }

    public String getStandardMd5() {
        return standardMd5;
    }

    private HashSet<String> sets = new HashSet<String>();

    public void addSet(String name) {
        sets.add(name);
    }

    public String getTags() {
        String name = new File(path).getName();
        name = name.replaceAll("\\..+$", "");
        name = name.replaceAll("[^\\w]+", " ");
        return name;
    }

}
