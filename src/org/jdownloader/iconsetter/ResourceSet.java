package org.jdownloader.iconsetter;

import java.util.ArrayList;
import java.util.List;

public class ResourceSet {

    private String                  name;
    private ArrayList<IconResource> icons;

    public String getName() {
        return name;
    }

    public List<IconResource> getIcons() {
        return icons;
    }

    public ResourceSet(String name) {
        this.name = name;
        this.icons = new ArrayList<IconResource>();
    }

    public void add(IconResource ir) {
        icons.add(ir);

    }

}
