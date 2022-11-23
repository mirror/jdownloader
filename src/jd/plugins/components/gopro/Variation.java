package jd.plugins.components.gopro;

import org.appwork.storage.StorableValidatorIgnoresMissingSetter;
import org.appwork.storage.flexijson.mapper.interfacestorage.FlexiStorableInterface;

@StorableValidatorIgnoresMissingSetter
public interface Variation extends FlexiStorableInterface {
    public String getUrl();

    public int getHeight();

    public String getHead();

    public String getLabel();

    public int getItem_number();
}