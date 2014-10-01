package org.jdownloader.extensions.schedulerV2.actions;

import jd.controlling.reconnect.Reconnecter;

import org.jdownloader.extensions.schedulerV2.helpers.ActionParameter;
import org.jdownloader.extensions.schedulerV2.translate.T;

public class ReconnectAction implements IScheduleAction {

    @Override
    public String getStorableID() {
        return "RECONNECT";
    }

    @Override
    public String getReadableName() {
        return T._.action_Reconnect();
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
