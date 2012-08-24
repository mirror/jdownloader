package org.jdownloader.extensions.streaming.mediaarchive.prepare;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import jd.plugins.DownloadLink;

import org.appwork.utils.Application;
import org.appwork.utils.Files;
import org.appwork.utils.os.CrossSystem;
import org.jdownloader.extensions.streaming.StreamingExtension;
import org.jdownloader.extensions.streaming.mediaarchive.VideoMediaItem;
import org.jdownloader.extensions.streaming.mediaarchive.prepare.ffmpeg.FFMpegInfoReader;
import org.jdownloader.extensions.streaming.mediaarchive.prepare.ffmpeg.Stream;

public class VideoHandler extends ExtensionHandler<VideoMediaItem> {

    @Override
    public VideoMediaItem handle(StreamingExtension extension, DownloadLink dl) {
        VideoMediaItem ret = new VideoMediaItem(dl);
        if (CrossSystem.isWindows()) {
            try {

                FFMpegInfoReader ffmpeg = new FFMpegInfoReader(dl);

                ffmpeg.load(extension);

                boolean hasVideoStream = false;
                ArrayList<Stream> streams = ffmpeg.getStreams();
                if (streams != null) {
                    for (Stream info : streams) {

                        if ("video".equals(info.getCodec_type()) && !"mjpeg".equalsIgnoreCase(info.getCodec_name())) {
                            hasVideoStream = true;
                            VideoStream as = new VideoStream();
                            as.setCodec(info.getCodec_type());
                            as.setBitrate(info.parseBitrate());

                            as.setDuration(info.parseDuration());

                            as.setIndex(info.getIndex());
                            as.setWidth(info.getWidth());
                            as.setIndex(info.getIndex());
                            as.setHeight(info.getHeight());
                            ret.addVideoStream(as);
                        } else if ("audio".equals(info.getCodec_type())) {
                            AudioStream as = new AudioStream();
                            as.setCodec(info.getCodec_type());
                            as.setBitrate(info.parseBitrate());

                            as.setDuration(info.parseDuration());
                            as.setIndex(info.getIndex());
                            ret.addAudioStream(as);
                        }

                    }
                    if (!hasVideoStream) {
                        // not a video
                        return null;
                    }

                    ret.setThumbnailPath(Files.getRelativePath(Application.getResource("tmp").getParentFile(), new File(ffmpeg.getThumbnailPath())));
                    ret.setBitrate(ffmpeg.getFormat().parseBitrate());
                    ret.setDuration(ffmpeg.getFormat().parseDuration());
                    ret.setContainerFormat(ffmpeg.getFormat().getFormat_name());
                    ret.setSize(ffmpeg.getFormat().parseSize());

                }

            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
        return ret;
    }
}
