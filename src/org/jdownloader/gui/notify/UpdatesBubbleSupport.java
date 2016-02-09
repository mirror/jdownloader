package org.jdownloader.gui.notify;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.Icon;

import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.notify.BubbleNotify.AbstractNotifyWindowFactory;
import org.jdownloader.gui.notify.gui.AbstractNotifyWindow;
import org.jdownloader.gui.notify.gui.CFG_BUBBLE;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.AbstractIcon;
import org.jdownloader.updatev2.InstallLog;
import org.jdownloader.updatev2.UpdateController;
import org.jdownloader.updatev2.UpdaterListener;

import jd.gui.swing.jdgui.components.toolbar.actions.UpdateAction;

public class UpdatesBubbleSupport extends AbstractBubbleSupport implements UpdaterListener {

    public UpdatesBubbleSupport() {
        super(_GUI.T.plugins_optional_JDLightTray_ballon_updates2(), CFG_BUBBLE.BUBBLE_NOTIFY_ON_UPDATE_AVAILABLE_ENABLED);
        UpdateController.getInstance().getEventSender().addListener(this, true);
    }

    private volatile boolean updatesNotified;

    @Override
    public void onUpdatesAvailable(boolean selfupdate, InstallLog installlog) {
        if (CFG_BUBBLE.BUBBLE_NOTIFY_ON_UPDATE_AVAILABLE_ENABLED.isEnabled()) {
            if (UpdateController.getInstance().hasPendingUpdates() && !updatesNotified) {
                updatesNotified = true;
                show(new AbstractNotifyWindowFactory() {

                    @Override
                    public AbstractNotifyWindow<?> buildAbstractNotifyWindow() {
                        BasicNotify no = new BasicNotify(_GUI.T.balloon_updates(), _GUI.T.balloon_updates_msg(), new AbstractIcon(IconKey.ICON_UPDATE, 32));
                        no.setActionListener(new ActionListener() {

                            public void actionPerformed(ActionEvent e) {
                                new UpdateAction().actionPerformed(e);
                            }
                        });
                        return no;
                    }
                });
            } else if (!UpdateController.getInstance().hasPendingUpdates()) {
                updatesNotified = false;
            }
        }
    }

    @Override
    public List<Element> getElements() {
        return null;
    }

    @Override
    public void onUpdaterStatusUpdate(String label, Icon icon, double progress) {
    }

}
