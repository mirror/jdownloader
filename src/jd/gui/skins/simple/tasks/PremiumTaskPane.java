package jd.gui.skins.simple.tasks;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JSeparator;

import jd.HostPluginWrapper;
import jd.event.ControlEvent;
import jd.event.ControlListener;
import jd.plugins.PluginForHost;
import jd.utils.JDLocale;
import jd.utils.JDTheme;
import jd.utils.JDUtilities;

public class PremiumTaskPane extends TaskPanel implements ControlListener, ActionListener {
    private static final long serialVersionUID = -373653036070545536L;

    ArrayList<JButton> hoster = new ArrayList<JButton>();

    public PremiumTaskPane(String string, ImageIcon ii) {
        super(string, ii, "premium");
        JDUtilities.getController().addControlListener(this);
        initGUI();
    }

    private void initGUI() {
        for (HostPluginWrapper wrapper : JDUtilities.getPluginsForHost()) {
            if (wrapper.isLoaded() && wrapper.usePlugin()) {
                final PluginForHost helpPlugin = wrapper.getPlugin();
                if (helpPlugin.createMenuitems() != null && helpPlugin.getPremiumAccounts().size() > 0) {
                    JButton bt = this.createButton(helpPlugin.getHost(), null);
                    hoster.add(bt);
                    add(bt, D1_BUTTON_ICON);
                }
            }

        }
        add(new JSeparator());

        add(this.createButton(JDLocale.L("gui.task.premium.add", "Add Premiumacccount"), JDTheme.II("gui.images.add", 16, 16)), D1_BUTTON_ICON);

    }

    public void controlEvent(ControlEvent event) {
    }

    public void actionPerformed(ActionEvent e) {
        this.broadcastEvent(e);
    }
}
