package org.jdownloader.gui.notify;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;

import javax.swing.ImageIcon;

import jd.gui.swing.jdgui.JDGui;
import jd.gui.swing.jdgui.views.settings.ConfigurationView;

import org.appwork.storage.config.JsonConfig;
import org.appwork.utils.swing.windowmanager.WindowManager.FrameState;
import org.jdownloader.gui.notify.gui.AbstractNotifyWindow;
import org.jdownloader.gui.notify.gui.BubbleNotifyConfigPanel;
import org.jdownloader.settings.GraphicalUserInterfaceSettings;

public class BasicNotify extends AbstractNotifyWindow<BasicContentPanel> {

    private ActionListener actionListener;

    public BasicNotify(String caption, String text, ImageIcon icon) {
        super(null, caption, new BasicContentPanel(text, icon));

    }

    @Override
    protected void onMouseClicked(MouseEvent m) {
        super.onMouseClicked(m);

        if (actionListener != null) actionListener.actionPerformed(new ActionEvent(m.getSource(), ActionEvent.ACTION_PERFORMED, null, m.getWhen(), m.getModifiers()));
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
