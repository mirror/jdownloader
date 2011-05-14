package jd.gui.swing.jdgui.views.settings.panels.pluginsettings;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import javax.swing.DefaultComboBoxModel;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JComboBox;
import javax.swing.JPanel;

import jd.DecryptPluginWrapper;
import jd.HostPluginWrapper;
import jd.PluginWrapper;
import jd.gui.swing.jdgui.views.settings.components.SettingsComponent;
import net.miginfocom.swing.MigLayout;

import org.appwork.swing.components.searchcombo.SearchComboBox;
import org.jdownloader.images.Theme;

public class PluginSettingsPanel extends JPanel implements SettingsComponent {
    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    private JComboBox         selector;
    private ImageIcon         decryterIcon;

    public PluginSettingsPanel() {
        super(new MigLayout("ins 0,wrap 1", "[grow,fill]", "[][grow,fill][]"));
        decryterIcon = Theme.getIcon("spider", 20);
        selector = new SearchComboBox<PluginWrapper>() {

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
        selector.setModel(new DefaultComboBoxModel(list.toArray(new PluginWrapper[] {})));
    }

    public String getConstraints() {
        return "wmin 10,height 60:n:n,growy,pushy";
    }

    public boolean isMultiline() {
        return false;
    }

}
