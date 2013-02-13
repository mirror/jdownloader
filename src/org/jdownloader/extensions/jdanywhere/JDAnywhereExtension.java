package org.jdownloader.extensions.jdanywhere;

import jd.plugins.AddonPanel;

import org.appwork.storage.config.JsonConfig;
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

    private JDAnywhereConfig      config;
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
            eva = null;
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
            int port = config.getPort();
            String user = config.getUsername();
            String pass = config.getPassword();
            remoteAPI.register(cma = new CaptchaApi(), port, user, pass);
            remoteAPI.register(coma = new ContentApi(), port, user, pass);
            remoteAPI.register(dba = new DashboardApi(), port, user, pass);
            remoteAPI.register(dla = new DownloadLinkApi(), port, user, pass);
            remoteAPI.register(fpa = new FilePackageApi(), port, user, pass);
            remoteAPI.register(lca = new LinkCrawlerApi(), port, user, pass);
            eva = new EventsAPI();
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

    public JDAnywhereConfig getConfig() {
        return config;
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

    public int getPort() {
        return getSettings().getPort();
    }

    void setPort(int port) {
        getSettings().setPort(port);
    }

}
