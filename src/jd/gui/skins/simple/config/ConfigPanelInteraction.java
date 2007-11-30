package jd.gui.skins.simple.config;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Vector;
import java.util.logging.Logger;

import jd.config.ConfigEntry;
import jd.controlling.interaction.Interaction;
import jd.gui.UIInterface;
import jd.utils.JDUtilities;

/**
 * Diese Klasse kann die ConfigContainer instanz einer Interaction verwenden um
 * den gewünschten Config Dialog anzuzeigen
 * 
 * @author coalado
 * 
 */
public class ConfigPanelInteraction extends ConfigPanel implements ActionListener {

    /**
     * 
     */
    private static final long serialVersionUID = -6647985066225177059L;

    /**
     * serialVersionUID
     */
    @SuppressWarnings("unused")
    private Logger            logger           = JDUtilities.getLogger();

    protected Interaction       interaction;

    public ConfigPanelInteraction(UIInterface uiinterface, Interaction interaction) {
        super(uiinterface);
        this.interaction = interaction;
        initPanel();

        load();
    }

    public void save() {
        this.saveConfigEntries();
    }

    public void actionPerformed(ActionEvent e) {

    }

    @Override
    public void initPanel() {

        Vector<ConfigEntry> entries = interaction.getConfig().getEntries();
       
        ConfigEntry entry;
        for (int i = 0; i < entries.size(); i++) {
            entry = entries.elementAt(i);

            GUIConfigEntry ce = null;
            ce = new GUIConfigEntry(entry);

            if (ce != null) addGUIConfigEntry(ce);

            
        }
        add(panel, BorderLayout.CENTER);

    }

    @Override
    public String getName() {
if(interaction==null){return "no Action";}
        return "Interaction Konfiguration: "+interaction.getInteractionName();
    }

    @Override
    public void load() {
        loadConfigEntries();

    }

}
