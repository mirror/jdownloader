package org.jdownloader.api.jdanywhere;

import org.appwork.storage.config.ValidationException;
import org.appwork.storage.config.events.GenericConfigEventListener;
import org.appwork.storage.config.handler.KeyHandler;
import org.appwork.utils.logging2.LogSource;
import org.jdownloader.api.RemoteAPIController;
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

    private CaptchaApi               cma;
    private ContentApi               coma;
    private DashboardApi             dba;
    private DownloadLinkApi          dla;
    private FilePackageApi           fpa;
    private LinkCrawlerApi           lca;

    private EventsAPI                eva;

    private JDAnywhereEventPublisher events;

    protected void stop() {
        logger.info("Stop API");
        RemoteAPIController.getInstance().unregister(cma);
        RemoteAPIController.getInstance().unregister(coma);
        RemoteAPIController.getInstance().unregister(dba);
        RemoteAPIController.getInstance().unregister(dla);
        RemoteAPIController.getInstance().unregister(fpa);
        RemoteAPIController.getInstance().unregister(lca);
        RemoteAPIController.getInstance().unregister(eva);
        RemoteAPIController.getInstance().unregister(events);

    }

    protected void start() {
        logger.info("Start API");
        RemoteAPIController.getInstance().register(cma = new CaptchaApi());
        RemoteAPIController.getInstance().register(coma = new ContentApi());
        RemoteAPIController.getInstance().register(dba = new DashboardApi());
        RemoteAPIController.getInstance().register(dla = new DownloadLinkApi());
        RemoteAPIController.getInstance().register(fpa = new FilePackageApi());
        RemoteAPIController.getInstance().register(lca = new LinkCrawlerApi());
        RemoteAPIController.getInstance().register(eva = new EventsAPI());
        RemoteAPIController.getInstance().register(events = new JDAnywhereEventPublisher());

    }

    public void init() {

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
