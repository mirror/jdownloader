package jd.plugins.components.gopro;

import org.appwork.storage.SimpleTypeRef;
import org.appwork.storage.StorableValidatorIgnoresMissingSetter;
import org.appwork.storage.TypeRef;
import org.appwork.storage.flexijson.mapper.FlexiJsonProperty;
import org.appwork.storage.flexijson.mapper.interfacestorage.FlexiStorableInterface;

@StorableValidatorIgnoresMissingSetter
public interface Download extends FlexiStorableInterface {
    public static TypeRef<Download> TYPEREF = new SimpleTypeRef<Download>(Download.class);

    public String getFilename();

    @FlexiJsonProperty("_embedded")
    public Embedded getEmbedded();
}