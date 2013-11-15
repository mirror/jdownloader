//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
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

package org.jdownloader.gui.notify.gui;

import java.util.ArrayList;
import java.util.List;

import javax.swing.ImageIcon;

import jd.gui.swing.jdgui.views.settings.components.Checkbox;
import jd.gui.swing.jdgui.views.settings.components.ComboBox;
import jd.gui.swing.jdgui.views.settings.components.Spinner;
import jd.gui.swing.jdgui.views.settings.components.StateUpdateListener;

import org.appwork.scheduler.DelayedRunnable;
import org.appwork.storage.config.ValidationException;
import org.appwork.storage.config.events.GenericConfigEventListener;
import org.appwork.storage.config.handler.KeyHandler;
import org.appwork.utils.swing.EDTRunner;
import org.jdownloader.gui.notify.BubbleNotify;
import org.jdownloader.gui.notify.AbstractBubbleSupport;
import org.jdownloader.gui.notify.gui.BubbleNotifyConfig.BubbleNotifyEnabledState;
import org.jdownloader.gui.settings.AbstractConfigPanel;
import org.jdownloader.gui.settings.Pair;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;

public class BubbleNotifyConfigPanel extends AbstractConfigPanel implements StateUpdateListener, GenericConfigEventListener<Enum> {

    private static final long         serialVersionUID = 1L;
    private ArrayList<Pair<Checkbox>> boxes;
    private DelayedRunnable           delayer;

    public String getTitle() {
        return _GUI._.NotifierConfigPanel_getTitle();
    }

    public BubbleNotifyConfigPanel() {
        super();

        layoutComponents();
        CFG_BUBBLE.BUBBLE_NOTIFY_ENABLED_STATE.getEventSender().addListener(this, true);
        onConfigValueModified(null, null);
        delayer = new DelayedRunnable(3000, 5000) {

            @Override
            public void delayedrun() {
                new EDTRunner() {

                    @Override
                    protected void runInEDT() {
                        removeAll();
                        layoutComponents();
                        revalidate();
                        repaint();
                    }
                };
            }
        };
    }

    protected void layoutComponents() {
        this.addHeader(getTitle(), NewTheme.I().getIcon("bubble", 32));
        this.addDescription(_GUI._.plugins_optional_JDLightTray_ballon_desc());
        addPair(_GUI._.BubbleNotifyConfigPanel_BubbleNotifyConfigPanel_enabledstate(), null,
                new ComboBox<BubbleNotifyEnabledState>((KeyHandler<BubbleNotifyEnabledState>) CFG_BUBBLE.SH.getKeyHandler("BubbleNotifyEnabledState", KeyHandler.class), new BubbleNotifyEnabledState[] { BubbleNotifyEnabledState.ALWAYS, BubbleNotifyEnabledState.JD_NOT_ACTIVE, BubbleNotifyEnabledState.TRAY_OR_TASKBAR, BubbleNotifyEnabledState.TASKBAR, BubbleNotifyEnabledState.TRAY, BubbleNotifyEnabledState.NEVER, }, new String[] { _GUI._.BubbleNotifyConfigPanel_BubbleNotifyConfigPanel_always(), _GUI._.BubbleNotifyConfigPanel_BubbleNotifyConfigPanel_jdnotactive(), _GUI._.BubbleNotifyConfigPanel_BubbleNotifyConfigPanel_trayortask(), _GUI._.BubbleNotifyConfigPanel_BubbleNotifyConfigPanel_taskbar(), _GUI._.BubbleNotifyConfigPanel_BubbleNotifyConfigPanel_tray(), _GUI._.BubbleNotifyConfigPanel_BubbleNotifyConfigPanel_never(), }));

        boxes = new ArrayList<Pair<Checkbox>>();
        for (AbstractBubbleSupport pt : BubbleNotify.getInstance().getTypes()) {
            boxes.add(addPair(_GUI._.lit_or() + " " + pt.getLabel(), "skip 1,split 2,pushx,growx", null, new Checkbox(pt.getKeyHandler())));
        }
        this.addHeader(_GUI._.BubbleNotifyConfigPanel_BubbleNotifyConfigPanel_settings_(), NewTheme.I().getIcon("settings", 32));

        addPair(_GUI._.BubbleNotifyConfigPanel_BubbleNotifyConfigPanel_silent_(), null, new Checkbox(CFG_BUBBLE.BUBBLE_NOTIFY_ENABLED_DURING_SILENT_MODE));
        addPair(_GUI._.BubbleNotifyConfigPanel_BubbleNotifyConfigPanel_timeout(), null, new Spinner(CFG_BUBBLE.DEFAULT_TIMEOUT));
        addPair(_GUI._.BubbleNotifyConfigPanel_BubbleNotifyConfigPanel_fadetime(), null, new Spinner(CFG_BUBBLE.FADE_ANIMATION_DURATION));
    }

    public void updateTypes(List<AbstractBubbleSupport> types) {
    }

    @Override
    public ImageIcon getIcon() {
        return NewTheme.I().getIcon("bubble", 32);
    }

    @Override
    public void save() {

    }

    @Override
    public void updateContents() {

    }

    @Override
    public void onStateUpdated() {

        delayer.resetAndStart();
    }

    @Override
    public void onConfigValidatorError(KeyHandler<Enum> keyHandler, Enum invalidValue, ValidationException validateException) {
    }

    @Override
    public void onConfigValueModified(KeyHandler<Enum> keyHandler, Enum newValue) {
        new EDTRunner() {

            @Override
            protected void runInEDT() {
                for (Pair<Checkbox> pc : boxes) {
                    pc.setEnabled(CFG_BUBBLE.BUBBLE_NOTIFY_ENABLED_STATE.getValue() != BubbleNotifyEnabledState.NEVER);
                }
            }
        };
    }

}