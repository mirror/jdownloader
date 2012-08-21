package org.jdownloader.extensions.streaming.dlna.profiles.audio;

import org.jdownloader.extensions.streaming.dlna.MimeType;
import org.jdownloader.extensions.streaming.dlna.profiles.container.AbstractMediaContainer;
import org.jdownloader.extensions.streaming.dlna.profiles.rawstreams.RawAudioLPCMStream;
import org.jdownloader.extensions.streaming.dlna.profiles.streams.audio.LPCMAudioStream;

/**
 * @author Thomas
 * 
 */
public class LPCMAudio extends AbstractAudioProfile {

    public static final LPCMAudio AUDIO_LPCM_L16_44100_1 = new LPCMAudio("LPCM") {
                                                             {
                                                                 mimeType = MimeType.AUDIO_LPCM_L16_44100_1;
                                                                 stream = new LPCMAudioStream(mimeType.getLabel()).addChannelRange(1, 1).addSamplingRates(44100);
                                                                 containers = new AbstractMediaContainer[] { RawAudioLPCMStream.INSTANCE };

                                                             }
                                                         };

    public static final LPCMAudio AUDIO_LPCM_L16_44100_2 = new LPCMAudio("LPCM") {
                                                             {
                                                                 mimeType = MimeType.AUDIO_LPCM_L16_44100_2;

                                                                 containers = new AbstractMediaContainer[] { RawAudioLPCMStream.INSTANCE };
                                                                 stream = new LPCMAudioStream(mimeType.getLabel()).addChannelRange(2, 2).addSamplingRates(44100);

                                                             }
                                                         };

    public static final LPCMAudio AUDIO_LPCM_L16_48000_1 = new LPCMAudio("LPCM") {
                                                             {
                                                                 mimeType = MimeType.AUDIO_LPCM_L16_48000_1;

                                                                 containers = new AbstractMediaContainer[] { RawAudioLPCMStream.INSTANCE };

                                                                 stream = new LPCMAudioStream(mimeType.getLabel()).addChannelRange(1, 1).addSamplingRates(48000);

                                                             }
                                                         };

    public static final LPCMAudio AUDIO_LPCM_L16_48000_2 = new LPCMAudio("LPCM") {
                                                             {
                                                                 mimeType = MimeType.AUDIO_LPCM_L16_48000_2;
                                                                 stream = new LPCMAudioStream(mimeType.getLabel()).addChannelRange(2, 2).addSamplingRates(48000);
                                                                 containers = new AbstractMediaContainer[] { RawAudioLPCMStream.INSTANCE };

                                                             }
                                                         };

    public LPCMAudio(String id) {
        super(id);

    }

}
