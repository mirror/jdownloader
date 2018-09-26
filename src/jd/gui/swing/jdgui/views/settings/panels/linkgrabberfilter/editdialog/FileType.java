package jd.gui.swing.jdgui.views.settings.panels.linkgrabberfilter.editdialog;

import javax.swing.Icon;

import org.jdownloader.controlling.filter.CompiledFiletypeFilter.ArchiveExtensions;
import org.jdownloader.controlling.filter.CompiledFiletypeFilter.AudioExtensions;
import org.jdownloader.controlling.filter.CompiledFiletypeFilter.DocumentExtensions;
import org.jdownloader.controlling.filter.CompiledFiletypeFilter.ExtensionsFilterInterface;
import org.jdownloader.controlling.filter.CompiledFiletypeFilter.HashExtensions;
import org.jdownloader.controlling.filter.CompiledFiletypeFilter.ImageExtensions;
import org.jdownloader.controlling.filter.CompiledFiletypeFilter.VideoExtensions;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.AbstractIcon;
import org.jdownloader.images.NewTheme;

public enum FileType {
    HASH(HashExtensions.MD5),
    AUDIO(AudioExtensions.AAC),
    VIDEO(VideoExtensions.ASF),
    ARCHIVE(ArchiveExtensions.ACE),
    IMAGE(ImageExtensions.BMP),
    TXT(DocumentExtensions.TXT),
    CUSTOM(null) {
        @Override
        public Icon getIcon() {
            return new AbstractIcon(IconKey.ICON_HELP, 18);
        }

        @Override
        public String getLabel() {
            return _GUI.T.ConditionDialog_getLabel_customtype_();
        }
    };
    ;
    private ExtensionsFilterInterface filter;

    private FileType(ExtensionsFilterInterface aa) {
        filter = aa;
    }

    public String getLabel() {
        return filter.getDesc();
    }

    public Icon getIcon() {
        return NewTheme.I().getIcon(filter.getIconID(), 18);
    }
}
