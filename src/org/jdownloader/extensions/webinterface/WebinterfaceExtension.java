package org.jdownloader.extensions.webinterface;

import java.io.IOException;

import jd.plugins.AddonPanel;

import org.appwork.storage.config.JsonConfig;
import org.appwork.utils.net.httpserver.HttpHandlerInfo;
import org.jdownloader.api.HttpServer;
import org.jdownloader.api.RemoteAPIConfig;
import org.jdownloader.extensions.AbstractExtension;
import org.jdownloader.extensions.ExtensionConfigPanel;
import org.jdownloader.extensions.StartException;
import org.jdownloader.extensions.StopException;

public class WebinterfaceExtension extends AbstractExtension<WebinterfaceConfig, WebinterfaceTranslation> {

    private HttpHandlerInfo handlerInfo = null;

    public ExtensionConfigPanel<WebinterfaceExtension> getConfigPanel() {
        return null;
    }

    public boolean hasConfigPanel() {
        return false;
    }

    public WebinterfaceExtension() throws StartException {
        setTitle(_.title());
    }

    @Override
    protected void stop() throws StopException {
        if (handlerInfo != null) HttpServer.getInstance().unregisterRequestHandler(handlerInfo);
    }

    @Override
    protected void start() throws StartException {
        int apiPort = JsonConfig.create(RemoteAPIConfig.class).getAPIPort();
        boolean apiLocal = JsonConfig.create(RemoteAPIConfig.class).getAPIlocalhost();
        try {
            handlerInfo = HttpServer.getInstance().registerRequestHandler(apiPort, apiLocal, new WebinterfaceRequestHandler(this));
        } catch (IOException e) {
            throw new StartException(e);
        }
    }

    @Override
    protected void initExtension() throws StartException {
    }

    @Override
    public String getAuthor() {
        return null;
    }

    @Override
    public String getDescription() {
        return "webinterface";
    }

    @Override
    public AddonPanel<? extends AbstractExtension<WebinterfaceConfig, WebinterfaceTranslation>> getGUI() {
        return null;
    }

}
