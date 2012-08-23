package org.jdownloader.extensions.streaming.dlna.profiles.audio;

import org.jdownloader.extensions.streaming.dlna.MimeType;
import org.jdownloader.extensions.streaming.dlna.profiles.container.AbstractMediaContainer;
import org.jdownloader.extensions.streaming.dlna.profiles.rawstreams.RawAudioAc3Stream;
import org.jdownloader.extensions.streaming.dlna.profiles.rawstreams.RawAudioEac3Stream;
import org.jdownloader.extensions.streaming.dlna.profiles.streams.audio.AC3AudioStream;

/**
 * Supported AAC Audio Profiles. This list is not complete at all. Look at
 * http://libdlna.sourcearchive.com/documentation/0.2.3/audio__ac3_8c-source.html
 * 
 * @author Thomas
 * 
 */
public class AC3Audio extends AbstractAudioProfile {

    public static final AC3Audio AC3  = new AC3Audio("AC3") {
                                          {
                                              containers = new AbstractMediaContainer[] { RawAudioAc3Stream.INSTANCE };

                                              // according to libdlna bitrates from 32000 to 640000 are supported. gupnp: 64000+
                                              stream = new AC3AudioStream("audio/x-ac3").addBitrateRange(64000, 640000).addChannelRange(1, 6).addSamplingRates(32000, 44100, 48000);

                                          }
                                      };

    public static final AC3Audio EAC3 = new AC3Audio("EAC3") {
                                          {
                                              containers = new AbstractMediaContainer[] { RawAudioEac3Stream.INSTANCE };
                                              stream = new AC3AudioStream("audio/x-ac3").addBitrateRange(0, 3024000).addChannelRange(1, 6).addSamplingRates(32000, 44100, 48000);

                                          }
                                      };

    public AC3Audio(String id) {
        super(id);
        mimeType = MimeType.AUDIO_DOLBY_DIGITAL;

    }

    public static void init() {
    }

}
