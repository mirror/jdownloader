package jd.plugins.components.gopro;

import java.util.List;

import org.appwork.storage.StorableValidatorIgnoresMissingSetter;
import org.appwork.storage.flexijson.mapper.interfacestorage.FlexiStorableInterface;

@StorableValidatorIgnoresMissingSetter
public interface Embedded extends FlexiStorableInterface {
    public Media[] getMedia();

    public List<Variation> getFiles();

    public List<Variation> getSidecar_files();

    public List<Variation> getVariations();
}