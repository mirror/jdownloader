package jd.plugins.optional.infobar;

import java.awt.event.ActionEvent;
import java.util.ArrayList;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.gui.swing.GuiRunnable;
import jd.gui.swing.jdgui.menu.MenuAction;
import jd.plugins.OptionalPlugin;
import jd.plugins.PluginOptional;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

import com.sun.awt.AWTUtilities;

@OptionalPlugin(rev = "$Revision$", id = "infobar", hasGui = true, interfaceversion = 5)
public class JDInfoBar extends PluginOptional {

    private static final String PROPERTY_OPACITY = "PROPERTY_OPACITY";

    private MenuAction activateAction;

    private InfoDialog infoDialog;

    public JDInfoBar(PluginWrapper wrapper) {
        super(wrapper);

        initConfigEntries();
    }

    private void initConfigEntries() {
        if (JDUtilities.getJavaVersion() >= 1.6) {
            ConfigEntry ce = new ConfigEntry(ConfigContainer.TYPE_SPINNER, getPluginConfig(), JDInfoBar.PROPERTY_OPACITY, JDL.L("jd.plugins.optional.infobar.JDInfoBar.opacity", "Opacity of the Dialog [in %]"), 1, 100) {
                private static final long serialVersionUID = 1L;

                @Override
                public void valueChanged(final Object newValue) {
                    updateOpacity(newValue);
                    super.valueChanged(newValue);
                }
            };
            ce.setDefaultValue(100);
            ce.setStep(10);
            config.addEntry(ce);
        }
    }

    private void updateOpacity(Object value) {
        final Float newValue;
        if (value == null) {
            newValue = getPluginConfig().getDoubleProperty(JDInfoBar.PROPERTY_OPACITY, 100.0).floatValue();
        } else {
            newValue = Float.parseFloat(value.toString());
        }
        new GuiRunnable<Object>() {

            @Override
            public Object runSave() {
                AWTUtilities.setWindowOpacity(infoDialog, newValue / 100);
                return null;
            }

        }.start();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == activateAction) {
            setGuiEnable(activateAction.isSelected());
        }
    }

    @Override
    public void setGuiEnable(boolean b) {
        if (b) {
            infoDialog.showDialog();
        } else {
            infoDialog.hideDialog();
        }
        if (activateAction != null && activateAction.isSelected() != b) activateAction.setSelected(b);
    }

    @Override
    public boolean initAddon() {
        activateAction = new MenuAction("infobar", 0);
        activateAction.setTitle(getHost());
        activateAction.setActionListener(this);
        activateAction.setIcon(this.getIconKey());
        activateAction.setSelected(false);

        infoDialog = InfoDialog.getInstance(activateAction);
        updateOpacity(null);

        return true;
    }

    @Override
    public void onExit() {
    }

    @Override
    public ArrayList<MenuAction> createMenuitems() {
        ArrayList<MenuAction> menu = new ArrayList<MenuAction>();

        menu.add(activateAction);

        return menu;
    }

    @Override
    public String getIconKey() {
        return "gui.images.help";
    }

}
