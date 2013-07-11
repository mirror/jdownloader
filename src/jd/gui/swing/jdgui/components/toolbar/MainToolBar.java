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
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JRootPane;
import javax.swing.JSeparator;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.Timer;

import jd.SecondLevelLaunch;
import jd.controlling.downloadcontroller.DownloadWatchDog;
import jd.controlling.downloadcontroller.event.DownloadWatchdogListener;
import jd.gui.swing.SwingGui;
import jd.gui.swing.jdgui.components.speedmeter.SpeedMeterPanel;
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
import org.jdownloader.controlling.contextmenu.MenuLink;
import org.jdownloader.controlling.contextmenu.SeperatorData;
import org.jdownloader.controlling.contextmenu.gui.ExtPopupMenu;
import org.jdownloader.controlling.contextmenu.gui.MenuBuilder;
import org.jdownloader.gui.laf.jddefault.LAFOptions;
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
        if (comp != speedmeter) {
            comp.removeMouseListener(this);
            comp.addMouseListener(this);
        }

    }

    private void initToolbar() {

        List<MenuItemData> list = MainToolbarManager.getInstance().getMenuData().getItems();
        this.setLayout(new MigLayout("ins 0 3 0 0", "[]", "[grow,32!]"));
        AbstractButton ab;
        // System.out.println(this.getColConstraints(list.length));
        MenuItemData last = null;
        for (final MenuItemData menudata : list) {

            AbstractButton bt = null;
            AppAction action;
            try {
                if (!menudata.showItem(null)) continue;
                if (menudata instanceof SeperatorData) {
                    if (last != null && last instanceof SeperatorData) {
                        // no seperator dupes
                        continue;
                    }
                    this.add(new JSeparator(SwingConstants.VERTICAL), "gapleft 10,gapright 10,width 2!,pushy,growy");
                    last = menudata;
                    continue;
                }
                if (menudata._getValidateException() != null) continue;

                if (menudata.getType() == org.jdownloader.controlling.contextmenu.MenuItemData.Type.CONTAINER) {
                    bt = new ExtButton(new AppAction() {
                        private ExtPopupMenu root = null;

                        {
                            setTooltipText(menudata.getName());
                            setName(menudata.getName());
                            putValue(AbstractAction.LARGE_ICON_KEY, createDropdownImage(menudata.getIconKey()));
                        }

                        @Override
                        public void actionPerformed(ActionEvent e) {
                            ExtPopupMenu lroot = root;
                            if (lroot != null && lroot.isShowing()) { return; }
                            Object src = e.getSource();
                            if (e.getSource() instanceof Component) {
                                lroot = new ExtPopupMenu();
                                new MenuBuilder(MainToolbarManager.getInstance(), lroot, null, (MenuContainer) menudata).run();
                                Component button = (Component) e.getSource();
                                Dimension prefSize = lroot.getPreferredSize();
                                int[] insets = LAFOptions.getInstance().getPopupBorderInsets();
                                root = lroot;
                                lroot.show(button, -insets[1], button.getHeight() - insets[0]);

                            }
                        }

                    });

                    last = menudata;
                    final AbstractButton finalBt = bt;
                    bt.addMouseListener(new MouseListener() {

                        private Timer timer;

                        @Override
                        public void mouseReleased(MouseEvent e) {
                            Timer ltimer = timer;
                            timer = null;
                            if (ltimer != null) {
                                ltimer.stop();
                            }
                        }

                        @Override
                        public void mousePressed(MouseEvent e) {
                            Timer ltimer = timer;
                            timer = null;
                            if (ltimer != null) {
                                ltimer.stop();
                            }
                        }

                        @Override
                        public void mouseExited(MouseEvent e) {
                            Timer ltimer = timer;
                            timer = null;
                            if (ltimer != null) {
                                ltimer.stop();
                            }
                        }

                        @Override
                        public void mouseEntered(MouseEvent e) {
                            Timer ltimer = timer;
                            timer = null;
                            if (ltimer != null) ltimer.stop();
                            ltimer = new Timer(200, new ActionListener() {

                                @Override
                                public void actionPerformed(ActionEvent e) {
                                    finalBt.doClick();
                                }
                            });
                            ltimer.setRepeats(false);
                            ltimer.start();
                            timer = ltimer;

                        }

                        @Override
                        public void mouseClicked(MouseEvent e) {
                            Timer ltimer = timer;
                            timer = null;
                            if (ltimer != null) {
                                ltimer.stop();
                            }
                        }
                    });

                    add(bt, "width 32!,height 32!,hidemode 3");
                    bt.setHideActionText(true);
                    continue;
                } else if (menudata.getActionData() != null) {

                    action = menudata.createAction(null);

                    if (StringUtils.isNotEmpty(menudata._getShortcut())) {
                        action.setAccelerator(KeyStroke.getKeyStroke(menudata._getShortcut()));
                    }
                    if (action.isToggle()) {
                        bt = new JToggleButton(action);
                        ImageIcon icon;

                        bt.setIcon(icon = NewTheme.I().getCheckBoxImage(action.getIconKey(), false, 24));
                        bt.setRolloverIcon(icon);
                        bt.setSelectedIcon(icon = NewTheme.I().getCheckBoxImage(action.getIconKey(), true, 24));
                        bt.setRolloverSelectedIcon(icon);
                        add(bt, "width 32!,height 32!,hidemode 3");
                        bt.setHideActionText(true);
                    } else {
                        bt = new ExtButton(action);

                        bt.setIcon(NewTheme.I().getIcon(action.getIconKey(), 24));
                        add(bt, "width 32!,height 32!,hidemode 3");
                        bt.setHideActionText(true);
                    }
                    final AbstractButton finalBt = bt;
                    action.addPropertyChangeListener(new PropertyChangeListener() {

                        @Override
                        public void propertyChange(PropertyChangeEvent evt) {
                            if ("visible".equals(evt.getPropertyName())) {
                                Boolean value = (Boolean) evt.getNewValue();
                                finalBt.setVisible(value);
                            }
                        }
                    });
                    bt.setVisible(action.isVisible());
                    last = menudata;
                    final Object value = action.getValue(Action.ACCELERATOR_KEY);
                    if (value == null) {
                        continue;
                    }
                    final KeyStroke ks = (KeyStroke) value;

                    this.rootpane.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(ks, action);
                    this.rootpane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(ks, action);
                    this.rootpane.getActionMap().put(action, action);

                    // final String shortCut = action.getShortCutString();
                    // if (bt != null) {
                    // if (StringUtils.isEmpty(action.getTooltipText())) {
                    // bt.setToolTipText(shortCut != null ? " [" + shortCut + "]" : null);
                    // } else {
                    // bt.setToolTipText(action.getTooltipText() + (shortCut != null ? " [" + shortCut + "]" : ""));
                    // }
                    //
                    // }
                } else if (menudata instanceof MenuLink) {
                    final JComponent item = menudata.createItem(null);
                    if (StringUtils.isNotEmpty(menudata.getIconKey())) {
                        if (item instanceof AbstractButton) {
                            ((AbstractButton) item).setIcon(NewTheme.I().getIcon(menudata.getIconKey(), 24));
                        }
                    }

                    if (item instanceof JMenu) {

                        bt = new ExtButton(new AppAction() {
                            {
                                setName(((JMenu) item).getText());
                                Icon ico = ((JMenu) item).getIcon();

                                if (ico == null || Math.max(ico.getIconHeight(), ico.getIconWidth()) < 24) {
                                    ico = createDropdownImage("menu");
                                    putValue(AbstractAction.LARGE_ICON_KEY, ico);
                                    setSmallIcon(ico);
                                } else if (ico instanceof ImageIcon) {
                                    if (Math.max(ico.getIconHeight(), ico.getIconWidth()) != 24) {
                                        ico = ImageProvider.scaleImageIcon((ImageIcon) ico, 24, 24);
                                    }
                                    ico = createDropdownImage(((ImageIcon) ico).getImage());
                                    putValue(AbstractAction.LARGE_ICON_KEY, ico);
                                    setSmallIcon(ico);
                                } else {

                                    putValue(AbstractAction.LARGE_ICON_KEY, ico);
                                    setSmallIcon(ico);
                                }
                            }

                            @Override
                            public void actionPerformed(ActionEvent e) {
                                ExtPopupMenu root = new ExtPopupMenu();
                                for (Component c : ((JMenu) item).getMenuComponents()) {
                                    root.add(c);
                                }

                                Object src = e.getSource();
                                if (e.getSource() instanceof Component) {
                                    Component button = (Component) e.getSource();
                                    Dimension prefSize = root.getPreferredSize();
                                    int[] insets = LAFOptions.getInstance().getPopupBorderInsets();
                                    root.show(button, -insets[1], button.getHeight() - insets[0]);

                                }
                            }

                        });

                        add(bt, "width 32!,height 32!,hidemode 3");
                        bt.setHideActionText(true);
                    } else {
                        add(item, "aligny center,hidemode 3");
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
        return createDropdownImage(back);

    }

    protected ImageIcon createDropdownImage(Image back) {
        Image checkBox = NewTheme.I().getImage("popdownButton", -1, false);
        back = ImageProvider.merge(back, checkBox, 0, 0, 24 - checkBox.getWidth(null), 24 - checkBox.getHeight(null));

        return new ImageIcon(back);
    }

    protected void updateSpecial() {
        if (speedmeter != null) add(speedmeter, "hidemode 3, width 32:300:300,pushy,growy");
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