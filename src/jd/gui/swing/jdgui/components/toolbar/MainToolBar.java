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

package jd.gui.swing.jdgui.components.toolbar;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JRootPane;
import javax.swing.JSeparator;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;

import jd.SecondLevelLaunch;
import jd.controlling.downloadcontroller.DownloadWatchDog;
import jd.gui.swing.SwingGui;
import jd.gui.swing.jdgui.components.speedmeter.SpeedMeterPanel;
import jd.gui.swing.jdgui.components.toolbar.actions.AbstractToolbarAction;
import net.miginfocom.swing.MigLayout;

import org.appwork.controlling.StateEvent;
import org.appwork.controlling.StateEventListener;
import org.appwork.storage.config.JsonConfig;
import org.appwork.swing.components.ExtButton;
import org.appwork.swing.synthetica.SyntheticaSettings;
import org.appwork.utils.StringUtils;
import org.appwork.utils.swing.EDTRunner;
import org.jdownloader.gui.views.downloads.QuickSettingsPopup;
import org.jdownloader.images.NewTheme;

public class MainToolBar extends JToolBar {

    private static final long  serialVersionUID = 922971719957349497L;

    private static MainToolBar INSTANCE         = null;

    private SpeedMeterPanel    speedmeter;
    private JRootPane          rootpane;

    public static synchronized MainToolBar getInstance() {
        if (INSTANCE == null) INSTANCE = new MainToolBar();
        return INSTANCE;
    }

    private MainToolBar() {
        super();
        this.setRollover(true);
        this.setFloatable(false);
        setPreferredSize(new Dimension(-1, 38));
        SecondLevelLaunch.GUI_COMPLETE.executeWhenReached(new Runnable() {

            public void run() {
                new EDTRunner() {

                    @Override
                    protected void runInEDT() {
                        speedmeter = new SpeedMeterPanel(true, false);
                        speedmeter.setAntiAliasing(JsonConfig.create(SyntheticaSettings.class).isTextAntiAliasEnabled());
                        speedmeter.addMouseListener(new MouseAdapter() {

                            @Override
                            public void mouseClicked(MouseEvent e) {
                                if (e.isPopupTrigger() || e.getButton() == MouseEvent.BUTTON3) {
                                    QuickSettingsPopup pu = new QuickSettingsPopup();
                                    pu.show((Component) e.getSource(), e.getX(), e.getY());

                                }
                            }

                        });
                        updateToolbar();
                    }
                };
                DownloadWatchDog.getInstance().getStateMachine().addListener(new StateEventListener() {

                    public void onStateUpdate(StateEvent event) {
                    }

                    public void onStateChange(StateEvent event) {
                        if (DownloadWatchDog.IDLE_STATE == event.getNewState() || DownloadWatchDog.STOPPED_STATE == event.getNewState()) {
                            if (speedmeter != null) speedmeter.stop();
                        } else if (DownloadWatchDog.RUNNING_STATE == event.getNewState()) {
                            if (speedmeter != null) speedmeter.start();
                        }
                    }
                });
            }

        });

    }

    protected String getColConstraints(int size) {
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < size; ++i) {
            sb.append("[]2");
        }
        sb.append("[grow,fill][]");
        return sb.toString();
    }

    /**
     * USed to register the shortcuts to the rootpane during init
     * 
     * @param jdGui
     */
    public void registerAccelerators(final SwingGui jdGui) {
        this.rootpane = jdGui.getMainFrame().getRootPane();
    }

    /**
     * Updates the toolbar
     */
    public final void updateToolbar() {
        if (!SecondLevelLaunch.GUI_COMPLETE.isReached()) return;

        new EDTRunner() {

            @Override
            protected void runInEDT() {
                setVisible(false);
                removeAll();
                initToolbar();
                updateSpecial();
                setVisible(true);
                revalidate();
            }

        };

    }

    private void initToolbar() {

        AbstractToolbarAction[] list = ToolbarManager.getInstance().getList();
        this.setLayout(new MigLayout("ins 0 3 0 0", this.getColConstraints(list.length), "[grow,fill,32!]"));
        AbstractButton ab;
        // System.out.println(this.getColConstraints(list.length));
        for (final AbstractToolbarAction action : list) {
            action.init();
            AbstractButton bt = null;
            if (action == Seperator.getInstance()) {
                this.add(new JSeparator(SwingConstants.VERTICAL), "gapleft 10,gapright 10");
            } else if (action.isToggle()) {
                bt = new JToggleButton(action);
                bt.setIcon(NewTheme.I().getCheckBoxImage(action.createIconKey(), false, 24));
                bt.setSelectedIcon(NewTheme.I().getCheckBoxImage(action.createIconKey(), true, 24));
                add(bt, "width 32!");
                bt.setHideActionText(true);
            } else {
                bt = new ExtButton(action);
                add(bt, "width 32!");
                bt.setHideActionText(true);
            }

            final Object value = action.getValue(Action.ACCELERATOR_KEY);
            if (value == null) {
                continue;
            }
            final KeyStroke ks = (KeyStroke) value;
            this.rootpane.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(ks, action);
            this.rootpane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(ks, action);
            this.rootpane.getActionMap().put(action, action);

            final String shortCut = action.getShortCutString();
            if (bt != null) {
                if (StringUtils.isEmpty(action.getTooltipText())) {
                    bt.setToolTipText(shortCut != null ? " [" + shortCut + "]" : null);
                } else {
                    bt.setToolTipText(action.getTooltipText() + (shortCut != null ? " [" + shortCut + "]" : ""));
                }

            }
        }
        add(Box.createHorizontalGlue());

    }

    protected void updateSpecial() {
        if (speedmeter != null) add(speedmeter, "hidemode 3, width 32:300:300");
    }

    // made speedMeter Instance public. Used in remote API
    public SpeedMeterPanel getSpeedMeter() {
        return speedmeter;
    }

}