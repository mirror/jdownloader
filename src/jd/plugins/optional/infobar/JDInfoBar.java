package jd.plugins.optional.infobar;

import java.awt.event.ActionEvent;
import java.util.ArrayList;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.ConfigGroup;
import jd.gui.swing.GuiRunnable;
import jd.gui.swing.jdgui.menu.MenuAction;
import jd.plugins.OptionalPlugin;
import jd.plugins.PluginOptional;
import jd.utils.locale.JDL;

import org.appwork.utils.Application;

import com.sun.awt.AWTUtilities;

@OptionalPlugin(rev = "$Revision$", defaultEnabled = true, id = "infobar", hasGui = true, interfaceversion = 7)
public class JDInfoBar extends PluginOptional {

    private static final String JDL_PREFIX            = "jd.plugins.optional.infobar.";

    private static final String PROPERTY_OPACITY      = "PROPERTY_OPACITY";

    private static final String PROPERTY_DROPLOCATION = "PROPERTY_DROPLOCATION";

    private static final String PROPERTY_DOCKING      = "PROPERTY_DOCKING";

    private MenuAction          activateAction;

    private InfoDialog          infoDialog;

    public JDInfoBar(PluginWrapper wrapper) {
        super(wrapper);

        initConfigEntries();
    }

    private void initConfigEntries() {
        config.setGroup(new ConfigGroup(getHost(), getIconKey()));
        if (Application.getJavaVersion() >= 16000000) {
            ConfigEntry ce = new ConfigEntry(ConfigContainer.TYPE_SPINNER, getPluginConfig(), PROPERTY_OPACITY, JDL.L(JDL_PREFIX + "opacity", "Opacity of the Dialog [in %]"), 1, 100, 10) {
                private static final long serialVersionUID = 1L;

                @Override
                public void valueChanged(Object newValue) {
                    updateOpacity(newValue);
                    super.valueChanged(newValue);
                }
            };
            ce.setDefaultValue(100);
            config.addEntry(ce);
            config.addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        }
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), PROPERTY_DROPLOCATION, JDL.L(JDL_PREFIX + "dropLocation2", "Enable Drop Location")) {

            private static final long serialVersionUID = 1L;

            @Override
            public void valueChanged(Object newValue) {
                if (infoDialog != null) infoDialog.setEnableDropLocation((Boolean) newValue);
                super.valueChanged(newValue);
            }
        }.setDefaultValue(true));
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), PROPERTY_DOCKING, JDL.L(JDL_PREFIX + "docking", "Enable Docking to the Sides")) {

            private static final long serialVersionUID = 1L;

            @Override
            public void valueChanged(Object newValue) {
                if (infoDialog != null) infoDialog.setEnableDocking((Boolean) newValue);
                super.valueChanged(newValue);
            }
        }.setDefaultValue(true));
    }

    private void updateOpacity(Object value) {
        if (infoDialog == null) return;
        final int newValue;
        if (value == null) {
            newValue = getPluginConfig().getIntegerProperty(PROPERTY_OPACITY, 100);
        } else {
            newValue = Integer.parseInt(value.toString());
        }
        new GuiRunnable<Object>() {

            @Override
            public Object runSave() {
                AWTUtilities.setWindowOpacity(infoDialog, (float) (newValue / 100.0));
                return null;
            }

        }.start();
    }

    @Override
    public void setGuiEnable(boolean b) {
        if (b) {
            if (infoDialog == null) {
                infoDialog = InfoDialog.getInstance(activateAction);
                infoDialog.setEnableDropLocation(getPluginConfig().getBooleanProperty(PROPERTY_DROPLOCATION, true));
                infoDialog.setEnableDocking(getPluginConfig().getBooleanProperty(PROPERTY_DOCKING, true));
                if (Application.getJavaVersion() >= 16000000) updateOpacity(null);
            }
            infoDialog.showDialog();
        } else {
            if (infoDialog != null) infoDialog.hideDialog();
        }
        if (activateAction != null && activateAction.isSelected() != b) activateAction.setSelected(b);
    }

    @Override
    public boolean initAddon() {
        activateAction = new MenuAction("infobar", 0) {
            private static final long serialVersionUID = 3252473048646596851L;

            @Override
            public void onAction(ActionEvent e) {
                setGuiEnable(activateAction.isSelected());
            }
        };
        activateAction.setIcon(this.getIconKey());
        activateAction.setSelected(false);

        logger.info("InfoBar: OK");
        return true;
    }

    @Override
    public void onExit() {
        if (infoDialog != null) infoDialog.hideDialog();
    }

    @Override
    public ArrayList<MenuAction> createMenuitems() {
        ArrayList<MenuAction> menu = new ArrayList<MenuAction>();

        menu.add(activateAction);

        return menu;
    }

    @Override
    public String getIconKey() {
        return "gui.images.addons.infobar";
    }

}
