package org.jdownloader.extensions.jdanywhere;

import jd.plugins.AddonPanel;

import org.appwork.storage.config.JsonConfig;
import org.appwork.txtresource.TranslateInterface;
import org.jdownloader.api.RemoteAPIController;
import org.jdownloader.extensions.AbstractExtension;
import org.jdownloader.extensions.ExtensionConfigPanel;
import org.jdownloader.extensions.StartException;
import org.jdownloader.extensions.StopException;
import org.jdownloader.extensions.jdanywhere.api.captcha.CaptchaMobileAPIImpl;
import org.jdownloader.extensions.jdanywhere.api.content.ContentMobileAPIImpl;
import org.jdownloader.extensions.jdanywhere.api.downloads.DownloadsMobileAPIImpl;
import org.jdownloader.extensions.jdanywhere.api.linkcollector.LinkCollectorMobileAPIImpl;
import org.jdownloader.extensions.jdanywhere.api.toolbar.JDownloaderToolBarMobileAPIImpl;
import org.jdownloader.logging.LogController;

public class JDAnywhereExtension extends AbstractExtension<JDAnywhereConfig, TranslateInterface> {

    private DownloadsMobileAPIImpl          dma;
    private LinkCollectorMobileAPIImpl      lca;
    private CaptchaMobileAPIImpl            cma;
    private ContentMobileAPIImpl            coma;
    private JDownloaderToolBarMobileAPIImpl tma;

    private JDAnywhereConfig                config;
    private JDAnywhereConfigPanel           configPanel;

    @Override
    public boolean isDefaultEnabled() {
        return false;
    }

    @Override
    protected void stop() throws StopException {
        try {
            RemoteAPIController remoteAPI = RemoteAPIController.getInstance();
            remoteAPI.unregister(dma);
            remoteAPI.unregister(lca);
            remoteAPI.unregister(cma);
            remoteAPI.unregister(coma);
            remoteAPI.unregister(tma);
        } catch (final Throwable e) {
            LogController.CL().log(e);
            throw new StopException(e.getMessage());
        }
    }

    @Override
    protected void start() throws StartException {
        try {
            RemoteAPIController remoteAPI = RemoteAPIController.getInstance();
            remoteAPI.register(dma = new DownloadsMobileAPIImpl());
            remoteAPI.register(lca = new LinkCollectorMobileAPIImpl());
            remoteAPI.register(cma = new CaptchaMobileAPIImpl());
            remoteAPI.register(coma = new ContentMobileAPIImpl());
            remoteAPI.register(tma = new JDownloaderToolBarMobileAPIImpl());
        } catch (final Throwable e) {
            LogController.CL().log(e);
            throw new StartException(e);
        }
    }

    @Override
    protected void initExtension() throws StartException {
        setTitle("JDAnywhere");
        config = JsonConfig.create(JDAnywhereConfig.class);
        configPanel = new JDAnywhereConfigPanel(this, config);
    }

    @Override
    public ExtensionConfigPanel<JDAnywhereExtension> getConfigPanel() {
        return configPanel;
    }

    @Override
    public boolean hasConfigPanel() {
        return true;
    }

    @Override
    public String getDescription() {
        return "JDAnywhere";
    }

    @Override
    public AddonPanel<? extends AbstractExtension<JDAnywhereConfig, TranslateInterface>> getGUI() {
        return null;
    }

    public String getUsername() {
        return getSettings().getUsername();
    }

    void setUsername(String username) {
        getSettings().setUsername(username);
    }

    public String getPassword() {
        return getSettings().getPassword();
    }

    void setPassword(String password) {
        getSettings().setPassword(password);
    }

}
