package jd.plugins.optional.schedule;

import java.io.Serializable;

import jd.plugins.optional.schedule.modules.SchedulerModuleInterface;

public class Executions implements Serializable {
    private static final long serialVersionUID = 8873752967587288865L;
    private SchedulerModuleInterface module;
    private String parameter;
    
    public Executions(SchedulerModuleInterface module, String parameter) {
        this.module = module;
        this.parameter = parameter;
    }

    public SchedulerModuleInterface getModule() {
        return module;
    }
    public String getParameter() {
        return parameter;
    }
    
    public void exceute() {
    	module.execute(parameter);
    }
}