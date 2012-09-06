package org.jdownloader.extensions.streaming.dataprovider;

import java.io.IOException;

import org.jdownloader.extensions.streaming.StreamingExtension;
import org.jdownloader.extensions.streaming.dlna.profiles.Profile;
import org.jdownloader.extensions.streaming.mediaarchive.MediaItem;
import org.jdownloader.extensions.streaming.upnp.remotedevices.RendererInfo;

public class TranscodeStreamManager extends AbstractStreamManager<StreamFactoryInterface> {

    public TranscodeStreamManager(StreamingExtension extension) {
        super(extension);
    }

    public StreamFactoryInterface getStreamFactory(StreamFactoryInterface streamfactory, MediaItem mediaItem, RendererInfo callingDevice, Profile dlnaProfile) throws IOException {

        String cacheKey = streamfactory.hashCode() + "_" + mediaItem.getUniqueID() + "_" + dlnaProfile.getProfileID();
        StreamFactoryInterface streaming = map.get(cacheKey);

        if (streaming == null || streaming.isClosed()) {
            streaming = new TranscodeStreamFactory(streamfactory, mediaItem, callingDevice, dlnaProfile);
            ((TranscodeStreamFactory) streaming).setTransportPort(getExtension().getSettings().getStreamServerPort());
            map.put(cacheKey, streaming);
        }

        return streaming;
    }

}
