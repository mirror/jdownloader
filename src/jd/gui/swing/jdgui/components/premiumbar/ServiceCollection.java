package jd.gui.swing.jdgui.components.premiumbar;

import java.util.ArrayList;

import javax.swing.Icon;
import javax.swing.JComponent;

import org.appwork.swing.components.tooltips.ExtTooltip;

public abstract class ServiceCollection<T> extends ArrayList<T> implements Comparable<ServiceCollection<?>> {

    /**
     * 
     */
    private static final long serialVersionUID = 8265367370670318032L;

    public abstract Icon getIcon();

    public abstract boolean isEnabled();

    @Override
    public int compareTo(ServiceCollection<?> o) {
        int ret = new Boolean(o.isEnabled()).compareTo(new Boolean(isEnabled()));
        if (ret == 0) {
            if (o.isEnabled()) {
                // sort on name
                ret = getName().compareTo(o.getName());
            } else {

                // last enabled one should be the first
                ret = new Long(getLastActiveTimestamp()).compareTo(o.getLastActiveTimestamp());
            }
        }
        return ret;

    }

    protected abstract long getLastActiveTimestamp();

    protected abstract String getName();

    public abstract ExtTooltip createTooltip(ServicePanel owner);

    public JComponent createIconComponent(ServicePanel servicePanel) {
        return new TinyProgressBar(servicePanel, this);
    }
}
