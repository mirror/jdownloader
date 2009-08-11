package jd.plugins.optional.schedule;

import javax.swing.Icon;

import jd.gui.swing.jdgui.views.ClosableView;
import jd.utils.JDTheme;
import jd.utils.locale.JDL;

public class SchedulerView extends ClosableView {
    private static final long serialVersionUID = -7876057076125402969L;
    private static final String JDL_PREFIX = "jd.plugins.optional.schedule.SchedulerView.";

    public SchedulerView() {
        super();

        init();
    }
    
    public Icon getIcon() {
        return JDTheme.II("gui.images.config.eventmanager", 16, 16);
    }

    public String getTitle() {
        return JDL.L(JDL_PREFIX + "title", "Scheduler");
    }

    public String getTooltip() {
        return JDL.L(JDL_PREFIX + "tooltip", "Schedule your downloads");
    }

    @Override
    protected void onHide() {}

    @Override
    protected void onShow() {}
}