package jd.plugins.components.gopro;

import org.appwork.storage.SimpleTypeRef;
import org.appwork.storage.StorableValidatorIgnoresMissingSetter;
import org.appwork.storage.TypeRef;
import org.appwork.storage.flexijson.mapper.FlexiJsonProperty;
import org.appwork.storage.flexijson.mapper.interfacestorage.FlexiStorableInterface;

@StorableValidatorIgnoresMissingSetter
public interface SearchResponse extends FlexiStorableInterface {
    public static TypeRef<SearchResponse> TYPEREF = new SimpleTypeRef<SearchResponse>(SearchResponse.class);

    @FlexiJsonProperty("_pages")
    public Pages getPages();

    @FlexiJsonProperty("_embedded")
    public Embedded getEmbedded();

    public String getError();
}