package jd.plugins.components.gopro;

import java.util.Date;

import org.appwork.storage.SimpleTypeRef;
import org.appwork.storage.StorableValidatorIgnoresMissingSetter;
import org.appwork.storage.TypeRef;
import org.appwork.storage.flexijson.mapper.interfacestorage.FlexiStorableInterface;

@StorableValidatorIgnoresMissingSetter
public interface Media extends FlexiStorableInterface {
    public static TypeRef<Media> TYPEREF = new SimpleTypeRef<Media>(Media.class);

    public Date getCaptured_at();

    public String getFilename();

    public String getFile_extension();

    public String getType();

    public int getItem_count();

    public String getId();

    public long getFile_size();

    public long getHeight();

    public long getWidth();
}