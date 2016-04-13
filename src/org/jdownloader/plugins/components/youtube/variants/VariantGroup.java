package org.jdownloader.plugins.components.youtube.variants;

import org.appwork.storage.config.annotations.LabelInterface;
import org.jdownloader.gui.translate._GUI;

public enum VariantGroup implements LabelInterface {
    AUDIO {

        @Override
        public String getLabel() {
            return _GUI.T.YoutubeBasicVariant_getLabel_audio();
        }
    },
    VIDEO {
        @Override
        public String getLabel() {
            return _GUI.T.YoutubeBasicVariant_getLabel_video();
        }
    },
    VIDEO_3D {
        @Override
        public String getLabel() {
            return _GUI.T.YoutubeBasicVariant_getLabel_video3d();
        }
    },
    IMAGE {
        @Override
        public String getLabel() {
            return _GUI.T.YoutubeBasicVariant_getLabel_image();
        }
    },
    SUBTITLES {
        @Override
        public String getLabel() {
            return _GUI.T.YoutubeBasicVariant_getLabel_subtitles();
        }
    },
    DESCRIPTION {
        @Override
        public String getLabel() {
            return _GUI.T.YoutubeBasicVariant_getLabel_description();
        }
    };
}