package jd.gui.swing.jdgui.views.settings.panels.anticaptcha;

import java.util.ArrayList;
import java.util.List;

import org.appwork.swing.components.tooltips.ExtTooltip;
import org.appwork.swing.components.tooltips.IconLabelToolTip;
import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.components.RegexListTextPane;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.AbstractIcon;

public class CaptchaRegexListTextPane extends RegexListTextPane {
    @Override
    protected boolean validateLine(String g2) {
        return g2 != null && g2.matches("[a-zA-z0-9_\\.\\-\\$\\(\\)\\[\\]\\{\\}\\^\\+\\*\\|]+") && super.validateLine(g2);
    }

    public ExtTooltip createFailTooltip(String p) {
        IconLabelToolTip ret = new IconLabelToolTip(_GUI._.CaptchaRegexListTextPane_createExtTooltip_bad(p), new AbstractIcon(IconKey.ICON_WARNING, 24));
        return ret;
    }

    public ExtTooltip createOkTooltip(String p) {
        IconLabelToolTip ret = new IconLabelToolTip(_GUI._.CaptchaRegexListTextPane_createExtTooltip_ok(p), new AbstractIcon(IconKey.ICON_OK, 24));
        return ret;
    }

    public List<String> getList() {
        List<String> ret = new ArrayList<String>();
        for (String s : Regex.getLines(getText())) {
            if (StringUtils.isNotEmpty(s)) {
                ret.add(s.trim());
            }
        }
        return ret;

    }

    public void setList(List<String> list) {
        StringBuilder sb = new StringBuilder();
        if (list != null) {
            for (String s : list) {
                if (sb.length() > 0) {
                    sb.append("\r\n");
                }
                sb.append(s);
            }
        }
        setText(sb.toString());
    }
}
