package org.jdownloader.extensions.streaming.dlna.profiles.audio;

import org.jdownloader.extensions.streaming.dlna.MimeType;
import org.jdownloader.extensions.streaming.dlna.profiles.container.AbstractMediaContainer;
import org.jdownloader.extensions.streaming.dlna.profiles.container.AudioWmaAsfContainer;
import org.jdownloader.extensions.streaming.dlna.profiles.rawstreams.RawAudioWmaStream;
import org.jdownloader.extensions.streaming.dlna.profiles.streams.audio.WmaAudioStream;

public class WMAAudio extends AbstractAudioProfile {

    public static final WMAAudio WMABASE = new WMAAudio("WMABASE") {

                                             {
                                                 stream = new WmaAudioStream().setWmaVersions(1, 2).addChannelRange(1, 2).addBitrateRange(1, 192999).addSamplingRateRange(8, 48000);

                                                 containers = new AbstractMediaContainer[] { RawAudioWmaStream.INSTANCE, AudioWmaAsfContainer.INSTANCE };

                                             }
                                         };
    public static final WMAAudio WMAFULL = new WMAAudio("WMAFULL") {

                                             {

                                                 stream = new WmaAudioStream().setWmaVersions(1, 2).addChannelRange(1, 2).addBitrateRange(1, 385000).addSamplingRateRange(8, 48000);

                                                 containers = new AbstractMediaContainer[] { RawAudioWmaStream.INSTANCE, AudioWmaAsfContainer.INSTANCE };

                                             }
                                         };

    public static final WMAAudio WMAPRO  = new WMAAudio("WMAPRO") {

                                             {

                                                 stream = new WmaAudioStream().setWmaVersions(3).addChannelRange(1, 8).addBitrateRange(1, 1500000).addSamplingRateRange(8, 96000);

                                                 containers = new AbstractMediaContainer[] { RawAudioWmaStream.INSTANCE, AudioWmaAsfContainer.INSTANCE };

                                             }
                                         };

    public WMAAudio(String id) {
        super(id);
        mimeType = MimeType.AUDIO_WMA;

    }

    public static void init() {
    }

}
