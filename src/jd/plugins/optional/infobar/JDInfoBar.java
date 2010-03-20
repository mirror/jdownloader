package jd.plugins.optional.infobar;

import java.awt.event.ActionEvent;
import java.util.ArrayList;

import jd.PluginWrapper;
import jd.gui.swing.jdgui.menu.MenuAction;
import jd.plugins.OptionalPlugin;
import jd.plugins.PluginOptional;

@OptionalPlugin(rev = "$Revision$", id = "infobar", hasGui = true, interfaceversion = 5)
public class JDInfoBar extends PluginOptional {

    private MenuAction activateAction;

    private InfoDialog infoDialog;

    public JDInfoBar(PluginWrapper wrapper) {
        super(wrapper);
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
