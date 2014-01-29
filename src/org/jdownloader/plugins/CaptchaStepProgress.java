package org.jdownloader.plugins;

import java.awt.Color;

import javax.swing.Icon;

import jd.plugins.PluginProgress;

import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.views.downloads.columns.ETAColumn;
import org.jdownloader.images.NewTheme;
import org.jdownloader.translate._JDT;

public final class CaptchaStepProgress extends PluginProgress {
    public CaptchaStepProgress(long current, long total, Color color) {
        super(current, total, color);
        setIcon(NewTheme.I().getIcon(IconKey.ICON_OCR, 16));
    }

    @Override
    public Icon getIcon(Object requestor) {
        if (requestor != null) {
            if (requestor instanceof ETAColumn) { return null; }
        }
        return super.getIcon(requestor);
    }

    @Override
    public String getMessage(Object requestor) {
        if (requestor != null) {
            if (requestor instanceof ETAColumn) { return null; }
        }
        return _JDT._.gui_downloadview_statustext_jac();
    }

    @Override
    public PluginTaskID getID() {
        return PluginTaskID.CAPTCHA;
    }
}