package jd.gui.swing.jdgui.views.settings.panels.pluginsettings;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;

import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import jd.gui.swing.jdgui.views.settings.components.SettingsComponent;
import jd.gui.swing.jdgui.views.settings.components.StateUpdateListener;
import net.miginfocom.swing.MigLayout;

import org.appwork.storage.config.JsonConfig;
import org.appwork.swing.MigPanel;
import org.appwork.swing.components.circlebar.CircledProgressBar;
import org.appwork.swing.components.circlebar.ImagePainter;
import org.appwork.utils.swing.EDTRunner;
import org.jdownloader.extensions.Header;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;
import org.jdownloader.plugins.controller.LazyPlugin;
import org.jdownloader.plugins.controller.crawler.CrawlerPluginController;
import org.jdownloader.plugins.controller.crawler.LazyCrawlerPlugin;
import org.jdownloader.plugins.controller.host.HostPluginController;
import org.jdownloader.plugins.controller.host.LazyHostPlugin;
import org.jdownloader.settings.GraphicalUserInterfaceSettings;

public class PluginSettingsPanel extends JPanel implements SettingsComponent, ActionListener {
    /**
     * 
     */
    private static final long   serialVersionUID = 1L;
    private JList               selector;
    private ImageIcon           decryterIcon;
    private MigPanel            card;
    protected PluginConfigPanel configPanel;
    protected List<Pattern>     filter;
    private Header              rightHeader;
    protected int               maxWidth         = -1;

    public void addStateUpdateListener(StateUpdateListener listener) {
        throw new IllegalStateException("Not implemented");
    }

    public Dimension getPreferredScrollableViewportSize() {

        return this.getPreferredSize();
    }

    public PluginSettingsPanel() {
        super(new MigLayout("ins 0,wrap 2", "[]10[grow,fill]", "[grow,fill][26!,grow,fill]"));
        decryterIcon = NewTheme.I().getIcon("linkgrabber", 16);

        selector = new JList() {
            private Dimension dim;
            {
                dim = new Dimension(200, 20000);
            }

            public Dimension getPreferredScrollableViewportSize() {
                if (maxWidth > 0) {
                    dim.width = maxWidth + 28;
                } else {
                    dim.width = selector.getPreferredSize().width + 28;
                }

                return dim;
            }

        };

        // selector.setLayoutOrientation(JList.HORIZONTAL_WRAP);
        final ListCellRenderer org = selector.getCellRenderer();
        selector.setCellRenderer(new ListCellRenderer() {

            public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                LazyPlugin<?> plg = (LazyPlugin<?>) value;

                if (plg instanceof LazyHostPlugin) {
                    JLabel ret = (JLabel) org.getListCellRendererComponent(list, plg.getDisplayName(), index, isSelected, cellHasFocus);
                    ImageIcon ic = ((LazyHostPlugin) plg).getIcon();
                    ret.setIcon(ic);
                    return ret;
                } else {
                    JLabel ret = (JLabel) org.getListCellRendererComponent(list, _GUI._.PluginSettingsPanel_getListCellRendererComponent_crawler_(plg.getDisplayName()), index, isSelected, cellHasFocus);
                    ret.setIcon(decryterIcon);
                    return ret;
                }

            }
        });
        selector.setFixedCellHeight(24);

        selector.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        setOpaque(false);

        final MigPanel left = new MigPanel("ins 0,wrap 1", "[grow,fill]", "[grow,fill]");
        left.setOpaque(false);
        left.setBackground(null);
        JScrollPane sp;
        left.add(sp = new JScrollPane(selector), "pushy,growy");
        final SearchField search = new SearchField() {

            protected void updaterFilter() {
                new EDTRunner() {

                    @Override
                    protected void runInEDT() {
                        filter = getFilterPatterns();
                        System.out.println(getFilterPatterns());
                        fill();
                        if (selector.getModel().getSize() > 0) {
                            selector.setSelectedIndex(0);

                        }
                    }
                };

            }
        };

        // left.setBorder(new JTextField().getBorder());
        // selector.setPreferredSize(new Dimension(200, 20000));
        // sp.setBorder(null);

        final MigPanel right = new MigPanel("ins 0,wrap 1", "[grow,fill]", "[][grow,fill]");
        right.setOpaque(false);
        right.setBackground(null);
        this.card = new MigPanel("ins 0", "[grow,fill]", "[grow,fill]");
        right.add(rightHeader = new Header("", null));

        right.add(card, "gapleft 26");

        card.setOpaque(false);
        selector.addListSelectionListener(new ListSelectionListener() {

            @Override
            public void valueChanged(ListSelectionEvent e) {
                actionPerformed(null);
            }
        });

        if (CrawlerPluginController.list(false) == null) {
            MigPanel loaderPanel = new MigPanel("ins 0,wrap 1", "[grow]", "50[][]");
            loaderPanel.setOpaque(false);
            loaderPanel.setBackground(null);

            CircledProgressBar loader = new CircledProgressBar();
            loader.setValueClipPainter(new ImagePainter(NewTheme.I().getImage("robot", 256), 1.0f));

            loader.setNonvalueClipPainter(new ImagePainter(NewTheme.I().getImage("robot", 256), 0.1f));
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
                        add(left);
                        add(right, "growx,pushx");
                        add(search, "spanx,pushx,growx");

                        if (selector.getModel().getSize() > 0) {
                            String active = JsonConfig.create(GraphicalUserInterfaceSettings.class).getActivePluginConfigPanel();
                            int selectIndex = 0;
                            if (active != null) {
                                for (int i = 0; i < selector.getModel().getSize(); i++) {
                                    if (((LazyPlugin<?>) selector.getModel().getElementAt(i)).getClassname().equals(active)) {
                                        selectIndex = i;
                                        break;
                                    }
                                }

                            }
                            selector.setSelectedIndex(selectIndex);
                            // show((LazyPlugin<?>) selector.getModel().getElementAt(selectIndex));
                        }
                    }
                };

            }
        }.start();

    }

    private void fill() {
        // java.util.List<LazyPlugin<?>> list = new ArrayList<LazyPlugin<?>>();
        DefaultListModel<LazyPlugin<?>> list = new DefaultListModel<LazyPlugin<?>>();
        boolean unfiltered = true;
        unfiltered = fillModel(list, true);
        if (list.size() == 0) {
            unfiltered = fillModel(list, false);
        }
        selector.setModel(list);
        if (unfiltered) maxWidth = selector.getPreferredSize().width;

    }

    public boolean fillModel(DefaultListModel<LazyPlugin<?>> list, boolean doFilter) {
        boolean unfiltered = true;
        ArrayList<LazyPlugin<?>> lst = new ArrayList<LazyPlugin<?>>();
        for (LazyHostPlugin plg : HostPluginController.getInstance().list()) {
            if (plg.isHasConfig()) {
                boolean match = false;
                if (filter != null && doFilter) {

                    for (Pattern p : filter) {
                        if (p.matcher(plg.getDisplayName()).find()) {
                            match = true;
                            break;
                        }
                    }
                } else {
                    match = true;
                }
                if (match) {
                    lst.add(plg);
                } else {
                    unfiltered = false;
                }
            }
        }
        for (LazyCrawlerPlugin plg : CrawlerPluginController.getInstance().list()) {
            if (plg.isHasConfig()) {
                boolean match = false;
                if (filter != null && doFilter) {

                    for (Pattern p : filter) {
                        if (p.matcher(plg.getDisplayName()).find()) {
                            match = true;
                            break;
                        }
                    }
                } else {
                    match = true;
                }
                if (match) {
                    lst.add(plg);
                } else {
                    unfiltered = false;
                }
            }
        }
        Collections.sort(lst, new Comparator<LazyPlugin<?>>() {

            @Override
            public int compare(LazyPlugin<?> o1, LazyPlugin<?> o2) {
                return o1.getDisplayName().compareToIgnoreCase(o2.getDisplayName());
            }
        });
        for (LazyPlugin<?> lp : lst)
            list.addElement(lp);
        return unfiltered;
    }

    public String getConstraints() {
        return "wmin 10,height 200:n:n,growy,pushy";
    }

    public boolean isMultiline() {
        return true;
    }

    public void actionPerformed(ActionEvent e) {
        LazyPlugin<?> selected = (LazyPlugin<?>) selector.getSelectedValue();

        JsonConfig.create(GraphicalUserInterfaceSettings.class).setActivePluginConfigPanel(selected.getClassname());

        show(selected);
    }

    private void show(final LazyPlugin<?> selectedItem) {
        new EDTRunner() {

            @Override
            protected void runInEDT() {
                card.removeAll();
                if (configPanel != null) {
                    configPanel.setHidden();
                }
                configPanel = PluginConfigPanel.create(selectedItem);
                if (configPanel != null) {
                    configPanel.setShown();
                    card.add(configPanel);

                    if (selectedItem != null) {

                        if (selectedItem instanceof LazyHostPlugin) {
                            rightHeader.setText(_GUI._.PluginSettingsPanel_runInEDT_plugin_header_text_host(selectedItem.getDisplayName()));
                            rightHeader.setIcon(((LazyHostPlugin) selectedItem).getIcon());
                        } else {
                            rightHeader.setText(_GUI._.PluginSettingsPanel_runInEDT_plugin_header_text_decrypt(selectedItem.getDisplayName()));
                            rightHeader.setIcon(decryterIcon);
                        }
                        rightHeader.setVisible(true);
                    } else {
                        rightHeader.setVisible(false);
                    }
                }
                revalidate();
            }
        };
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
