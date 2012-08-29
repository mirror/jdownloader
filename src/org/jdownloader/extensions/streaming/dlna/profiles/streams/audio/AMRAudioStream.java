package org.jdownloader.extensions.streaming.dlna.profiles.streams.audio;

public class AMRAudioStream extends InternalAudioStream {

    public AMRAudioStream(String contentType) {
        super(contentType);

        // amrnb AMR-NB (Adaptive Multi-Rate NarrowBand)
        setCodecNames("amrnb");
        setCodecTags("samr");

    }

}
