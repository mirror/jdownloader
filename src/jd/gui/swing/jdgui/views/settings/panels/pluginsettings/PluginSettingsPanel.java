package jd.gui.swing.jdgui.views.settings.panels.pluginsettings;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JPanel;

import jd.DecryptPluginWrapper;
import jd.HostPluginWrapper;
import jd.PluginWrapper;
import jd.gui.swing.jdgui.views.settings.components.SettingsComponent;
import net.miginfocom.swing.MigLayout;

import org.appwork.app.gui.MigPanel;
import org.appwork.swing.components.searchcombo.SearchComboBox;
import org.appwork.utils.swing.EDTRunner;
import org.jdownloader.images.Theme;

public class PluginSettingsPanel extends JPanel implements SettingsComponent, ActionListener {
    /**
     * 
     */
    private static final long             serialVersionUID = 1L;
    private SearchComboBox<PluginWrapper> selector;
    private ImageIcon                     decryterIcon;
    private MigPanel                      card;
    protected PluginConfigPanel           configPanel;

    public PluginSettingsPanel() {
        super(new MigLayout("ins 0,wrap 1", "[][]", "[]15[grow,fill][]"));
        decryterIcon = Theme.getIcon("spider", 20);
        selector = new SearchComboBox<PluginWrapper>() {

            /**
             * 
             */
            private static final long serialVersionUID = 1L;

            @Override
            protected Icon getIcon(PluginWrapper value) {
                if (value == null) return null;
                if (value instanceof HostPluginWrapper) {
                    return (((HostPluginWrapper) value).getIcon());
                } else {
                    return (decryterIcon);
                }
            }

            @Override
            protected String getText(PluginWrapper value) {
                if (value == null) return null;
                return value.getHost();
            }

        };

        add(selector);
        this.card = new MigPanel("ins 0", "[grow,fill]", "[grow,fill]");
        add(card, "spanx,growx,pushx");
        selector.addActionListener(this);
        fill();
    }

    private void fill() {
        ArrayList<PluginWrapper> list = new ArrayList<PluginWrapper>();
        for (HostPluginWrapper plg : HostPluginWrapper.getHostWrapper()) {
            if (plg.hasConfig()) {
                list.add(plg);
            }
        }
        for (DecryptPluginWrapper plg : DecryptPluginWrapper.getDecryptWrapper()) {
            if (plg.hasConfig()) {
                list.add(plg);
            }
        }
        Collections.sort(list, new Comparator<PluginWrapper>() {

            public int compare(PluginWrapper o1, PluginWrapper o2) {
                return o1.getHost().compareTo(o2.getHost());
            }
        });
        selector.setList(list);

    }

    public String getConstraints() {
        return "wmin 10,height 200:n:n,growy,pushy";
    }

    public boolean isMultiline() {
        return true;
    }

    public void actionPerformed(ActionEvent e) {
        show((PluginWrapper) selector.getSelectedItem());
    }

    private void show(final PluginWrapper selectedItem) {

        new EDTRunner() {

            @Override
            protected void runInEDT() {

                card.removeAll();
                if (configPanel != null) {
                    configPanel.setHidden();
                }
                configPanel = PluginConfigPanel.create(selectedItem);
                configPanel.setShown();
                card.add(configPanel);
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
