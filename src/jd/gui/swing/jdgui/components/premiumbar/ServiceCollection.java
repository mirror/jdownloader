package jd.gui.swing.jdgui.components.premiumbar;

import java.util.ArrayList;

import javax.swing.Icon;
import javax.swing.JComponent;

import org.appwork.swing.components.tooltips.ExtTooltip;
import org.appwork.utils.CompareUtils;

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

    @Override
    public int compareTo(ServiceCollection<?> o) {
        int ret = CompareUtils.compareBoolean(o.isEnabled(), isEnabled());
        if (ret == 0) {
            ret = CompareUtils.compareBoolean(o.isInUse(), isInUse());
            if (ret == 0) {
                ret = CompareUtils.compareInt(getInvalidCount(), o.getInvalidCount());
                if (ret == 0) {
                    ret = getName().compareTo(o.getName());
                } else {
                    ret = CompareUtils.compareLong(o.getLastActiveTimestamp(), getLastActiveTimestamp());
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
