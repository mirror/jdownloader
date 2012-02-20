package jd.gui.swing.jdgui.components.toolbar.actions;

import java.awt.event.ActionEvent;

import jd.controlling.downloadcontroller.DownloadWatchDog;

import org.appwork.controlling.StateEvent;
import org.appwork.controlling.StateEventListener;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.translate._JDT;

public class StartDownloadsAction extends AbstractToolbarAction {
    private static final StartDownloadsAction INSTANCE = new StartDownloadsAction();

    /**
     * get the only existing instance of StartDownloadsAction. This is a
     * singleton
     * 
     * @return
     */
    public static StartDownloadsAction getInstance() {
        return StartDownloadsAction.INSTANCE;
    }

    /**
     * Create a new instance of StartDownloadsAction. This is a singleton class.
     * Access the only existing instance by using {@link #getInstance()}.
     */
    private StartDownloadsAction() {

    }

    public void actionPerformed(ActionEvent e) {
        DownloadWatchDog.getInstance().startDownloads();
    }

    @Override
    public String createTooltip() {
        return _JDT._.StartDownloadsAction_createTooltip_();
    }

    @Override
    protected String createMnemonic() {
        return _GUI._.action_start_downloads_mnemonic();
    }

    @Override
    protected String createAccelerator() {
        return _GUI._.action_start_downloads_accelerator();
    }

    @Override
    public String createIconKey() {
        return "media-playback-start";
    }

    @Override
    protected void doInit() {
        DownloadWatchDog.getInstance().getStateMachine().addListener(new StateEventListener() {

            public void onStateUpdate(StateEvent event) {
            }

            public void onStateChange(StateEvent event) {
                if (DownloadWatchDog.IDLE_STATE == event.getNewState() || DownloadWatchDog.STOPPED_STATE == event.getNewState()) {
                    setEnabled(true);
                } else if (DownloadWatchDog.RUNNING_STATE == event.getNewState()) {
                    setEnabled(false);
                }
            }
        });
    }

}
