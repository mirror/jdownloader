package org.jdownloader.extensions.infobar;

import java.awt.event.ActionEvent;
import java.util.ArrayList;

import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.ConfigGroup;
import jd.gui.swing.GuiRunnable;
import jd.gui.swing.jdgui.menu.MenuAction;
import jd.plugins.AddonPanel;
import jd.utils.locale.JDL;

import org.appwork.utils.Application;
import org.jdownloader.extensions.AbstractExtension;
import org.jdownloader.extensions.ExtensionConfigPanel;
import org.jdownloader.extensions.StartException;
import org.jdownloader.extensions.StopException;

import com.sun.awt.AWTUtilities;

public class InfoBarExtension extends AbstractExtension {

    @Override
    public boolean isDefaultEnabled() {
        return true;
    }

    private static final String JDL_PREFIX            = "jd.plugins.optional.infobar.";

    private static final String PROPERTY_OPACITY      = "PROPERTY_OPACITY";

    private static final String PROPERTY_DROPLOCATION = "PROPERTY_DROPLOCATION";

    private static final String PROPERTY_DOCKING      = "PROPERTY_DOCKING";

    private MenuAction          activateAction;

    private InfoDialog          infoDialog;

    public ExtensionConfigPanel getConfigPanel() {
        return null;
    }

    public boolean hasConfigPanel() {
        return false;
    }

    public InfoBarExtension() throws StartException {
        super(JDL.L("jd.plugins.optional.infobar.jdinfobar", null));

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
    public String getIconKey() {
        return "gui.images.addons.infobar";
    }

    @Override
    protected void stop() throws StopException {
        if (infoDialog != null) infoDialog.hideDialog();
    }

    @Override
    protected void start() throws StartException {
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
    }

    @Override
    protected void initSettings(ConfigContainer config) {
        config.setGroup(new ConfigGroup(getName(), getIconKey()));
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

    @Override
    public String getConfigID() {
        return "infobar";
    }

    @Override
    public String getAuthor() {
        return null;
    }

    @Override
    public String getDescription() {
        return JDL.L("jd.plugins.optional.infobar.jdinfobar.description", null);
    }

    @Override
    public AddonPanel getGUI() {
        return null;
    }

    @Override
    public ArrayList<MenuAction> getMenuAction() {
        ArrayList<MenuAction> menu = new ArrayList<MenuAction>();

        menu.add(activateAction);

        return menu;
    }

    @Override
    protected void initExtension() throws StartException {
    }

}
