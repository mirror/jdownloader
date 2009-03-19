package jd.gui.skins.simple.tasks;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JSeparator;

import net.miginfocom.swing.MigLayout;

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
        this.setLayout(new MigLayout("ins 0, wrap 1", "[fill]"));
        JDUtilities.getController().addControlListener(this);
        initGUI();
    }

    private void initGUI() {
        for (HostPluginWrapper wrapper : JDUtilities.getPluginsForHost()) {
            if (wrapper.isLoaded() && wrapper.usePlugin()) {
                final PluginForHost helpPlugin = wrapper.getPlugin();
                if (helpPlugin.createMenuitems() != null && helpPlugin.getPremiumAccounts().size() > 0) {
                    hoster.add(addButton(this.createButton(helpPlugin.getHost(), null)));
                }
            }

        }
        add(new JSeparator());

        addButton(this.createButton(JDLocale.L("gui.task.premium.add", "Add Premiumacccount"), JDTheme.II("gui.images.add", 16, 16)));
     
    }
    
    private JButton addButton(JButton bt) {
        bt.addActionListener(this);
        bt.setHorizontalAlignment(JButton.LEFT);
        add(bt, "alignx leading");
        return bt;
    }

	@Override
	public void controlEvent(ControlEvent event) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		this.broadcastEvent(e);
	}
}
