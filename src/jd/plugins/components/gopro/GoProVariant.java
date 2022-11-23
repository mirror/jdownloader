package jd.plugins.components.gopro;

import javax.swing.Icon;

import org.appwork.storage.Storable;
import org.jdownloader.controlling.linkcrawler.LinkVariant;

public class GoProVariant implements LinkVariant, Storable {
    private String id;
    private String name;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public GoProVariant() {
    }

    public GoProVariant(String name, String id) {
        this.id = id;
        this.name = name;
    }

    @Override
    public String _getUniqueId() {
        return id;
    }

    @Override
    public String _getName(Object caller) {
        return name;
    }

    @Override
    public Icon _getIcon(Object caller) {
        return null;
    }

    @Override
    public String _getTooltipDescription(Object caller) {
        return null;
    }
}