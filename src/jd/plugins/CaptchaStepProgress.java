package jd.plugins;

import java.awt.Color;

import org.jdownloader.gui.IconKey;
import org.jdownloader.images.NewTheme;
import org.jdownloader.translate._JDT;

public final class CaptchaStepProgress extends PluginProgress {
    public CaptchaStepProgress(long current, long total, Color color) {
        super(current, total, color);
        setIcon(NewTheme.I().getIcon(IconKey.ICON_OCR, 16));
    }

    @Override
    public String getMessage(Object requestor) {
        return _JDT._.gui_downloadview_statustext_jac();
    }
}