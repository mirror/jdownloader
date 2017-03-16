//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.

package org.jdownloader.extensions.antistandby;

import java.awt.Dialog.ModalityType;
import java.util.concurrent.atomic.AtomicReference;

import jd.controlling.downloadcontroller.DownloadWatchDog;
import jd.controlling.linkcollector.LinkCollector;
import jd.plugins.AddonPanel;

import org.appwork.shutdown.ShutdownController;
import org.appwork.shutdown.ShutdownRequest;
import org.appwork.shutdown.ShutdownVetoException;
import org.appwork.shutdown.ShutdownVetoListener;
import org.appwork.uio.ExceptionDialogInterface;
import org.appwork.uio.UIOManager;
import org.appwork.utils.Application;
import org.appwork.utils.os.CrossSystem;
import org.appwork.utils.swing.dialog.ExceptionDialog;
import org.jdownloader.extensions.AbstractExtension;
import org.jdownloader.extensions.ExtensionConfigPanel;
import org.jdownloader.extensions.StartException;
import org.jdownloader.extensions.StopException;
import org.jdownloader.extensions.antistandby.translate.AntistandbyTranslation;
import org.jdownloader.gui.translate._GUI;

public class AntiStandbyExtension extends AbstractExtension<AntiStandbyConfig, AntistandbyTranslation> implements ShutdownVetoListener {

    private final AtomicReference<Thread>              currentThread = new AtomicReference<Thread>(null);
    private ExtensionConfigPanel<AntiStandbyExtension> configPanel;

    public ExtensionConfigPanel<AntiStandbyExtension> getConfigPanel() {
        return configPanel;
    }

    public boolean hasConfigPanel() {
        return true;
    }

    public AntiStandbyExtension() throws StartException {
        super();
        setTitle(T.jd_plugins_optional_antistandby_jdantistandby());

    }

    public boolean isLinuxRunnable() {
        return false;
    }

    @Override
    public boolean isDefaultEnabled() {
        return true;
    }

    @Override
    protected void stop() throws StopException {
        setThread(null);
    }

    protected boolean isAntiStandbyThread() {
        return Thread.currentThread() == currentThread.get();
    }

    @Override
    public String getIconKey() {
        return "settings";
    }

    public boolean isQuickToggleEnabled() {
        return true;
    }

    private void setThread(final Thread thread) {
        if (thread != null) {
            ShutdownController.getInstance().addShutdownVetoListener(this);
        } else {
            ShutdownController.getInstance().removeShutdownVetoListener(this);
        }
        final Thread old = currentThread.getAndSet(thread);
        if (thread != null) {
            thread.start();
        }
        if (old != null) {
            old.interrupt();
        }
    }

    protected boolean requiresAntiStandby() {
        return requiresAntiStandby(getSettings().getMode());
    }

    protected boolean requiresAntiStandby(final Mode mode) {
        switch (mode) {
        case RUNNING:
            return true;
        case CRAWLING:
            return LinkCollector.getInstance().isCollecting();
        case DOWNLOADING:
            return DownloadWatchDog.getInstance().getStateMachine().isState(DownloadWatchDog.RUNNING_STATE, DownloadWatchDog.STOPPING_STATE);
        case DOWNLOADINGDORCRAWLING:
            return requiresAntiStandby(Mode.CRAWLING) || requiresAntiStandby(Mode.DOWNLOADING);
        default:
            return false;
        }
    }

    @Override
    protected void start() throws StartException {
        new Thread("AntiStandByLoader") {
            public void run() {
                try {
                    if (CrossSystem.isWindows()) {
                        final Thread thread = new WindowsAntiStandby(AntiStandbyExtension.this);
                        setThread(thread);
                    } else if (CrossSystem.isMac()) {
                        final Thread thread = new MacAntiStandBy(AntiStandbyExtension.this);
                        setThread(thread);
                    }
                } catch (Throwable e) {
                    ExceptionDialog d = new ExceptionDialog(UIOManager.STYLE_SHOW_DO_NOT_DISPLAY_AGAIN | UIOManager.LOGIC_COUNTDOWN | UIOManager.BUTTONS_HIDE_OK, _GUI.T.lit_error_occured(), _GUI.T.special_char_lib_loading_problem(Application.getHome(), AntiStandbyExtension.this.getName()), e, null, _GUI.T.lit_close()) {
                        @Override
                        public ModalityType getModalityType() {
                            return ModalityType.MODELESS;
                        }
                    };
                    UIOManager.I().show(ExceptionDialogInterface.class, d);
                    logger.log(e);
                    try {
                        AntiStandbyExtension.this.setEnabled(false);
                    } catch (Throwable e1) {
                        logger.log(e1);
                    }
                }
            }
        }.start();
    }

    @Override
    public String getDescription() {
        return T.jd_plugins_optional_antistandby_jdantistandby_description();
    }

    @Override
    public AddonPanel<AntiStandbyExtension> getGUI() {
        return null;
    }

    @Override
    protected void initExtension() throws StartException {
        if (!Application.isHeadless()) {
            configPanel = new AntistandbyConfigPanel(this);
        }
    }

    public Mode getMode() {
        final Mode ret = getSettings().getMode();
        if (ret == null) {
            return Mode.DOWNLOADING;
        } else {
            return ret;
        }
    }

    @Override
    public boolean isHeadlessRunnable() {
        return true;
    }

    @Override
    public void onShutdown(ShutdownRequest request) {
        setThread(null);
    }

    @Override
    public void onShutdownVeto(ShutdownRequest request) {
    }

    @Override
    public void onShutdownVetoRequest(ShutdownRequest request) throws ShutdownVetoException {
    }

    @Override
    public long getShutdownVetoPriority() {
        return 0;
    }

}