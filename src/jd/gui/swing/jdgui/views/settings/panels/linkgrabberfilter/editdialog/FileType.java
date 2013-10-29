package jd.gui.swing.jdgui.views.settings.panels.linkgrabberfilter.editdialog;

import javax.swing.Icon;

import org.jdownloader.controlling.filter.CompiledFiletypeFilter.ArchiveExtensions;
import org.jdownloader.controlling.filter.CompiledFiletypeFilter.AudioExtensions;
import org.jdownloader.controlling.filter.CompiledFiletypeFilter.ExtensionsFilterInterface;
import org.jdownloader.controlling.filter.CompiledFiletypeFilter.ImageExtensions;
import org.jdownloader.controlling.filter.CompiledFiletypeFilter.VideoExtensions;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;

public enum FileType {
    AUDIO(AudioExtensions.AA),
    VIDEO(VideoExtensions.ASF),
    ARCHIVE(ArchiveExtensions.ACE),
    IMAGE(ImageExtensions.BMP),
    CUSTOM(null) {
        @Override
        public Icon getIcon() {
            return NewTheme.I().getIcon("help", 18);
        }

        @Override
        public String getLabel() {
            return _GUI._.ConditionDialog_getLabel_customtype_();
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
