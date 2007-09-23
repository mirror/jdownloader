package jd.gui.skins.simple.config;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.Configuration;
import jd.controlling.interaction.DummyInteraction;
import jd.controlling.interaction.Interaction;
import jd.gui.UIInterface;

public class ConfigPanelInteractionDummy extends ConfigPanel implements ActionListener {
    /**
     * 
     */
    private static final long serialVersionUID = -7983057329558110899L;

    /**
     * serialVersionUID
     */
    private Interaction       interaction;

    ConfigPanelInteractionDummy(Configuration configuration, UIInterface uiinterface, DummyInteraction interaction) {
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
        GUIConfigEntry ce;

        ce = new GUIConfigEntry(new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, interaction, DummyInteraction.PROPERTY_QUESTION, "Frage"));

        addGUIConfigEntry(ce);

        add(panel, BorderLayout.CENTER);

    }

    @Override
    public String getName() {

        return "Dummy Konfiguration";
    }

    @Override
    public void load() {
        loadConfigEntries();

    }

}
