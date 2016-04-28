package org.jdownloader.plugins.components.youtube.variants;

import javax.swing.Icon;

import org.appwork.storage.config.annotations.LabelInterface;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.AbstractIcon;
import org.jdownloader.images.BadgeIcon;

public enum VariantGroup implements LabelInterface {
    AUDIO(IconKey.ICON_AUDIO) {

        @Override
        public String getLabel() {
            return _GUI.T.YoutubeBasicVariant_getLabel_audio();
        }
    },
    VIDEO(IconKey.ICON_VIDEO) {
        @Override
        public String getLabel() {
            return _GUI.T.YoutubeBasicVariant_getLabel_video();
        }
    },
    // VIDEO_3D {
    // @Override
    // public String getLabel() {
    // return _GUI.T.YoutubeBasicVariant_getLabel_video3d();
    // }
    // },
    // VIDEO_360 {
    // @Override
    // public String getLabel() {
    // return _GUI.T.YoutubeBasicVariant_getLabel_video360();
    // }
    // },
    // VIDEO_3D_360 {
    // @Override
    // public String getLabel() {
    // return _GUI.T.YoutubeBasicVariant_getLabel_video360_3D();
    // }
    // },
    IMAGE(IconKey.ICON_IMAGE) {
        @Override
        public String getLabel() {
            return _GUI.T.YoutubeBasicVariant_getLabel_image();
        }
    },
    SUBTITLES(IconKey.ICON_LANGUAGE) {
        @Override
        public Icon getIcon(int size) {
            return new BadgeIcon(IconKey.ICON_TEXT, IconKey.ICON_LANGUAGE, size);

        }

        @Override
        public String getLabel() {
            return _GUI.T.YoutubeBasicVariant_getLabel_subtitles();
        }
    },
    DESCRIPTION(IconKey.ICON_TEXT) {

        @Override
        public String getLabel() {
            return _GUI.T.YoutubeBasicVariant_getLabel_description();
        }
    };
    private String iconKey;

    private VariantGroup(String iconKey) {
        this.iconKey = iconKey;
    }

    public Icon getIcon(int size) {
        return new AbstractIcon(iconKey, size);
    }

}