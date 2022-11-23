package jd.plugins.components.gopro;

import org.appwork.storage.SimpleTypeRef;
import org.appwork.storage.StorableValidatorIgnoresMissingSetter;
import org.appwork.storage.TypeRef;
import org.appwork.storage.flexijson.mapper.interfacestorage.FlexiStorableInterface;

@StorableValidatorIgnoresMissingSetter
public interface ReflectData extends FlexiStorableInterface {
    public static TypeRef<ReflectData> TYPEREF = new SimpleTypeRef<ReflectData>(ReflectData.class);

    public Media[] getCollectionMedia();

    public Collection getCollection();
}