package jd.plugins;

import java.util.ArrayList;
import java.util.Collection;

import javax.swing.Icon;

import jd.plugins.FilePackageView.PluginState;

import org.appwork.utils.StringUtils;
import org.jdownloader.gui.views.components.MergedIcon;

public class PluginStateCollection extends ArrayList<PluginState> {

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

    public PluginStateCollection(Collection<PluginState> values) {
        super(values);

        ArrayList<Icon> icons = new ArrayList<Icon>();
        StringBuilder tt = new StringBuilder();
        multiline = false;
        for (PluginState state : this) {
            if (state.getIcon() != null) {
                icons.add(state.getIcon());
            }
            if (StringUtils.isNotEmpty(state.getDescription())) {
                if (tt.length() > 0) {
                    tt.append("\r\n");
                    multiline = true;
                }
                tt.append(state.getDescription());
            }

        }
        text = tt.toString();
        if (icons.size() > 0) {
            mergedIcon = new MergedIcon(icons.toArray(new Icon[] {}));
        }

    }

}
