package org.jdownloader.api.jdanywhere;

import org.appwork.storage.config.ValidationException;
import org.appwork.storage.config.events.GenericConfigEventListener;
import org.appwork.storage.config.handler.KeyHandler;
import org.appwork.utils.logging2.LogSource;
import org.jdownloader.api.RemoteAPIController;
import org.jdownloader.api.downloads.DownloadsAPIImpl;
import org.jdownloader.api.jdanywhere.api.CaptchaApi;
import org.jdownloader.api.jdanywhere.api.ContentApi;
import org.jdownloader.api.jdanywhere.api.DashboardApi;
import org.jdownloader.api.jdanywhere.api.DownloadLinkApi;
import org.jdownloader.api.jdanywhere.api.EventsAPI;
import org.jdownloader.api.jdanywhere.api.FilePackageApi;
import org.jdownloader.api.jdanywhere.api.JDAnywhereEventPublisher;
import org.jdownloader.api.jdanywhere.api.LinkCrawlerApi;
import org.jdownloader.logging.LogController;
import org.jdownloader.settings.staticreferences.CFG_API;

/**
 * This API is only temp. It will be removed and replaced step by step. Please do never use Deprecated methods of the JDanywhere API
 * 
 * @author Thomas
 * 
 */
@Deprecated
public class JDAnywhereAPI implements GenericConfigEventListener<Boolean> {
    private static final JDAnywhereAPI INSTANCE = new JDAnywhereAPI();

    @Deprecated
    public static JDAnywhereAPI getInstance() {
        return INSTANCE;

    }

    private LogSource logger;

    private JDAnywhereAPI() {
        logger = LogController.getInstance().getLogger(JDAnywhereAPI.class.getName());
    }

    private CaptchaApi              cma;
    private ContentApi              coma;
    private DashboardApi            dba;
    private DownloadLinkApi         dla;
    private FilePackageApi          fpa;
    private LinkCrawlerApi          lca;

    private EventsAPI               eva;

    public JDAnywhereEventPublisher events;
    private RemoteAPIController     controller;
    private DownloadsAPIImpl        downloadsAPI;

    protected void stop() {
        logger.info("Stop API");
        controller.unregister(cma);
        controller.unregister(coma);
        controller.unregister(dba);
        controller.unregister(dla);
        controller.unregister(fpa);
        controller.unregister(lca);
        controller.unregister(eva);
        controller.unregister(events);

    }

    protected void start() {
        logger.info("Start API");
        controller.register(cma = new CaptchaApi());
        controller.register(coma = new ContentApi());
        controller.register(dba = new DashboardApi());
        controller.register(dla = new DownloadLinkApi(downloadsAPI));
        controller.register(fpa = new FilePackageApi());
        controller.register(lca = new LinkCrawlerApi());
        controller.register(eva = new EventsAPI());
        controller.register(events = new JDAnywhereEventPublisher());

    }

    public void init(RemoteAPIController remoteAPIController, DownloadsAPIImpl downloadsAPI) {
        this.controller = remoteAPIController;
        this.downloadsAPI = downloadsAPI;
        CFG_API.JDANYWHERE_API_ENABLED.getEventSender().addListener(this, true);
        onConfigValueModified(null, true);
    }

    @Override
    public void onConfigValidatorError(KeyHandler<Boolean> keyHandler, Boolean invalidValue, ValidationException validateException) {
    }

    @Override
    public void onConfigValueModified(KeyHandler<Boolean> keyHandler, Boolean newValue) {
        if (CFG_API.JDANYWHERE_API_ENABLED.isEnabled()) {
            try {
                start();
            } catch (Exception e) {
                logger.log(e);
            }
        } else {
            try {
                stop();
            } catch (Exception e) {
                logger.log(e);
            }
        }
    }
}
