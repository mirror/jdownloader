package jd.plugins;

import java.util.ArrayList;
import java.util.Collection;

import javax.swing.Icon;

import jd.plugins.FilePackageView.PluginState;

import org.appwork.utils.StringUtils;
import org.jdownloader.gui.views.components.MergedIcon;

public class PluginStateCollection extends ArrayList<PluginState<?>> {
    private boolean    multiline;
    private MergedIcon mergedIcon;

    public boolean isMultiline() {
        return multiline;
    }

    public MergedIcon getMergedIcon() {
        return mergedIcon;
    }

    public String getText() {
        return text;
    }

    private String text;

    public PluginStateCollection(Collection<PluginState<?>> values) {
        super(values);
        ArrayList<Icon> icons = new ArrayList<Icon>();
        StringBuilder tt = new StringBuilder();
        multiline = false;
        for (final PluginState<?> state : this) {
            final Icon icon = state.getIcon();
            if (icon != null) {
                icons.add(icon);
            }
            final String description = state.getDescription();
            if (StringUtils.isNotEmpty(description)) {
                if (tt.length() > 0) {
                    tt.append("\r\n");
                    multiline = true;
                }
                tt.append(description);
            }
        }
        text = tt.toString();
        if (icons.size() > 0) {
            mergedIcon = new MergedIcon(icons.toArray(new Icon[] {}));
        }
    }
}
