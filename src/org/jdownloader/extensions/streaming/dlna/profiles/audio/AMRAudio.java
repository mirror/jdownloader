package org.jdownloader.extensions.streaming.dlna.profiles.audio;

import org.jdownloader.extensions.streaming.dlna.MimeType;
import org.jdownloader.extensions.streaming.dlna.profiles.container.AbstractMediaContainer;
import org.jdownloader.extensions.streaming.dlna.profiles.container.AudioGp3Container;
import org.jdownloader.extensions.streaming.dlna.profiles.container.AudioMp4Container;
import org.jdownloader.extensions.streaming.dlna.profiles.rawstreams.RawAudioAMRStream;
import org.jdownloader.extensions.streaming.dlna.profiles.streams.audio.AMRAudioStream;

public class AMRAudio extends AbstractAudioProfile {

    public static final AMRAudio AMR_3GPP   = new AMRAudio("AMR_3GPP") {
                                                {
                                                    mimeType = MimeType.AUDIO_MPEG_4;
                                                    containers = new AbstractMediaContainer[] { AudioMp4Container.INSTANCE, RawAudioAMRStream.INSTANCE };

                                                }
                                            };

    public static final AMRAudio AMR_3GPP_2 = new AMRAudio("AMR_3GPP") {
                                                {
                                                    mimeType = MimeType.AUDIO_3GP;

                                                    containers = new AbstractMediaContainer[] { AudioGp3Container.INSTANCE };

                                                }
                                            };
    public static final AMRAudio AMR_WBplus = new AMRAudio("AMR_WBplus") {
                                                {
                                                    profileTags = new String[] { "basic", "progressive-download" };
                                                    mimeType = MimeType.AUDIO_3GP;
                                                    containers = new AbstractMediaContainer[] { AudioGp3Container.INSTANCE };

                                                }
                                            };

    public AMRAudio(String id) {
        super(id);
        stream = new AMRAudioStream("audio/AMR").addChannelRange(1, 1).addBitrateRange(64000, 640000).addSamplingRates(32000, 44100, 48000);

        // according to libdlna bitrates from 32000 to 640000 are supported. gupnp: 64000+

    }

}
