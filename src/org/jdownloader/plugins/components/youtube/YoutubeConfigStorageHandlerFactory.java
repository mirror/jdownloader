package org.jdownloader.plugins.components.youtube;

import java.io.File;
import java.net.URISyntaxException;
import java.util.ArrayList;

import org.appwork.exceptions.WTFException;
import org.appwork.storage.config.StorageHandlerFactory;
import org.appwork.storage.config.handler.DefaultFactoryInterface;
import org.appwork.storage.config.handler.KeyHandler;
import org.appwork.storage.config.handler.StorageHandler;
import org.jdownloader.plugins.components.youtube.configpanel.YoutubeVariantCollection;
import org.jdownloader.plugins.components.youtube.itag.AudioCodec;
import org.jdownloader.settings.staticreferences.CFG_YOUTUBE;

public class YoutubeConfigStorageHandlerFactory implements StorageHandlerFactory<YoutubeConfig>, DefaultFactoryInterface {
    @Override
    public StorageHandler<YoutubeConfig> create(File resource, Class<YoutubeConfig> configInterface) {
        StorageHandler ret = new StorageHandler<YoutubeConfig>(resource, configInterface);
        ret.setDefaultFactory(this);
        return ret;
    }

    @Override
    public StorageHandler<YoutubeConfig> create(String urlPath, Class<YoutubeConfig> configInterface) {
        StorageHandler ret;
        try {
            ret = new StorageHandler<YoutubeConfig>(urlPath, configInterface);
        } catch (URISyntaxException e) {
            throw new WTFException(e);
        }
        ret.setDefaultFactory(this);
        return ret;
    }

    @Override
    public Object getDefaultValue(KeyHandler<?> handler, Object o) {
        if (handler == CFG_YOUTUBE.BLACKLISTED_AUDIO_CODECS) {
            ArrayList<AudioCodec> ret = new ArrayList<AudioCodec>();
            ret.add(AudioCodec.AAC_SPATIAL);
            ret.add(AudioCodec.VORBIS_SPATIAL);
            ret.add(AudioCodec.OPUS_SPATIAL);
            return ret;
        }
        if (handler == CFG_YOUTUBE.COLLECTIONS) {
            return YoutubeVariantCollection.getDefaults();
        }
        return o;
    }
}
