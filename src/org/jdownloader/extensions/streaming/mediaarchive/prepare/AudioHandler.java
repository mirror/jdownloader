package org.jdownloader.extensions.streaming.mediaarchive.prepare;

import java.io.File;
import java.util.ArrayList;

import jd.plugins.DownloadLink;

import org.appwork.utils.Application;
import org.appwork.utils.Files;
import org.appwork.utils.StringUtils;
import org.jdownloader.extensions.streaming.StreamingExtension;
import org.jdownloader.extensions.streaming.mediaarchive.AudioMediaItem;
import org.jdownloader.extensions.streaming.mediaarchive.prepare.ffmpeg.FFMpegInfoReader;
import org.jdownloader.extensions.streaming.mediaarchive.prepare.ffmpeg.Stream;

public class AudioHandler extends ExtensionHandler<AudioMediaItem> {

    @Override
    public AudioMediaItem handle(StreamingExtension extension, DownloadLink dl) {
        AudioMediaItem ret = new AudioMediaItem(dl);
        FFMpegInfoReader ffmpeg = new FFMpegInfoReader(new UndefinedMediaItem(dl));

        try {

            ffmpeg.load(extension);
            boolean hasVideoStream = false;
            ArrayList<Stream> streams = ffmpeg.getStreams();
            boolean isVideo = false;
            if (streams != null) {
                for (Stream info : streams) {
                    // mjpeg = mp3 covers
                    if ("video".equals(info.getCodec_type()) && !"mjpeg".equalsIgnoreCase(info.getCodec_name())) {

                        isVideo = true;

                        continue;
                    } else if ("audio".equals(info.getCodec_type())) {

                        ret.setStream(info.toAudioStream());
                        break;
                    }

                }
                if (ffmpeg.getThumbnailPath() != null) ret.setThumbnailPath(Files.getRelativePath(Application.getTemp().getParentFile(), new File(ffmpeg.getThumbnailPath())));
                ret.setContainerFormat(ffmpeg.getFormat().getFormat_name());
                ret.setSize(ffmpeg.getFormat().parseSize());

                ret.setTitle(dl.getName());
                ret.setAlbum("Unknown");
                ret.setArtist("Unknown");
                if (ffmpeg.getFormat().getTags() != null) {
                    if (StringUtils.isNotEmpty(ffmpeg.getFormat().getTags().getTitle())) ret.setTitle(ffmpeg.getFormat().getTags().getTitle());
                    if (StringUtils.isNotEmpty(ffmpeg.getFormat().getTags().getAlbum())) ret.setAlbum(ffmpeg.getFormat().getTags().getAlbum());
                    if (StringUtils.isNotEmpty(ffmpeg.getFormat().getTags().getArtist())) ret.setArtist(ffmpeg.getFormat().getTags().getArtist());
                }

            }

        } catch (Exception e) {

        }
        ret.setDownloadError(ffmpeg.getMediaItem().getDownloadError());
        return ret;
    }

}
