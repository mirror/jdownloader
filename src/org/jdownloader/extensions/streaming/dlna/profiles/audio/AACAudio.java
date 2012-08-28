package org.jdownloader.extensions.streaming.dlna.profiles.audio;

import org.jdownloader.extensions.streaming.dlna.MimeType;
import org.jdownloader.extensions.streaming.dlna.profiles.container.AbstractMediaContainer;
import org.jdownloader.extensions.streaming.dlna.profiles.container.AudioGp3Container;
import org.jdownloader.extensions.streaming.dlna.profiles.container.AudioMp4Container;
import org.jdownloader.extensions.streaming.dlna.profiles.container.AudioQuicktimeContainer;
import org.jdownloader.extensions.streaming.dlna.profiles.rawstreams.RawAudioAACStream;
import org.jdownloader.extensions.streaming.dlna.profiles.streams.audio.AACAudioStream;

/**
 * Supported AAC Audio Profiles. This list is not complete at all. Look at
 * http://libdlna.sourcearchive.com/documentation/0.2.3-0ubuntu7/audio__aac_8c_source.html
 * 
 * @author Thomas
 * 
 */
public class AACAudio extends AbstractAudioProfile {

    static class AAC_ADTSAudio extends AACAudio {
        public AAC_ADTSAudio(String id) {
            super(id);

            raw = true;
            // info from gupnp-dlna
            streamFormat = "adts";
            mimeType = MimeType.AUDIO_ADTS;
            stream = new AACAudioStream().addBitrateRange(0, 576000).addChannelRange(2, 2).addSamplingRates(8000, 11025, 12000, 16000, 22050, 24000, 32000, 44100, 48000);
            containers = new AbstractMediaContainer[] { RawAudioAACStream.INSTANCE };

        }
    }

    static class AAC_ISOAudio extends AACAudio {
        public AAC_ISOAudio(String id) {
            super(id);
            mimeType = MimeType.AUDIO_MPEG_4;
            stream = new AACAudioStream().addBitrateRange(0, 576000).addChannelRange(2, 2).addSamplingRates(8000, 11025, 12000, 16000, 22050, 24000, 32000, 44100, 48000);
            raw = false;
            // info from gupnp-dlna
            variant = "iso";
            containers = new AbstractMediaContainer[] { AudioMp4Container.INSTANCE, AudioGp3Container.INSTANCE, AudioQuicktimeContainer.INSTANCE };

        }

    }

    public static final AACAudio AAC_ADTS        = new AAC_ADTSAudio("AAC_ADTS");

    public static final AACAudio AAC_ADTS_320    = new AAC_ADTSAudio("AAC_ADTS_320") {
                                                     {
                                                         stream = new AACAudioStream().addBitrateRange(0, 320000).addChannelRange(1, 2).addSamplingRates(8000, 11025, 12000, 16000, 22050, 24000, 32000, 44100, 48000);

                                                     }
                                                 };

    public static final AACAudio AAC_ISO         = new AAC_ISOAudio("AAC_ISO");

    public static final AACAudio AAC_ISO_320     = new AAC_ISOAudio("AAC_ISO_320") {
                                                     {
                                                         stream = new AACAudioStream().addBitrateRange(0, 320000).addChannelRange(1, 2).addSamplingRates(8000, 11025, 12000, 16000, 22050, 24000, 32000, 44100, 48000);

                                                     }
                                                 };

    public static final AACAudio AAC_MULT5_ADTS  = new AAC_ADTSAudio("AAC_MULT5_ADTS") {
                                                     {
                                                         stream = new AACAudioStream().addBitrateRange(0, 1440000).addChannelRange(3, 6).addSamplingRates(8000, 11025, 12000, 16000, 22050, 24000, 32000, 44100, 48000);

                                                     }
                                                 };

    public static final AACAudio AAC_MULT5_ISO   = new AAC_ISOAudio("AAC_MULT5_ISO") {
                                                     {
                                                         stream = new AACAudioStream().addBitrateRange(0, 1440000).addChannelRange(3, 6).addSamplingRates(8000, 11025, 12000, 16000, 22050, 24000, 32000, 44100, 48000);

                                                     }
                                                 };

    public static final AACAudio AAC_MULT5_ISO_2 = new AAC_ISOAudio("AAC_MULT5_ISO") {
                                                     {

                                                         stream = new AACAudioStream().addBitrateRange(0, 1440000).addChannelRange(3, 6).addSamplingRates(8000, 11025, 12000, 16000, 22050, 24000, 32000, 44100, 48000);

                                                         mimeType = MimeType.AUDIO_3GP;
                                                         containers = new AbstractMediaContainer[] { AudioGp3Container.INSTANCE };

                                                     }
                                                 };

    protected int[]              mpegVersions;

    protected boolean            raw;
    protected String             streamFormat;

    protected String             variant;

    protected String[]           extensions;

    public AACAudio(String id) {
        super(id);

    }

    public int[] getMpegVersions() {
        return mpegVersions;
    }

    public String getStreamFormat() {
        return streamFormat;
    }

    public String getVariant() {
        return variant;
    }

    public boolean isRaw() {
        return raw;
    }

    public static void init() {
    }

}
