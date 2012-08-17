package org.jdownloader.extensions.streaming.mediaarchive.prepare;

import java.io.IOException;

import jd.plugins.DownloadLink;

import org.appwork.utils.os.CrossSystem;
import org.jdownloader.extensions.streaming.StreamingExtension;
import org.jdownloader.extensions.streaming.mediaarchive.VideoMediaItem;
import org.jdownloader.extensions.streaming.mediaarchive.prepare.ffmpeg.FFMpegInfoReader;

public class VideoHandler extends ExtensionHandler<VideoMediaItem> {

    @Override
    public VideoMediaItem handle(StreamingExtension extension, DownloadLink dl) {
        if (CrossSystem.isWindows()) {
            try {

                FFMpegInfoReader info = new FFMpegInfoReader(dl);

                info.load(extension);
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
        return new VideoMediaItem(dl);
    }

}
