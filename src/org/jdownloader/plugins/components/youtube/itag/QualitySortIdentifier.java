package org.jdownloader.plugins.components.youtube.itag;

import org.appwork.storage.config.annotations.LabelInterface;
import org.appwork.utils.CompareUtils;
import org.jdownloader.plugins.components.youtube.variants.AbstractVariant;
import org.jdownloader.plugins.components.youtube.variants.AudioVariant;
import org.jdownloader.plugins.components.youtube.variants.FileContainer;
import org.jdownloader.translate._JDT;

public enum QualitySortIdentifier implements LabelInterface {
    RESOLUTION {
        @Override
        public String getLabel() {
            return _JDT.T.lit_resolution();
        }

        public int compare(AbstractVariant o1, AbstractVariant o2) {

            return CompareUtils.compare(VideoResolution.getSortId(o1), VideoResolution.getSortId(o2));
        }

    },
    VIDEO_CODEC {
        @Override
        public String getLabel() {
            return _JDT.T.lit_video_codec();
        }

        public int compare(AbstractVariant o1, AbstractVariant o2) {

            return CompareUtils.compare(VideoCodec.getSortId(o1), VideoCodec.getSortId(o2));
        }
    },
    VIDEO_FRAMERATE {
        @Override
        public String getLabel() {
            return _JDT.T.lit_video_framerate();
        }

        public int compare(AbstractVariant o1, AbstractVariant o2) {

            return CompareUtils.compare(VideoFrameRate.getSortId(o1), VideoFrameRate.getSortId(o2));
        }
    },

    AUDIO_BITRATE {
        @Override
        public String getLabel() {
            return _JDT.T.lit_audio_bitrate();
        }

        public int compare(AbstractVariant o1, AbstractVariant o2) {

            return CompareUtils.compare(AudioBitrate.getSortId(o1), AudioBitrate.getSortId(o2));
        }

    },
    AUDIO_CODEC {
        @Override
        public String getLabel() {
            return _JDT.T.lit_audio_codec();
        }

        public int compare(AbstractVariant o1, AbstractVariant o2) {

            return CompareUtils.compare(AudioCodec.getSortId(o1), AudioCodec.getSortId(o2));
        }

    },
    CONTAINER {
        @Override
        public String getLabel() {
            return _JDT.T.lit_file_container();
        }

        public int compare(AbstractVariant o1, AbstractVariant o2) {

            return CompareUtils.compare(FileContainer.getSortId(o1), FileContainer.getSortId(o2));
        }
    },
    SEGMENT_STREAM {
        @Override
        public String getLabel() {
            return _JDT.T.youtube_segment();
        }

        public int compare(AbstractVariant o1, AbstractVariant o2) {

            boolean b1 = o1.getVariantInfo() != null && o1.getVariantInfo().hasDefaultSegmentsStream();
            boolean b2 = o2.getVariantInfo() != null && o2.getVariantInfo().hasDefaultSegmentsStream();
            return CompareUtils.compare(b2, b1);
        }
    },
    DEMUX_AUDIO {
        @Override
        public String getLabel() {
            return _JDT.T.youtube_demux();
        }

        public int compare(AbstractVariant o1, AbstractVariant o2) {
            boolean b1 = o1 instanceof AudioVariant && o1.getBaseVariant().getiTagAudio() == null;
            boolean b2 = o2 instanceof AudioVariant && o2.getBaseVariant().getiTagAudio() == null;
            return CompareUtils.compare(b2, b1);
        }
    };

    abstract public int compare(AbstractVariant o1, AbstractVariant o2);

}
