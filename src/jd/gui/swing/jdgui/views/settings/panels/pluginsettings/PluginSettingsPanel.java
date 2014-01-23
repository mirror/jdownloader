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
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.Scrollable;
import javax.swing.SwingUtilities;

import jd.gui.swing.jdgui.JDGui;
import jd.gui.swing.jdgui.interfaces.SwitchPanel;
import jd.gui.swing.jdgui.views.settings.components.SettingsComponent;
import jd.gui.swing.jdgui.views.settings.components.StateUpdateListener;
import jd.gui.swing.jdgui.views.settings.sidebar.AddonConfig;
import jd.plugins.Plugin;
import jd.plugins.PluginConfigPanelNG;
import jd.plugins.PluginForHost;
import net.miginfocom.swing.MigLayout;

import org.appwork.storage.config.JsonConfig;
import org.appwork.swing.MigPanel;
import org.appwork.swing.components.ExtButton;
import org.appwork.swing.components.circlebar.CircledProgressBar;
import org.appwork.swing.components.circlebar.ImagePainter;
import org.appwork.swing.components.searchcombo.SearchComboBox;
import org.appwork.utils.logging.Log;
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
import org.jdownloader.images.NewTheme;
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

    private ImageIcon                     decryterIcon;
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
        decryterIcon = NewTheme.I().getIcon("linkgrabber", 16);
        logger = LogController.getInstance().getLogger(PluginSettingsPanel.class.getName());
        searchCombobox = new SearchComboBox<LazyPlugin<?>>(null) {

            @Override
            protected Icon getIconForValue(LazyPlugin<?> value) {
                if (value == null) return null;
                return DomainInfo.getInstance(value.getDisplayName()).getFavIcon();
            }

            @Override
            public void onChanged() {
                super.onChanged();

            }

            @Override
            protected String getTextForValue(LazyPlugin<?> value) {
                if (value == null) return "";
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
                setTooltipText(_GUI._.PluginSettingsPanel_PluginSettingsPanel_reset());
            }

            @Override
            public void actionPerformed(ActionEvent e) {
                if (configPanel != null && configPanel.isShown()) {
                    Plugin proto = null;
                    try {
                        if (currentItem != null) {
                            Dialog.getInstance().showConfirmDialog(0, _GUI._.lit_are_you_sure(), _GUI._.PluginSettingsPanel_are_you_sure(currentItem.getDisplayName()));
                            proto = currentItem.getPrototype(null);
                            PluginConfigPanelNG ccp = proto.createConfigPanel();
                            if (ccp != null) {
                                ccp.reset();

                            } else {
                                proto.getPluginConfig().reset();
                                AddonConfig.getInstance(proto.getConfig(), "", false).reload();

                            }
                            // avoid that the panel saves it's data on hide;
                            configPanel = null;

                            show(currentItem);
                            Dialog.getInstance().showMessageDialog(_GUI._.PluginSettingsPanel_actionPerformed_reset_done(currentItem.getDisplayName()));
                        }
                    } catch (UpdateRequiredClassNotFoundException e1) {
                        Log.exception(e1);
                    } catch (DialogClosedException e1) {
                        e1.printStackTrace();
                    } catch (DialogCanceledException e1) {
                        e1.printStackTrace();
                    }
                }
            }
        });
        this.card = new MigPanel("ins 3", "[grow,fill]", "[grow,fill]");
        header = new Header("", null);

        card.setOpaque(false);

        if (CrawlerPluginController.list(false) == null) {
            MigPanel loaderPanel = new MigPanel("ins 0,wrap 1", "[grow]", "50[][]");
            loaderPanel.setOpaque(false);
            loaderPanel.setBackground(null);

            CircledProgressBar loader = new CircledProgressBar();
            loader.setValueClipPainter(new ImagePainter(NewTheme.I().getIcon("robot", 256), 1.0f));

            loader.setNonvalueClipPainter(new ImagePainter(NewTheme.I().getIcon("robot", 256), 0.1f));
            ((ImagePainter) loader.getValueClipPainter()).setBackground(null);
            ((ImagePainter) loader.getValueClipPainter()).setForeground(null);
            loader.setIndeterminate(true);

            loaderPanel.add(loader, "width 256!,height 256!,alignx center");
            loaderPanel.add(new JLabel(_GUI._.PluginSettingsPanel_PluginSettingsPanel_waittext_()), "alignx center");

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
                        add(SwingUtils.toBold(new JLabel(_GUI._.PluginSettingsPanel_runInEDT_choose_())), "split 3,shrinkx");
                        add(searchCombobox, "pushx,growx,height 22!");
                        add(resetButton, "width 22!,height 22!");
                        add(header, "growx,pushx,gaptop 10");
                        add(card, "spanx,pushx,growx");

                        if (searchCombobox.getModel().getSize() > 0) {
                            String active = JsonConfig.create(GraphicalUserInterfaceSettings.class).getActivePluginConfigPanel();
                            int selectIndex = 0;
                            if (active != null) {
                                for (int i = 0; i < searchCombobox.getModel().getSize(); i++) {
                                    if (((LazyPlugin<?>) searchCombobox.getModel().getElementAt(i)).getClassname().equals(active)) {
                                        selectIndex = i;
                                        break;
                                    }
                                }

                            }
                            searchCombobox.setSelectedIndex(selectIndex);
                            // show((LazyPlugin<?>) selector.getModel().getElementAt(selectIndex));
                        }
                    }
                };

            }
        }.start();

    }

    public void setPlugin(final Class<? extends PluginForHost> class1) {
        new EDTRunner() {

            @Override
            protected void runInEDT() {
                if (searchCombobox.getModel().getSize() > 0) {
                    String active = class1.getName();
                    int selectIndex = 0;
                    if (active != null) {
                        for (int i = 0; i < searchCombobox.getModel().getSize(); i++) {
                            if (((LazyPlugin<?>) searchCombobox.getModel().getElementAt(i)).getClassname().equals(active)) {
                                selectIndex = i;
                                break;
                            }
                        }

                    }
                    searchCombobox.setSelectedIndex(selectIndex);
                    // show((LazyPlugin<?>) selector.getModel().getElementAt(selectIndex));
                } else {
                    JsonConfig.create(GraphicalUserInterfaceSettings.class).setActivePluginConfigPanel(class1.getName());
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
            if (plg.isHasConfig()) {

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

        return lst;
    }

    public String getConstraints() {
        return "wmin 10,height 200:n:n,growy,pushy";
    }

    public boolean isMultiline() {
        return true;
    }

    public void actionPerformed(ActionEvent e) {
        LazyPlugin<?> selected = (LazyPlugin<?>) searchCombobox.getSelectedItem();

        if (selected != null) {
            JsonConfig.create(GraphicalUserInterfaceSettings.class).setActivePluginConfigPanel(selected.getClassname());

            show(selected);
        }
    }

    private LazyPlugin<?> currentItem = null;

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

                PluginConfigPanelNG newCP;
                try {
                    JDGui.getInstance().setWaiting(true);
                    newCP = selectedItem.getPrototype(null).createConfigPanel();

                    if (newCP != null) {
                        configPanel = scrollerWrapper(newCP);
                    } else {
                        configPanel = PluginConfigPanel.create(selectedItem);
                    }
                    if (configPanel != null) {
                        configPanel.setShown();
                        card.add(configPanel);

                        if (selectedItem != null) {

                            if (selectedItem instanceof LazyHostPlugin) {
                                header.setText(_GUI._.PluginSettingsPanel_runInEDT_plugin_header_text_host(selectedItem.getDisplayName()));
                                header.setIcon(DomainInfo.getInstance(((LazyHostPlugin) selectedItem).getHost()).getFavIcon());
                            } else {
                                header.setText(_GUI._.PluginSettingsPanel_runInEDT_plugin_header_text_decrypt(selectedItem.getDisplayName()));
                                header.setIcon(decryterIcon);
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
                System.out.println("Finished 1: " + (System.currentTimeMillis() - start));
                SwingUtilities.invokeLater(new Runnable() {

                    @Override
                    public void run() {
                        System.out.println("Finished 2: " + (System.currentTimeMillis() - start));
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
        JScrollPane sp;
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
        }
    }

    public void setShown() {
        if (configPanel != null) {
            configPanel.setShown();
        }
    }

}
