package org.jdownloader.extensions.streaming.renderer;

import org.jdownloader.extensions.streaming.upnp.RendererDevice;

public class RendererDeviceImpl implements RendererDevice {

    private String userAgentPattern;

    public RendererDeviceImpl(String userAgent) {
        this.userAgentPattern = userAgent;
    }

    @Override
    public String getUserAgentPattern() {
        return userAgentPattern;
    }

    public void setUserAgentPattern(String userAgentPattern) {
        this.userAgentPattern = userAgentPattern;
    }

}
