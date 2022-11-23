package jd.plugins.components.gopro;

import org.appwork.storage.StorableValidatorIgnoresMissingSetter;
import org.appwork.storage.flexijson.mapper.interfacestorage.FlexiStorableInterface;

@StorableValidatorIgnoresMissingSetter
public interface Pages extends FlexiStorableInterface {
    public int getTotal_pages();
}