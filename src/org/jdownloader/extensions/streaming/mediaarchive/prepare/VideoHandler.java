package org.jdownloader.extensions.streaming.mediaarchive.prepare;

import java.io.File;
import java.util.ArrayList;

import jd.plugins.DownloadLink;

import org.appwork.utils.Application;
import org.appwork.utils.Files;
import org.jdownloader.extensions.streaming.StreamingExtension;
import org.jdownloader.extensions.streaming.mediaarchive.VideoMediaItem;
import org.jdownloader.extensions.streaming.mediaarchive.prepare.ffmpeg.FFMpegInfoReader;
import org.jdownloader.extensions.streaming.mediaarchive.prepare.ffmpeg.Stream;

public class VideoHandler extends ExtensionHandler<VideoMediaItem> {

    @Override
    public VideoMediaItem handle(StreamingExtension extension, DownloadLink dl) {
        VideoMediaItem ret = new VideoMediaItem(dl);
        FFMpegInfoReader ffmpeg = new FFMpegInfoReader(new UndefinedMediaItem(dl));

        try {

            ffmpeg.load(extension);

            boolean hasVideoStream = false;
            ArrayList<Stream> streams = ffmpeg.getStreams();
            if (streams != null) {
                for (Stream info : streams) {

                    if ("video".equals(info.getCodec_type()) && !"mjpeg".equalsIgnoreCase(info.getCodec_name())) {
                        hasVideoStream = true;

                        ret.addVideoStream(info.toVideoStream());
                    } else if ("audio".equals(info.getCodec_type())) {

                        ret.addAudioStream(info.toAudioStream());
                    }

                }
                if (!hasVideoStream) {
                    // not a video
                    return null;
                }
                ret.setInfoString(ffmpeg.getResult());
                if (ffmpeg.getThumbnailPath() != null) ret.setThumbnailPath(Files.getRelativePath(Application.getTemp().getParentFile(), new File(ffmpeg.getThumbnailPath())));
                ret.setSystemBitrate(ffmpeg.getFormat().parseBitrate());
                ret.setDuration(ffmpeg.getFormat().parseDuration());
                ret.setContainerFormat(ffmpeg.getFormat().getFormat_name());
                if (ffmpeg.getFormat().getTags() != null) ret.setMajorBrand(ffmpeg.getFormat().getTags().getMajor_brand());
                ret.setSize(ffmpeg.getFormat().parseSize());

            } else {
                // ffmpeg failed
                return null;
            }

        } catch (Exception e) {
            e.printStackTrace();

        }
        ret.setDownloadError(ffmpeg.getMediaItem().getDownloadError());
        return ret;
    }
}
