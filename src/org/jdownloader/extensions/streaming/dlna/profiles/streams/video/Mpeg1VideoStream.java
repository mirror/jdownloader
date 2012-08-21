package org.jdownloader.extensions.streaming.dlna.profiles.streams.video;


public class Mpeg1VideoStream extends MpegVideoStream {
    public Mpeg1VideoStream() {

        mpegVersions = new int[] { 1 };
        /**
         * According to gupnp; -->This isn't exactly as in the spec, but should catch more compliant streams
         */
        addBitrateRange(1150000, 1152000);
    }

}
