package jd.plugins.optional.schedule.modules;

import java.io.Serializable;

public interface SchedulerModuleInterface extends Serializable {
    public String getName();
    public void execute(String parameter);
    public boolean needParameter();
    public String getTranslation();
    public boolean checkParameter(String parameter);
}
