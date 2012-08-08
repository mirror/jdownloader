package org.jdownloader.extensions.streaming.upnp;

import java.beans.PropertyChangeSupport;

import org.fourthline.cling.model.types.UnsignedIntegerFourBytes;
import org.fourthline.cling.support.xmicrosoft.AbstractMediaReceiverRegistrarService;

public class MediaReceiverRegistrar extends AbstractMediaReceiverRegistrarService {

    public MediaReceiverRegistrar() {

    }

    @Override
    public int isAuthorized(String deviceID) {
        return super.isAuthorized(deviceID);
    }

    @Override
    public int isValidated(String deviceID) {
        return super.isValidated(deviceID);
    }

    @Override
    public PropertyChangeSupport getPropertyChangeSupport() {
        return super.getPropertyChangeSupport();
    }

    @Override
    public UnsignedIntegerFourBytes getAuthorizationGrantedUpdateID() {
        return super.getAuthorizationGrantedUpdateID();
    }

    @Override
    public UnsignedIntegerFourBytes getAuthorizationDeniedUpdateID() {
        return super.getAuthorizationDeniedUpdateID();
    }

    @Override
    public UnsignedIntegerFourBytes getValidationSucceededUpdateID() {
        return super.getValidationSucceededUpdateID();
    }

    @Override
    public UnsignedIntegerFourBytes getValidationRevokedUpdateID() {
        return super.getValidationRevokedUpdateID();
    }

    @Override
    public byte[] registerDevice(byte[] registrationReqMsg) {
        return super.registerDevice(registrationReqMsg);
    }

}
