package org.jdownloader.settings;

import java.util.ArrayList;

import jd.controlling.proxy.DirectGatewayData;
import jd.controlling.proxy.ProxyData;

import org.appwork.storage.config.StorageHandler;

public class InternetConnectionSettingsValidator implements InternetConnectionSettings {

    public StorageHandler<?> getStorageHandler() {
        return null;
    }

    public void setCustomProxyList(ArrayList<ProxyData> ret) {

    }

    public ArrayList<ProxyData> getCustomProxyList() {
        return null;
    }

    public void setDirectGatewayList(ArrayList<DirectGatewayData> ret) {
    }

    public ArrayList<DirectGatewayData> getDirectGatewayList() {
        return null;
    }

    public void setNoneDefault(boolean b) {
    }

    public boolean isNoneDefault() {
        return false;
    }

    public void setNoneRotationEnabled(boolean proxyRotationEnabled) {

    }

    public boolean isNoneRotationEnabled() {
        return false;
    }

}
