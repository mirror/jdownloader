package jd.gui.swing.jdgui.views.settings.panels.pluginsettings;

import java.awt.Container;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.Scrollable;
import javax.swing.SwingUtilities;

import jd.controlling.AccountController;
import jd.gui.swing.jdgui.JDGui;
import jd.gui.swing.jdgui.interfaces.SwitchPanel;
import jd.gui.swing.jdgui.views.settings.components.SettingsComponent;
import jd.gui.swing.jdgui.views.settings.components.StateUpdateListener;
import jd.gui.swing.jdgui.views.settings.sidebar.AddonConfig;
import jd.plugins.Account;
import jd.plugins.Plugin;
import jd.plugins.PluginConfigPanelNG;
import net.miginfocom.swing.MigLayout;

import org.appwork.exceptions.WTFException;
import org.appwork.storage.config.JsonConfig;
import org.appwork.swing.MigPanel;
import org.appwork.swing.components.ExtButton;
import org.appwork.swing.components.circlebar.CircledProgressBar;
import org.appwork.swing.components.circlebar.ImagePainter;
import org.appwork.swing.components.searchcombo.SearchComboBox;
import org.appwork.utils.logging2.LogSource;
import org.appwork.utils.swing.EDTRunner;
import org.appwork.utils.swing.SwingUtils;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.jdownloader.DomainInfo;
import org.jdownloader.actions.AppAction;
import org.jdownloader.extensions.Header;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.AbstractIcon;
import org.jdownloader.logging.LogController;
import org.jdownloader.plugins.controller.LazyPlugin;
import org.jdownloader.plugins.controller.UpdateRequiredClassNotFoundException;
import org.jdownloader.plugins.controller.crawler.CrawlerPluginController;
import org.jdownloader.plugins.controller.crawler.LazyCrawlerPlugin;
import org.jdownloader.plugins.controller.host.HostPluginController;
import org.jdownloader.plugins.controller.host.LazyHostPlugin;
import org.jdownloader.settings.GraphicalUserInterfaceSettings;

public class PluginSettingsPanel extends JPanel implements SettingsComponent, ActionListener {
    /**
     *
     */
    private static final long             serialVersionUID = 1L;
    private Icon                          decryterIcon;
    private MigPanel                      card;
    protected SwitchPanel                 configPanel;
    protected List<Pattern>               filter;
    private Header                        header;
    private SearchComboBox<LazyPlugin<?>> searchCombobox;
    private ExtButton                     resetButton;
    private LogSource                     logger;

    public void addStateUpdateListener(StateUpdateListener listener) {
        throw new IllegalStateException("Not implemented");
    }

    public Dimension getPreferredScrollableViewportSize() {
        return this.getPreferredSize();
    }

    public PluginSettingsPanel() {
        super(new MigLayout("ins 0,wrap 1", "[grow,fill]", "[][][grow,fill]"));
        decryterIcon = new AbstractIcon(IconKey.ICON_LINKGRABBER, 16);
        logger = LogController.getInstance().getLogger(PluginSettingsPanel.class.getName());
        searchCombobox = new SearchComboBox<LazyPlugin<?>>() {
            @Override
            protected Icon getIconForValue(LazyPlugin<?> value) {
                if (value == null) {
                    return null;
                }
                return DomainInfo.getInstance(value.getDisplayName()).getFavIcon();
            }

            @Override
            public void onChanged() {
                super.onChanged();
            }

            @Override
            protected String getTextForValue(LazyPlugin<?> value) {
                if (value == null) {
                    return "";
                }
                return value.getDisplayName();
            }
        };
        searchCombobox.setActualMaximumRowCount(20);
        searchCombobox.addActionListener(this);
        setOpaque(false);
        // left.setBorder(new JTextField().getBorder());
        // selector.setPreferredSize(new Dimension(200, 20000));
        // sp.setBorder(null);
        resetButton = new ExtButton(new AppAction() {
            {
                setIconKey(IconKey.ICON_RESET);
                setTooltipText(_GUI.T.PluginSettingsPanel_PluginSettingsPanel_reset());
            }

            @Override
            public void actionPerformed(ActionEvent e) {
                if (configPanel != null && configPanel.isShown()) {
                    Plugin proto = null;
                    try {
                        if (currentItem != null) {
                            Dialog.getInstance().showConfirmDialog(0, _GUI.T.lit_are_you_sure(), _GUI.T.PluginSettingsPanel_are_you_sure(currentItem.getDisplayName()));
                            proto = currentItem.getPrototype(null);
                            PluginConfigPanelNG ccp = proto.getConfigPanel();
                            if (ccp != null) {
                                ccp.reset();
                            } else {
                                proto.getPluginConfig().reset();
                                AddonConfig.getInstance(proto.getConfig(), "", false).reload();
                            }
                            // avoid that the panel saves it's data on hide;
                            configPanel = null;
                            show(currentItem);
                            Dialog.getInstance().showMessageDialog(_GUI.T.PluginSettingsPanel_actionPerformed_reset_done(currentItem.getDisplayName()));
                        }
                    } catch (UpdateRequiredClassNotFoundException e1) {
                        org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().log(e1);
                    } catch (DialogClosedException e1) {
                        e1.printStackTrace();
                    } catch (DialogCanceledException e1) {
                        e1.printStackTrace();
                    }
                }
            }
        });
        this.card = new MigPanel("ins 3 0 0 0", "[grow,fill]", "[grow,fill]");
        header = new Header("", null);
        card.setOpaque(false);
        if (CrawlerPluginController.list(false) == null) {
            MigPanel loaderPanel = new MigPanel("ins 0,wrap 1", "[grow]", "50[][]");
            loaderPanel.setOpaque(false);
            loaderPanel.setBackground(null);
            CircledProgressBar loader = new CircledProgressBar();
            loader.setValueClipPainter(new ImagePainter(new AbstractIcon(IconKey.ICON_BOTTY_ROBOT, 256), 1.0f));
            loader.setNonvalueClipPainter(new ImagePainter(new AbstractIcon(IconKey.ICON_BOTTY_ROBOT, 256), 0.1f));
            ((ImagePainter) loader.getValueClipPainter()).setBackground(null);
            ((ImagePainter) loader.getValueClipPainter()).setForeground(null);
            loader.setIndeterminate(true);
            loaderPanel.add(loader, "width 256!,height 256!,alignx center");
            loaderPanel.add(new JLabel(_GUI.T.PluginSettingsPanel_PluginSettingsPanel_waittext_()), "alignx center");
            add(loaderPanel, "spanx,pushx,growx,spany,growy,pushy");
        }
        new Thread("Plugin Init") {
            public void run() {
                // try {
                // Thread.sleep(5000);
                // } catch (InterruptedException e) {
                // e.printStackTrace();
                // }
                fill();
                new EDTRunner() {
                    @Override
                    protected void runInEDT() {
                        removeAll();
                        add(SwingUtils.toBold(new JLabel(_GUI.T.PluginSettingsPanel_runInEDT_choose_())), "split 3,shrinkx");
                        add(searchCombobox, "pushx,growx,height 24!");
                        add(resetButton, "width 22!,height 24!");
                        add(header, "growx,pushx,gaptop 10");
                        add(card, "spanx,pushx,growx");
                        if (searchCombobox.getModel().getSize() > 0) {
                            final String activePlugin = JsonConfig.create(GraphicalUserInterfaceSettings.class).getActivePluginConfigPanel();
                            setPlugin(activePlugin);
                        }
                    }
                };
            }
        }.start();
    }

    private void setPlugin(final String pluginID) {
        new EDTRunner() {
            @Override
            protected void runInEDT() {
                if (searchCombobox.getModel().getSize() > 0) {
                    int selectIndex = 0;
                    if (pluginID != null) {
                        for (int i = 0; i < searchCombobox.getModel().getSize(); i++) {
                            if (((LazyPlugin<?>) searchCombobox.getModel().getElementAt(i)).getID().equals(pluginID)) {
                                selectIndex = i;
                                break;
                            }
                        }
                    }
                    searchCombobox.setSelectedIndex(selectIndex);
                }
            }
        };
    }

    public void setPlugin(final LazyPlugin<?> plugin) {
        new EDTRunner() {
            @Override
            protected void runInEDT() {
                if (plugin != null) {
                    if (searchCombobox.getModel().getSize() > 0) {
                        setPlugin(plugin.getID());
                    } else {
                        JsonConfig.create(GraphicalUserInterfaceSettings.class).setActivePluginConfigPanel(plugin.getID());
                    }
                }
            }
        };
    }

    private void fill() {
        searchCombobox.setList(fillModel());
    }

    public List<LazyPlugin<?>> fillModel() {
        ArrayList<LazyPlugin<?>> lst = new ArrayList<LazyPlugin<?>>();
        for (LazyHostPlugin plg : HostPluginController.getInstance().list()) {
            if (plg.isHasConfig() || plg.isPremium()) {
                lst.add(plg);
            }
        }
        for (LazyCrawlerPlugin plg : CrawlerPluginController.getInstance().list()) {
            if (plg.isHasConfig()) {
                lst.add(plg);
            }
        }
        Collections.sort(lst, new Comparator<LazyPlugin<?>>() {
            @Override
            public int compare(LazyPlugin<?> o1, LazyPlugin<?> o2) {
                return o1.getDisplayName().compareToIgnoreCase(o2.getDisplayName());
            }
        });
        Collections.sort(lst, new Comparator<LazyPlugin<?>>() {
            public int compare(int x, int y) {
                return (x < y) ? -1 : ((x == y) ? 0 : 1);
            }

            @Override
            public int compare(LazyPlugin<?> o1, LazyPlugin<?> o2) {
                final int o1Num;
                if (o1 instanceof LazyHostPlugin && ((LazyHostPlugin) o1).isPremium()) {
                    o1Num = AccountController.getInstance().getAccountsSize(o1.getDisplayName());
                } else {
                    o1Num = 0;
                }
                final int o2Num;
                if (o1 == o2) {
                    o2Num = o1Num;
                } else if (o2 instanceof LazyHostPlugin && ((LazyHostPlugin) o2).isPremium()) {
                    o2Num = AccountController.getInstance().getAccountsSize(o2.getDisplayName());
                } else {
                    o2Num = 0;
                }
                return compare(o2Num, o1Num);
            }
        });
        return lst;
    }

    public String getConstraints() {
        return "wmin 10,height 200:n:n,growy,pushy";
    }

    public boolean isMultiline() {
        return true;
    }

    public void actionPerformed(ActionEvent e) {
        final LazyPlugin<?> selected = searchCombobox.getSelectedItem();
        if (selected != null) {
            JsonConfig.create(GraphicalUserInterfaceSettings.class).setActivePluginConfigPanel(selected.getID());
            show(selected);
        }
    }

    private LazyPlugin<?>         currentItem = null;
    private JScrollPane           sp;
    protected PluginConfigPanelNG newCP;
    private Account               scrollToAccount;

    private void show(final LazyPlugin<?> selectedItem) {
        new EDTRunner() {
            @Override
            protected void runInEDT() {
                final long start = System.currentTimeMillis();
                card.removeAll();
                if (configPanel != null) {
                    configPanel.setHidden();
                }
                currentItem = selectedItem;
                try {
                    JDGui.getInstance().setWaiting(true);
                    Plugin protoType = selectedItem.getPrototype(null);
                    newCP = protoType.getConfigPanel();
                    if (newCP == null) {
                        if (selectedItem instanceof LazyHostPlugin) {
                            if (!((LazyHostPlugin) selectedItem).isHasConfig()) {
                                if (((LazyHostPlugin) selectedItem).isPremium()) {
                                    throw new WTFException("Should not happen");
                                }
                            }
                        }
                    }
                    sp = null;
                    if (newCP != null) {
                        newCP.initLayout(protoType);
                        configPanel = scrollerWrapper(newCP);
                    } else {
                        configPanel = PluginConfigPanel.create(selectedItem);
                    }
                    if (configPanel != null) {
                        configPanel.setShown();
                        card.add(configPanel);
                        if (selectedItem != null) {
                            if (selectedItem instanceof LazyHostPlugin) {
                                header.setText(_GUI.T.PluginSettingsPanel_runInEDT_plugin_header_text_host(selectedItem.getDisplayName()));
                                // IconIO.
                                Icon fav = DomainInfo.getInstance(((LazyHostPlugin) selectedItem).getHost()).getFavIcon();
                                header.setIcon(fav);
                            } else {
                                header.setText(_GUI.T.PluginSettingsPanel_runInEDT_plugin_header_text_decrypt(selectedItem.getDisplayName()));
                                Icon fav = DomainInfo.getInstance(((LazyCrawlerPlugin) selectedItem).getDisplayName()).getFavIcon();
                                if (fav == null) {
                                    header.setIcon(decryterIcon);
                                } else {
                                    header.setIcon(fav);
                                }
                            }
                            header.setVisible(true);
                        } else {
                            header.setVisible(false);
                        }
                    }
                } catch (UpdateRequiredClassNotFoundException e) {
                    logger.log(e);
                } finally {
                    JDGui.getInstance().setWaiting(false);
                }
                revalidate();
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        scrollToAccount(null);
                    }
                });
            }
        };
    }

    public class Scroll extends JPanel implements Scrollable {
        public Scroll() {
            setOpaque(false);
        }

        @Override
        public Dimension getPreferredSize() {
            return super.getPreferredSize();
        }

        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        public int getScrollableBlockIncrement(final Rectangle visibleRect, final int orientation, final int direction) {
            return Math.max(visibleRect.height * 9 / 10, 1);
        }

        public int getScrollableUnitIncrement(final Rectangle visibleRect, final int orientation, final int direction) {
            return Math.max(visibleRect.height / 10, 1);
        }

        public boolean getScrollableTracksViewportWidth() {
            Container p = getParent();
            if (p.getSize().width < getMinimumSize().width) {
                // enable horizontal scrolling if the viewport size is less than the minimum panel size
                return false;
            }
            return true;
        }

        public boolean getScrollableTracksViewportHeight() {
            return false;
        }
    }

    protected SwitchPanel scrollerWrapper(final PluginConfigPanelNG createConfigPanel) {
        Scroll ret = new Scroll();
        ret.setLayout(new MigLayout("ins 0", "[grow,fill]", "[grow,fill]"));
        ret.add(createConfigPanel);
        sp = new JScrollPane(ret);
        // sp.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        sp.getVerticalScrollBar().setUnitIncrement(24);
        sp.setBorder(null);
        sp.setOpaque(false);
        sp.getViewport().setOpaque(false);
        sp.setViewportBorder(BorderFactory.createEmptyBorder(0, 0, 0, 5));
        SwitchPanel wrapper = new SwitchPanel() {
            {
                setOpaque(false);
                setLayout(new MigLayout("ins 0", "[grow,fill]", "[grow,fill]"));
            }

            @Override
            protected void onShow() {
                createConfigPanel.setShown();
            }

            @Override
            protected void onHide() {
                createConfigPanel.setHidden();
            }
        };
        wrapper.add(sp);
        return wrapper;
    }

    public void setHidden() {
        if (configPanel != null) {
            configPanel.setHidden();
            configPanel = null;
        }
        if (card != null) {
            card.removeAll();
        }
        currentItem = null;
    }

    public void setShown() {
        if (configPanel != null) {
            configPanel.setShown();
        }
    }

    public void scrollToAccount(Account account) {
        if (account == null) {
            account = scrollToAccount;
        }
        if (sp == null || newCP == null || account == null) {
            scrollToAccount = account;
            return;
        }
        Rectangle bounds = newCP.getAccountRectangle(account);
        if (bounds == null) {
            Dimension pref = newCP.getPreferredSize();
            bounds = new Rectangle(0, pref.height - 3, pref.width, 3);
        }
        newCP.scrollRectToVisible(bounds);
    }
}
