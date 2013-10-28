package org.jdownloader.gui.notify;

import java.awt.event.MouseEvent;

import jd.controlling.downloadcontroller.SingleDownloadController;
import jd.gui.swing.jdgui.JDGui;
import jd.gui.swing.jdgui.views.settings.ConfigurationView;

import org.appwork.storage.config.JsonConfig;
import org.appwork.utils.swing.windowmanager.WindowManager.FrameState;
import org.jdownloader.gui.notify.gui.AbstractNotifyWindow;
import org.jdownloader.gui.notify.gui.BubbleNotifyConfigPanel;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.settings.GraphicalUserInterfaceSettings;

public class DownloadStoppedNotify extends AbstractNotifyWindow<DownloadStoppedContentPanel> {

    public DownloadStoppedNotify(SingleDownloadController downloadController) {
        super(_GUI._.DownloadStoppedNotify(), new DownloadStoppedContentPanel(downloadController));
    }

    @Override
    protected void onMouseClicked(MouseEvent m) {
        super.onMouseClicked(m);

        getContentComponent().onClicked();
    }

    protected void onSettings() {
        JDGui.getInstance().setFrameState(FrameState.TO_FRONT_FOCUSED);
        JsonConfig.create(GraphicalUserInterfaceSettings.class).setConfigViewVisible(true);
        JDGui.getInstance().setContent(ConfigurationView.getInstance(), true);
        ConfigurationView.getInstance().setSelectedSubPanel(BubbleNotifyConfigPanel.class);

    }

}
