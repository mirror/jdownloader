package jd.controlling.reconnect.pluginsinc.upnp.cling;

import org.fourthline.cling.controlpoint.ActionCallback;
import org.fourthline.cling.model.action.ActionInvocation;
import org.fourthline.cling.model.message.UpnpResponse;
import org.fourthline.cling.model.meta.Service;

public class RequestConnection extends ActionCallback {

    protected RequestConnection(Service service) {
        super(new ActionInvocation(service.getAction("RequestConnection")));

    }

    @Override
    public void success(ActionInvocation invocation) {
    }

    @Override
    public void failure(ActionInvocation invocation, UpnpResponse operation, String defaultMsg) {
    }

}
