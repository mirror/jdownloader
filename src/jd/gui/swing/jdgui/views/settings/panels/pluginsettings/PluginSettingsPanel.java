package jd.gui.swing.jdgui.views.settings.panels.pluginsettings;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JPanel;

import jd.gui.swing.jdgui.views.settings.components.SettingsComponent;
import jd.gui.swing.jdgui.views.settings.components.StateUpdateListener;
import net.miginfocom.swing.MigLayout;

import org.appwork.swing.MigPanel;
import org.appwork.swing.components.searchcombo.SearchComboBox;
import org.appwork.utils.swing.EDTRunner;
import org.jdownloader.images.NewTheme;
import org.jdownloader.plugins.controller.LazyPlugin;
import org.jdownloader.plugins.controller.crawler.CrawlerPluginController;
import org.jdownloader.plugins.controller.crawler.LazyCrawlerPlugin;
import org.jdownloader.plugins.controller.host.HostPluginController;
import org.jdownloader.plugins.controller.host.LazyHostPlugin;

public class PluginSettingsPanel extends JPanel implements SettingsComponent, ActionListener {
    /**
     * 
     */
    private static final long             serialVersionUID = 1L;
    private SearchComboBox<LazyPlugin<?>> selector;
    private ImageIcon                     decryterIcon;
    private MigPanel                      card;
    protected PluginConfigPanel           configPanel;

    public void addStateUpdateListener(StateUpdateListener listener) {
        throw new IllegalStateException("Not implemented");
    }

    public PluginSettingsPanel() {
        super(new MigLayout("ins 0,wrap 1", "[grow,fill]", "[]15[grow,fill][]"));
        decryterIcon = NewTheme.I().getIcon("linkgrabber", 16);
        selector = new SearchComboBox<LazyPlugin<?>>() {

            /**
             * 
             */
            private static final long serialVersionUID = 1L;

            @Override
            protected Icon getIconForValue(LazyPlugin<?> value) {
                if (value == null) return null;
                if (value instanceof LazyHostPlugin) {
                    return (((LazyHostPlugin) value).getIcon());
                } else {
                    return (decryterIcon);
                }
            }

            @Override
            protected String getTextForValue(LazyPlugin<?> value) {
                if (value == null) return null;
                return value.getDisplayName();
            }

        };
        setOpaque(false);
        add(selector);
        this.card = new MigPanel("ins 0", "[grow,fill]", "[grow,fill]");
        add(card, "growx,pushx");
        card.setOpaque(false);
        selector.addActionListener(this);
        fill();
        if (selector.getItemCount() > 0) {
            show((LazyPlugin<?>) selector.getItemAt(0));
        }
    }

    private void fill() {
        java.util.List<LazyPlugin<?>> list = new ArrayList<LazyPlugin<?>>();
        if (list != null) {
            for (LazyHostPlugin plg : HostPluginController.getInstance().list()) {
                if (plg.isHasConfig()) {
                    list.add(plg);
                }
            }
            for (LazyCrawlerPlugin plg : CrawlerPluginController.getInstance().list()) {
                if (plg.isHasConfig()) {
                    list.add(plg);
                }
            }
            selector.setList(list);
        }
    }

    public String getConstraints() {
        return "wmin 10,height 200:n:n,growy,pushy";
    }

    public boolean isMultiline() {
        return true;
    }

    public void actionPerformed(ActionEvent e) {
        show((LazyPlugin<?>) selector.getSelectedItem());
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
