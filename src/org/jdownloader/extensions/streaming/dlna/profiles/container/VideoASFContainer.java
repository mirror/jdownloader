package org.jdownloader.extensions.streaming.dlna.profiles.container;

import org.jdownloader.extensions.streaming.dlna.Extensions;
import org.jdownloader.extensions.streaming.dlna.MimeType;

public class VideoASFContainer extends AbstractAudioVideoContainer {

    public VideoASFContainer() {
        super(Extensions.AUDIO_VIDEO_ASF, Extensions.AUDIO_VIDEO_WMV);
        /*
         * Example: "format": { "filename": "http://192.168.2.122:3128/stream/ffmpeg/1346222721900", "nb_streams": 2, "format_name": "asf",
         * "format_long_name": "ASF (Advanced / Active Streaming Format)", "start_time": "0.000000", "duration": "246.976000", "size":
         * "655360", "bit_rate": "21228" }
         * 
         * 
         * "nb_streams": 2, "format_name": "asf", "format_long_name": "ASF (Advanced / Active Streaming Format)", "start_time": "0.000000",
         * "duration": "35.101000", "size": "1563624", "bit_rate": "356371",
         */
        setFormatName("asf");
        setName(MimeType.VIDEO_ASF.getLabel());

    }

}
