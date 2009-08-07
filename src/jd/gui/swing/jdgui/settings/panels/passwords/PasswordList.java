package jd.gui.swing.jdgui.settings.panels.passwords;

import javax.swing.JTabbedPane;

import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.Configuration;
import jd.controlling.ListController;
import jd.controlling.PasswordListController;
import jd.gui.swing.jdgui.settings.ConfigPanel;
import jd.gui.swing.jdgui.settings.GUIConfigEntry;
import jd.utils.locale.JDL;

public class PasswordList extends ConfigPanel {
    private static final String JDL_PREFIX = "jd.gui.swing.jdgui.settings.panels.PasswordList.";

    public String getBreadcrum() {
        return JDL.L(this.getClass().getName() + ".breadcrum", this.getClass().getSimpleName());
    }

    public static String getTitle() {
        return JDL.L(JDL_PREFIX + "general.title", "Archive passwords");
    }

    private static final long serialVersionUID = 3383448498625377495L;

    public PasswordList(Configuration configuration) {
        super();

        initPanel();
        // load();
    }

    @Override
    public void initPanel() {
        // ConfigEntry conditionEntry;

        // this.passwordConfig = new
        // ConfigContainer(JDL.L("plugins.optional.jdunrar.config.passwordtab",
        // "List of passwords"));
        // config.addEntry(new ConfigEntry(ConfigContainer.TYPE_CONTAINER,
        // passwordConfig));
        //
        // passwordConfig.addEntry();
        //
        //   

        addGUIConfigEntry(new GUIConfigEntry(new ConfigEntry(ConfigContainer.TYPE_LISTCONTROLLED, (ListController) PasswordListController.getInstance(), JDL.LF("plugins.optional.jdunrar.config.passwordlist2", "List of all passwords. Each line one password. Available passwords: %s", ""))));
        // addGUIConfigEntry(new GUIConfigEntry(new
        // ConfigEntry(ConfigContainer.TYPE_CHECKBOX, configuration,
        // Configuration.LOGGER_FILELOG,
        // JDLocale.L("gui.config.general.filelogger",
        // "Erstelle Logdatei im ./logs/ Ordner")).setDefaultValue(false).setGroup(logging)));

        JTabbedPane tabbed = new JTabbedPane();

        tabbed.setOpaque(false);
        tabbed.add(getBreadcrum(), panel);

        this.add(tabbed);
    }

    @Override
    public void load() {
        loadConfigEntries();
    }

    @Override
    public void save() {
        saveConfigEntries();

    }

}
