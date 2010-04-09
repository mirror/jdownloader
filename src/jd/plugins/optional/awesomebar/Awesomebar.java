package jd.plugins.optional.awesomebar;

import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.JDController;
import jd.event.ControlEvent;
import jd.event.ControlListener;
import jd.gui.swing.jdgui.actions.ActionController;
import jd.gui.swing.jdgui.actions.CustomToolbarAction;
import jd.gui.swing.jdgui.menu.MenuAction;
import jd.gui.swing.jdgui.views.settings.panels.gui.ToolbarController;
import jd.plugins.OptionalPlugin;
import jd.plugins.PluginOptional;
import jd.plugins.optional.awesomebar.awesome.Awesome;
import jd.plugins.optional.awesomebar.awesome.gui.AwesomeCustomToolbarAction;
import jd.plugins.optional.awesomebar.awesome.gui.AwesomeProposalPanel;
import jd.plugins.optional.awesomebar.awesome.gui.AwesomeToolbarPanel;

@OptionalPlugin(rev = "$Revision: 10379 $", id = "addons.awesomebar", hasGui = true, interfaceversion = 5)
public class Awesomebar extends PluginOptional implements ControlListener {

    private CustomToolbarAction toolbarAction;
    private AwesomeToolbarPanel toolbarPanel;
    private AwesomeProposalPanel proposalPanel = null;
    private Awesome awesome = new Awesome();

    public Awesome getAwesome() {
        return awesome;
    }

    public Awesomebar(PluginWrapper wrapper) {
        super(wrapper);
        this.toolbarAction = new AwesomeCustomToolbarAction(this);
        /* Workaround for toolbar */
        JDController.getInstance().addControlListener(new ControlListener() {
            public void controlEvent(ControlEvent event) {
                if (event.getID() == ControlEvent.CONTROL_INIT_COMPLETE) {
                    initAddon();
                }
            }
        });
    }

    @Override
    public boolean initAddon() {
        setGuiEnable(true);
        return false;
    }

    @Override
    public void onExit() {

    }

    @Override
    public ArrayList<MenuAction> createMenuitems() {
        return null;
    }

    public AwesomeToolbarPanel getToolbarPanel() {
        if (toolbarPanel == null) {
            toolbarPanel = new AwesomeToolbarPanel(this);
        }
        return toolbarPanel;
    }

    public AwesomeProposalPanel getProposalPanel() {
        if (proposalPanel == null) {
            proposalPanel = new AwesomeProposalPanel(this);
        }
        return proposalPanel;
    }

    @Override
    public void setGuiEnable(boolean b) {
        System.out.println("setGuiEnable");
        if (b) {
            ActionController.register(toolbarAction);
            ToolbarController.setActions(ActionController.getActions());

        } else {
            ActionController.unRegister(toolbarAction);
            ToolbarController.setActions(ActionController.getActions());
        }
    }

}