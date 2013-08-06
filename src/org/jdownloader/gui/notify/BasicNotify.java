package org.jdownloader.gui.notify;

import java.awt.event.ActionListener;

import javax.swing.ImageIcon;

import jd.gui.swing.jdgui.JDGui;
import jd.gui.swing.jdgui.views.settings.ConfigurationView;

import org.appwork.storage.config.JsonConfig;
import org.appwork.storage.config.handler.BooleanKeyHandler;
import org.appwork.utils.swing.WindowManager.FrameState;
import org.jdownloader.gui.notify.gui.AbstractNotifyWindow;
import org.jdownloader.gui.notify.gui.BubbleNotifyConfigPanel;
import org.jdownloader.settings.GraphicalUserInterfaceSettings;

public class BasicNotify extends AbstractNotifyWindow {

    private BooleanKeyHandler keyhandler;
    private ActionListener    actionListener;

    public BasicNotify(BooleanKeyHandler keyhandler, String caption, String text, ImageIcon icon) {
        super(caption, new BasicContentPanel(text, icon));
        this.keyhandler = keyhandler;
    }

    public void setActionListener(ActionListener actionListener) {
        this.actionListener = actionListener;
    }

    protected void onSettings() {
        JDGui.getInstance().setFrameState(FrameState.TO_FRONT_FOCUSED);
        JsonConfig.create(GraphicalUserInterfaceSettings.class).setConfigViewVisible(true);
        JDGui.getInstance().setContent(ConfigurationView.getInstance(), true);
        ConfigurationView.getInstance().setSelectedSubPanel(BubbleNotifyConfigPanel.class);

    }

}
