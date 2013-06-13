package org.jdownloader.extensions.jdanywhere;

import jd.plugins.AddonPanel;

import org.appwork.txtresource.TranslateInterface;
import org.jdownloader.api.RemoteAPIController;
import org.jdownloader.extensions.AbstractExtension;
import org.jdownloader.extensions.ExtensionConfigPanel;
import org.jdownloader.extensions.StartException;
import org.jdownloader.extensions.StopException;
import org.jdownloader.extensions.jdanywhere.api.CaptchaApi;
import org.jdownloader.extensions.jdanywhere.api.ContentApi;
import org.jdownloader.extensions.jdanywhere.api.DashboardApi;
import org.jdownloader.extensions.jdanywhere.api.DownloadLinkApi;
import org.jdownloader.extensions.jdanywhere.api.EventsAPI;
import org.jdownloader.extensions.jdanywhere.api.FilePackageApi;
import org.jdownloader.extensions.jdanywhere.api.JDAnywhereEventPublisher;
import org.jdownloader.extensions.jdanywhere.api.LinkCrawlerApi;
import org.jdownloader.logging.LogController;

public class JDAnywhereExtension extends AbstractExtension<JDAnywhereConfig, TranslateInterface> {

    private CaptchaApi               cma;
    private ContentApi               coma;
    private DashboardApi             dba;
    private DownloadLinkApi          dla;
    private FilePackageApi           fpa;
    private LinkCrawlerApi           lca;

    private EventsAPI                eva;

    private JDAnywhereConfigPanel    configPanel;
    private JDAnywhereEventPublisher events;

    @Override
    public boolean isDefaultEnabled() {
        return false;
    }

    @Override
    public String getIconKey() {
        return "settings";
    }

    @Override
    protected void stop() throws StopException {
        try {
            RemoteAPIController.getInstance().unregister(cma);
            RemoteAPIController.getInstance().unregister(coma);
            RemoteAPIController.getInstance().unregister(dba);
            RemoteAPIController.getInstance().unregister(dla);
            RemoteAPIController.getInstance().unregister(fpa);
            RemoteAPIController.getInstance().unregister(lca);
            RemoteAPIController.getInstance().unregister(eva);
            RemoteAPIController.getInstance().unregister(events);
        } catch (final Throwable e) {
            LogController.CL().log(e);
            throw new StopException(e.getMessage());
        }

    }

    @Override
    protected void start() throws StartException {
        try {
            RemoteAPIController.getInstance().register(cma = new CaptchaApi());
            RemoteAPIController.getInstance().register(coma = new ContentApi());
            RemoteAPIController.getInstance().register(dba = new DashboardApi());
            RemoteAPIController.getInstance().register(dla = new DownloadLinkApi());
            RemoteAPIController.getInstance().register(fpa = new FilePackageApi());
            RemoteAPIController.getInstance().register(lca = new LinkCrawlerApi());
            RemoteAPIController.getInstance().register(eva = new EventsAPI());
            RemoteAPIController.getInstance().register(events = new JDAnywhereEventPublisher());
        } catch (final Throwable e) {
            LogController.CL().log(e);
            throw new StartException(e);
        }
    }

    @Override
    protected void initExtension() throws StartException {
        setTitle("JDAnywhere");

        configPanel = new JDAnywhereConfigPanel(this, getSettings());
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

    public JDAnywhereConfig getConfig() {
        return getSettings();
    }

}
