package org.jdownloader.extensions.schedulerV2.actions;

import jd.controlling.reconnect.Reconnecter;

import org.jdownloader.extensions.schedulerV2.helpers.ActionParameter;

public class ReconnectAction implements IScheduleAction {

    @Override
    public String getStorableID() {
        return "RECONNECT";
    }

    @Override
    public String getReadableName() {
        return "Reconnect"; // TODO
    }

    @Override
    public ActionParameter getParameterType() {
        return ActionParameter.NONE;
    }

    @Override
    public void execute(String parameter) {
        Reconnecter.getInstance().doReconnect();
    }

}
