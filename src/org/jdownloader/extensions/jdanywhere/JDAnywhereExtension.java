package org.jdownloader.extensions.jdanywhere;

import jd.plugins.AddonPanel;

import org.appwork.txtresource.TranslateInterface;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
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
import org.jdownloader.extensions.jdanywhere.api.LinkCrawlerApi;
import org.jdownloader.logging.LogController;
import org.jdownloader.translate._JDT;
import org.jdownloader.updatev2.RestartController;

public class JDAnywhereExtension extends AbstractExtension<JDAnywhereConfig, TranslateInterface> {

    private CaptchaApi            cma;
    private ContentApi            coma;
    private DashboardApi          dba;
    private DownloadLinkApi       dla;
    private FilePackageApi        fpa;
    private LinkCrawlerApi        lca;

    private EventsAPI             eva;

    private JDAnywhereConfigPanel configPanel;

    @Override
    public boolean isDefaultEnabled() {
        return false;
    }

    @Override
    protected void stop() throws StopException {
        try {
            JDAnywhereController remoteAPI = JDAnywhereController.getInstance();
            remoteAPI.unregister(cma);
            remoteAPI.unregister(coma);
            remoteAPI.unregister(dba);
            remoteAPI.unregister(dla);
            remoteAPI.unregister(fpa);
            remoteAPI.unregister(lca);
            remoteAPI.unregister(eva);

        } catch (final Throwable e) {
            LogController.CL().log(e);
            throw new StopException(e.getMessage());
        }
        showRestartRequiredMessage();
    }

    protected void showRestartRequiredMessage() {
        try {
            Dialog.getInstance().showConfirmDialog(0, _JDT._.dialog_optional_showRestartRequiredMessage_title(), _JDT._.dialog_optional_showRestartRequiredMessage_msg(), null, _JDT._.basics_yes(), _JDT._.basics_no());
            RestartController.getInstance().exitAsynch();
        } catch (DialogClosedException e) {
        } catch (DialogCanceledException e) {
        }
    }

    @Override
    protected void start() throws StartException {
        try {
            JDAnywhereController remoteAPI = JDAnywhereController.getInstance();

            remoteAPI.register(cma = new CaptchaApi());
            remoteAPI.register(coma = new ContentApi());
            remoteAPI.register(dba = new DashboardApi());
            remoteAPI.register(dla = new DownloadLinkApi());
            remoteAPI.register(fpa = new FilePackageApi());
            remoteAPI.register(lca = new LinkCrawlerApi());
            remoteAPI.register(eva = new EventsAPI());
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
