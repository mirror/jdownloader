package org.jdownloader.plugins.components.youtube.variants;

import org.jdownloader.plugins.components.youtube.itag.AudioBitrate;
import org.jdownloader.plugins.components.youtube.itag.AudioCodec;

public interface AudioInterface {

    AudioBitrate getAudioBitrate();

    AudioCodec getAudioCodec();

}
