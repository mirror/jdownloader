package org.jdownloader.extensions.streaming.upnp.clingext;

import java.util.logging.Logger;

import org.fourthline.cling.UpnpService;
import org.fourthline.cling.model.ValidationError;
import org.fourthline.cling.model.ValidationException;
import org.fourthline.cling.model.message.IncomingDatagramMessage;
import org.fourthline.cling.model.message.UpnpRequest;
import org.fourthline.cling.model.meta.RemoteDevice;
import org.fourthline.cling.model.meta.RemoteDeviceIdentity;
import org.fourthline.cling.model.types.UDN;
import org.fourthline.cling.protocol.RetrieveRemoteDescriptors;
import org.fourthline.cling.protocol.async.ReceivingNotification;

public class ExtReceivingNotification extends ReceivingNotification {
    final private static Logger logger = Logger.getLogger(ExtReceivingNotification.class.getName());

    public ExtReceivingNotification(UpnpService upnpService, IncomingDatagramMessage<UpnpRequest> inputMessage) {
        super(upnpService, inputMessage);

    }

    @Override
    protected void execute() {
        UDN udn = getInputMessage().getUDN();
        if (udn == null) {
            logger.fine("Ignoring notification message without UDN: " + getInputMessage());
            return;
        }

        RemoteDeviceIdentity rdIdentity = new RemoteDeviceIdentity(getInputMessage());
        logger.fine("Received device notification: " + rdIdentity);

        RemoteDevice rd;
        try {
            rd = new ExtRemoteDevice(getInputMessage(), rdIdentity);

        } catch (ValidationException ex) {
            logger.warning("Validation errors of device during discovery: " + rdIdentity);
            for (ValidationError validationError : ex.getErrors()) {
                logger.warning(validationError.toString());
            }
            return;
        }

        if (getInputMessage().isAliveMessage()) {

            logger.fine("Received device ALIVE advertisement, descriptor location is: " + rdIdentity.getDescriptorURL());

            if (rdIdentity.getDescriptorURL() == null) {
                logger.finer("Ignoring message without location URL header: " + getInputMessage());
                return;
            }

            if (rdIdentity.getMaxAgeSeconds() == null) {
                logger.finer("Ignoring message without max-age header: " + getInputMessage());
                return;
            }

            if (getUpnpService().getRegistry().update(rdIdentity)) {
                logger.finer("Remote device was already known: " + udn);
                return;
            }

            // Unfortunately, we always have to retrieve the descriptor because at this point we
            // have no idea if it's a root or embedded device
            getUpnpService().getConfiguration().getAsyncProtocolExecutor().execute(new RetrieveRemoteDescriptors(getUpnpService(), rd));

        } else if (getInputMessage().isByeByeMessage()) {

            logger.fine("Received device BYEBYE advertisement");
            boolean removed = getUpnpService().getRegistry().removeDevice(rd);
            if (removed) {
                logger.fine("Removed remote device from registry: " + rd);
            }

        } else {
            logger.finer("Ignoring unknown notification message: " + getInputMessage());
        }
    }

}
