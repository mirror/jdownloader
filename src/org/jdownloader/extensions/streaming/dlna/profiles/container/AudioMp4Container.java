package org.jdownloader.extensions.streaming.dlna.profiles.container;

import org.jdownloader.extensions.streaming.dlna.Extensions;

/*
 *     "filename": "http://192.168.2.122:3128/stream/ffmpeg/1346240942619",
 "nb_streams": 1,
 "format_name": "mov,mp4,m4a,3gp,3g2,mj2",
 "format_long_name": "QuickTime / MOV",
 "start_time": "0.000000",
 "duration": "7.848333",
 "size": "32990",
 "bit_rate": "33627",
 "tags": {
 "major_brand": "mp42",
 "minor_version": "0",
 "compatible_brands": "mp42isom3gp63g2a",
 "creation_time": "2007-07-18 09:45:06",
 "title": "The 5th file",
 "artist": "Coding Technologies",
 "album": "aacPlus Decoder Check",
 "date": "2007"
 }
 */
public class AudioMp4Container extends AbstractAudioContainer {
    public static final AudioMp4Container INSTANCE = new AudioMp4Container();

    protected AudioMp4Container() {
        super(Extensions.AUDIO_VIDEO_MP4, Extensions.AUDIO_M4A);
        setName("video/quicktime");

    }
}
