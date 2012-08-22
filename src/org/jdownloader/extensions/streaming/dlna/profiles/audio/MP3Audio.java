package org.jdownloader.extensions.streaming.dlna.profiles.audio;

import org.jdownloader.extensions.streaming.dlna.MimeType;
import org.jdownloader.extensions.streaming.dlna.profiles.container.AbstractMediaContainer;
import org.jdownloader.extensions.streaming.dlna.profiles.container.AudioMp4Container;
import org.jdownloader.extensions.streaming.dlna.profiles.rawstreams.RawAudioAMRStream;
import org.jdownloader.extensions.streaming.dlna.profiles.streams.audio.Mp3AudioStream;

public class MP3Audio extends AbstractAudioProfile {

    public static final MP3Audio MP3  = new MP3Audio("MP3") {
                                          {

                                              containers = new AbstractMediaContainer[] { AudioMp4Container.INSTANCE, RawAudioAMRStream.INSTANCE };
                                              stream = new Mp3AudioStream().addBitrateRange(32000, 320000).addSamplingRates(32000, 44100, 48000);

                                          }
                                      };
    public static final MP3Audio MP3X = new MP3Audio("MP3X") {
                                          {
                                              stream = new Mp3AudioStream().addBitrateRange(8000, 320000).addSamplingRates(16000, 22050, 24000, 32000, 44100, 48000);
                                              containers = new AbstractMediaContainer[] { AudioMp4Container.INSTANCE, RawAudioAMRStream.INSTANCE };

                                          }
                                      };

    public MP3Audio(String id) {
        super(id);
        mimeType = MimeType.AUDIO_MPEG;

    }

}
