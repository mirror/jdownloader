package jd.gui.skins.simple.config;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Iterator;
import java.util.Vector;
import java.util.logging.Logger;

import javax.swing.JPanel;

import jd.config.SubConfiguration;
import jd.gui.UIInterface;
import jd.gui.skins.simple.SimpleGUI;
import jd.utils.JDUtilities;

public abstract class ConfigPanel extends JPanel {
    /**
     * serialVersionUID
     */
    private static final long      serialVersionUID = 3383448498625377495L;

    private Vector<GUIConfigEntry> entries          = new Vector<GUIConfigEntry>();

    protected UIInterface          uiinterface;

    protected JPanel               panel;

    protected Logger               logger           = JDUtilities.getLogger();

    protected Insets               insets           = new Insets(1, 5, 1, 5);

    ConfigPanel(UIInterface uiinterface) {

        this.setLayout(new BorderLayout());
        panel = new JPanel(new GridBagLayout());
        this.uiinterface = uiinterface;

    }

    public void addGUIConfigEntry(GUIConfigEntry entry) {
        if (!entry.isExpertEntry() || JDUtilities.getSubConfig(SimpleGUI.GUICONFIGNAME).getBooleanProperty(SimpleGUI.PARAM_USE_EXPERT_VIEW, false)) {
            JDUtilities.addToGridBag(panel, entry, GridBagConstraints.RELATIVE, GridBagConstraints.RELATIVE, GridBagConstraints.REMAINDER, 1, 1, 0, insets, GridBagConstraints.BOTH, GridBagConstraints.EAST);
            entries.add(entry);
        }

    }

    public void saveConfigEntries() {
        Iterator<GUIConfigEntry> it = entries.iterator();
        Vector<SubConfiguration> subs= new   Vector<SubConfiguration>();
        while (it.hasNext()) {
            GUIConfigEntry akt = it.next();
            if(akt.getConfigEntry().getPropertyInstance() instanceof SubConfiguration && subs.indexOf(akt.getConfigEntry().getPropertyInstance())<0){
                subs.add((SubConfiguration)akt.getConfigEntry().getPropertyInstance());
                
            }
            // logger.info("entries: "+entries.size()+" :
            // "+akt.getConfigEntry().getPropertyInstance());
            if (akt.getConfigEntry().getPropertyInstance() != null && akt.getConfigEntry().getPropertyName() != null) akt.getConfigEntry().getPropertyInstance().setProperty(akt.getConfigEntry().getPropertyName(), akt.getText());

        }
        Iterator<SubConfiguration> it2 = subs.iterator();
        while(it2.hasNext())it2.next().save();
    }

    public void loadConfigEntries() {
        Iterator<GUIConfigEntry> it = entries.iterator();

        while (it.hasNext()) {
            GUIConfigEntry akt = it.next();

            if (akt.getConfigEntry().getPropertyInstance() != null && akt.getConfigEntry().getPropertyName() != null) akt.setData(akt.getConfigEntry().getPropertyInstance().getProperty(akt.getConfigEntry().getPropertyName()));

        }
    }

    public abstract void initPanel();

    public abstract void save();

    public abstract void load();

    public abstract String getName();
}
