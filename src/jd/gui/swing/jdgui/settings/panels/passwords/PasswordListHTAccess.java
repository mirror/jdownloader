package jd.gui.swing.jdgui.settings.panels.passwords;

import javax.swing.JTabbedPane;

import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.Configuration;
import jd.controlling.HTACCESSController;
import jd.controlling.ListController;
import jd.gui.swing.jdgui.settings.ConfigPanel;
import jd.gui.swing.jdgui.settings.GUIConfigEntry;
import jd.utils.locale.JDL;

public class PasswordListHTAccess extends ConfigPanel {
    private static final String JDL_PREFIX = "jd.gui.swing.jdgui.settings.panels.passwords.PasswordListHTAccess.";

    public String getBreadcrum() {
        return JDL.L(this.getClass().getName() + ".breadcrum", this.getClass().getSimpleName());
    }

    public static String getTitle() {
        return JDL.L(JDL_PREFIX + "general.title", "HTAccess logins");
    }

    private static final long serialVersionUID = 3383448498625377495L;

    public PasswordListHTAccess(Configuration configuration) {
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
        // new ConfigEntry(ConfigContainer.TYPE_LISTCONTROLLED, (ListController)
        // HTACCESSController.getInstance(), JDL.L("plugins.http.htaccess",
        // "List of all HTAccess passwords. Each line one password."))

        addGUIConfigEntry(new GUIConfigEntry(new ConfigEntry(ConfigContainer.TYPE_LISTCONTROLLED, (ListController) HTACCESSController.getInstance(), JDL.L("plugins.http.htaccess", "List of all HTAccess passwords. Each line one password."))));
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
