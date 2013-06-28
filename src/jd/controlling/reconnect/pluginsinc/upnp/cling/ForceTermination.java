package jd.controlling.reconnect.pluginsinc.upnp.cling;

import org.fourthline.cling.controlpoint.ActionCallback;
import org.fourthline.cling.model.action.ActionInvocation;
import org.fourthline.cling.model.meta.Service;

public abstract class ForceTermination extends ActionCallback {

    public static final String FORCE_TERMINATION = "ForceTermination";

    public ForceTermination(Service service) {
        super(new ActionInvocation(service.getAction(FORCE_TERMINATION)));
    }

}