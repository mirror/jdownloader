package jd.plugins.components.gopro;

import org.appwork.storage.StorableValidatorIgnoresMissingSetter;
import org.appwork.storage.flexijson.mapper.interfacestorage.FlexiStorableInterface;

@StorableValidatorIgnoresMissingSetter
public interface Collection extends FlexiStorableInterface {
    public String getTitle();
}