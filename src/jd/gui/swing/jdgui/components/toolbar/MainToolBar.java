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
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JRootPane;
import javax.swing.JSeparator;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;

import jd.SecondLevelLaunch;
import jd.controlling.downloadcontroller.DownloadWatchDog;
import jd.controlling.downloadcontroller.event.DownloadWatchdogListener;
import jd.gui.swing.SwingGui;
import jd.gui.swing.jdgui.components.speedmeter.SpeedMeterPanel;
import jd.gui.swing.laf.LookAndFeelController;
import net.miginfocom.swing.MigLayout;

import org.appwork.storage.config.JsonConfig;
import org.appwork.swing.components.ExtButton;
import org.appwork.swing.synthetica.SyntheticaSettings;
import org.appwork.utils.StringUtils;
import org.appwork.utils.ImageProvider.ImageProvider;
import org.appwork.utils.swing.EDTRunner;
import org.jdownloader.actions.AppAction;
import org.jdownloader.controlling.contextmenu.MenuContainer;
import org.jdownloader.controlling.contextmenu.MenuItemData;
import org.jdownloader.controlling.contextmenu.SeperatorData;
import org.jdownloader.controlling.contextmenu.gui.ExtPopupMenu;
import org.jdownloader.controlling.contextmenu.gui.MenuBuilder;
import org.jdownloader.gui.toolbar.MainToolbarManager;
import org.jdownloader.gui.views.downloads.QuickSettingsPopup;
import org.jdownloader.images.NewTheme;

public class MainToolBar extends JToolBar implements MouseListener, DownloadWatchdogListener {

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
        this.addMouseListener(this);
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
                DownloadWatchDog.getInstance().getEventSender().addListener(MainToolBar.this);

            }

        });

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

    protected void addImpl(Component comp, Object constraints, int index) {
        super.addImpl(comp, constraints, index);
        comp.removeMouseListener(this);
        comp.addMouseListener(this);

    }

    private void initToolbar() {

        List<MenuItemData> list = MainToolbarManager.getInstance().getMenuData().getItems();
        this.setLayout(new MigLayout("ins 0 3 0 0", "[]", "[grow,fill,32!]"));
        AbstractButton ab;
        // System.out.println(this.getColConstraints(list.length));
        for (final MenuItemData menudata : list) {

            AbstractButton bt = null;
            AppAction action;
            try {
                if (!menudata.showItem(null)) continue;
                if (menudata instanceof SeperatorData) {
                    this.add(new JSeparator(SwingConstants.VERTICAL), "gapleft 10,gapright 10,width 2!");
                    continue;
                }
                if (menudata._getValidateException() != null) continue;

                if (menudata.getType() == org.jdownloader.controlling.contextmenu.MenuItemData.Type.CONTAINER) {
                    bt = new ExtButton(new AppAction() {
                        {
                            setTooltipText(menudata.getName());
                            setName(menudata.getName());
                            putValue(AbstractAction.LARGE_ICON_KEY, createDropdownImage(menudata.getIconKey()));
                        }

                        @Override
                        public void actionPerformed(ActionEvent e) {
                            ExtPopupMenu root = new ExtPopupMenu();
                            new MenuBuilder(MainToolbarManager.getInstance(), root, null, (MenuContainer) menudata).run();
                            Object src = e.getSource();
                            if (e.getSource() instanceof Component) {
                                Component button = (Component) e.getSource();
                                Dimension prefSize = root.getPreferredSize();
                                int[] insets = LookAndFeelController.getInstance().getLAFOptions().getPopupBorderInsets();
                                root.show(button, -insets[1], button.getHeight() - insets[0]);

                            }
                        }

                    });
                    add(bt, "width 32!");
                    bt.setHideActionText(true);
                    continue;
                } else if (menudata.getActionData() != null) {

                    action = menudata.createAction(null);
                    if (action.isToggle()) {
                        bt = new JToggleButton(action);
                        ImageIcon icon;
                        bt.setIcon(icon = NewTheme.I().getCheckBoxImage(action.getIconKey(), false, 24));
                        bt.setRolloverIcon(icon);
                        bt.setSelectedIcon(icon = NewTheme.I().getCheckBoxImage(action.getIconKey(), true, 24));
                        bt.setRolloverSelectedIcon(icon);
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
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        add(Box.createHorizontalGlue(), "pushx,growx");

    }

    protected ImageIcon createDropdownImage(String iconKey) {

        Image back = NewTheme.I().getImage(iconKey, 20, false);
        Image checkBox = NewTheme.I().getImage("popdownButton", -1, false);
        back = ImageProvider.merge(back, checkBox, 0, 0, 24 - checkBox.getWidth(null), 24 - checkBox.getHeight(null));

        return new ImageIcon(back);

    }

    protected void updateSpecial() {
        if (speedmeter != null) add(speedmeter, "hidemode 3, width 32:300:300");
    }

    // made speedMeter Instance public. Used in remote API
    public SpeedMeterPanel getSpeedMeter() {
        return speedmeter;
    }

    @Override
    public void mouseClicked(MouseEvent e) {
    }

    @Override
    public void mousePressed(MouseEvent e) {
        if (e.isPopupTrigger() || e.getButton() == 3) {

            MainToolbarManager.getInstance().openGui();
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
    }

    @Override
    public void mouseEntered(MouseEvent e) {
    }

    @Override
    public void mouseExited(MouseEvent e) {
    }

    @Override
    public void onDownloadWatchdogDataUpdate() {
    }

    @Override
    public void onDownloadWatchdogStateIsIdle() {
        new EDTRunner() {

            @Override
            protected void runInEDT() {
                if (speedmeter != null) speedmeter.stop();
            }
        };
    }

    @Override
    public void onDownloadWatchdogStateIsPause() {
    }

    @Override
    public void onDownloadWatchdogStateIsRunning() {
        new EDTRunner() {

            @Override
            protected void runInEDT() {
                if (speedmeter != null) speedmeter.start();
            }
        };
    }

    @Override
    public void onDownloadWatchdogStateIsStopped() {
        new EDTRunner() {

            @Override
            protected void runInEDT() {
                if (speedmeter != null) speedmeter.stop();
            }
        };
    }

    @Override
    public void onDownloadWatchdogStateIsStopping() {
    }

}