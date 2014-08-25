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

    public int getInvalidCount() {
        return 0;
    }

    private int compare(boolean x, boolean y) {
        return (x == y) ? 0 : (x ? 1 : -1);
    }

    private int compare(long x, long y) {
        return (x < y) ? -1 : ((x == y) ? 0 : 1);
    }

    @Override
    public int compareTo(ServiceCollection<?> o) {
        int ret = o.getName().compareTo(getName());
        if (ret == 0) {
            ret = compare(o.isEnabled(), isEnabled());
            if (ret == 0) {
                ret = compare(o.getInvalidCount(), getInvalidCount());
                if (ret == 0) {
                    if (o.isEnabled()) {
                        // sort on name
                        ret = compare(o.isInUse(), isInUse());
                    } else {
                        // last enabled one should be the first
                        ret = compare(o.getLastActiveTimestamp(), getLastActiveTimestamp());
                    }
                }
            }
        }
        return ret;

    }

    protected boolean isInUse() {
        return isEnabled();
    }

    protected abstract long getLastActiveTimestamp();

    protected abstract String getName();

    public abstract ExtTooltip createTooltip(ServicePanel owner);

    public JComponent createIconComponent(ServicePanel servicePanel) {
        return new TinyProgressBar(servicePanel, this);
    }
}
