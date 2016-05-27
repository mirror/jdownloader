package org.jdownloader.plugins.components.youtube.configpanel;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.appwork.storage.Storable;
import org.appwork.utils.StringUtils;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.plugins.components.youtube.Projection;
import org.jdownloader.plugins.components.youtube.VariantIDStorable;
import org.jdownloader.plugins.components.youtube.variants.FileContainer;
import org.jdownloader.plugins.components.youtube.variants.VariantGroup;
import org.jdownloader.settings.staticreferences.CFG_YOUTUBE;

public class YoutubeVariantCollection implements Storable {

    public YoutubeVariantCollection(/* storable */) {
    }

    private boolean enabled;
    private String  groupingID;

    public String getGroupingID() {
        return groupingID;
    }

    public void setGroupingID(String groupingID) {
        this.groupingID = groupingID;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<VariantIDStorable> getVariants() {
        return variants;
    }

    public void setVariants(List<VariantIDStorable> variants) {
        this.variants = variants;
    }

    private String                  name;
    private List<VariantIDStorable> variants;
    private List<VariantIDStorable> dropdown;

    public List<VariantIDStorable> getDropdown() {
        return dropdown;
    }

    public void setDropdown(List<VariantIDStorable> dropdown) {
        this.dropdown = dropdown;
    }

    public YoutubeVariantCollection(String name, List<VariantIDStorable> variants) {
        enabled = true;
        this.name = name;
        this.variants = variants;
    }

    public YoutubeVariantCollection(String name, String id) {
        this.enabled = true;
        this.name = name;
        this.groupingID = id;
    }

    public static List<YoutubeVariantCollection> load() {
        List<YoutubeVariantCollection> links = CFG_YOUTUBE.CFG.getCollections();
        ;
        ArrayList<YoutubeVariantCollection> ret = new ArrayList<YoutubeVariantCollection>();

        HashSet<String> groupIds = new HashSet<String>();
        if (links != null) {
            for (YoutubeVariantCollection l : links) {
                if (l != null) {
                    ret.add(l);
                    if (StringUtils.isNotEmpty(l.getGroupingID())) {
                        groupIds.add(l.getGroupingID());
                    }
                }

            }
        }
        for (YoutubeVariantCollection g : getDefaults()) {
            if (groupIds.add(g.getGroupingID())) {
                ret.add(g);
            }
        }

        return ret;
    }

    public HashSet<String> createUniqueIDSet() {
        HashSet<String> ret = new HashSet<String>();
        if (variants != null) {
            for (VariantIDStorable v : variants) {
                ret.add(v.createUniqueID());
            }
        }
        return ret;
    }

    public HashSet<String> createUniqueIDSetForDropDownList() {
        HashSet<String> ret = new HashSet<String>();
        if (dropdown != null) {
            for (VariantIDStorable v : dropdown) {
                ret.add(v.createUniqueID());
            }
        }
        return ret;
    }

    public static List<YoutubeVariantCollection> getDefaults() {
        ArrayList<YoutubeVariantCollection> ret = new ArrayList<YoutubeVariantCollection>();

        for (VariantGroup g : VariantGroup.values()) {

            switch (g) {
            case VIDEO:
                for (Projection p : Projection.values()) {
                    String id = g.name() + "_" + p.name();

                    ret.add(0, new YoutubeVariantCollection(_GUI.T.Youtube_bestcollection(p, g), id));

                }
                break;
            default:
                String id = g.name();

                ret.add(0, new YoutubeVariantCollection(_GUI.T.Youtube_bestcollection_2(g), id));

            }
        }
        for (FileContainer fc : FileContainer.values()) {
            String id = fc.name();

            YoutubeVariantCollection l;
            ret.add(l = new YoutubeVariantCollection(_GUI.T.Youtube_bestcollection_2(fc), id));
            l.setEnabled(false);

        }

        return ret;
    }

}
