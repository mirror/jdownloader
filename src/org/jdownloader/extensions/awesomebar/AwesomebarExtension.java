package org.jdownloader.extensions.awesomebar;

import java.util.ArrayList;

import jd.config.ConfigContainer;
import jd.controlling.JDController;
import jd.event.ControlEvent;
import jd.event.ControlListener;
import jd.gui.swing.jdgui.actions.ActionController;
import jd.gui.swing.jdgui.actions.CustomToolbarAction;
import jd.gui.swing.jdgui.menu.MenuAction;
import jd.gui.swing.jdgui.views.settings.panels.gui.ToolbarController;
import jd.plugins.AddonPanel;
import jd.utils.locale.JDL;

import org.appwork.shutdown.ShutdownVetoException;
import org.jdownloader.extensions.AbstractExtension;
import org.jdownloader.extensions.ExtensionConfigPanel;
import org.jdownloader.extensions.StartException;
import org.jdownloader.extensions.StopException;
import org.jdownloader.extensions.awesomebar.awesome.Awesome;
import org.jdownloader.extensions.awesomebar.awesome.gui.AwesomeCustomToolbarAction;
import org.jdownloader.extensions.awesomebar.awesome.gui.AwesomeProposalPanel;
import org.jdownloader.extensions.awesomebar.awesome.gui.AwesomeToolbarPanel;

public class AwesomebarExtension extends AbstractExtension implements ControlListener {

    private CustomToolbarAction  toolbarAction;
    private AwesomeToolbarPanel  toolbarPanel;
    private AwesomeProposalPanel proposalPanel = null;
    private Awesome              awesome       = new Awesome();

    public Awesome getAwesome() {
        return awesome;
    }

    public ExtensionConfigPanel getConfigPanel() {
        return null;
    }

    public boolean hasConfigPanel() {
        return false;
    }

    public AwesomebarExtension() throws StartException {
        super(JDL.L("jd.plugins.optional.awesomebar.awesomebar", null));
        this.toolbarAction = new AwesomeCustomToolbarAction(this);
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

    public void onShutdown() {
    }

    public void onShutdownRequest() throws ShutdownVetoException {

    }

    public void onShutdownVeto(ArrayList<ShutdownVetoException> vetos) {
    }

    @Override
    protected void stop() throws StopException {
        JDController.getInstance().removeControlListener(this);
        ActionController.unRegister(toolbarAction);
        ToolbarController.setActions(ActionController.getActions());
    }

    @Override
    protected void start() throws StartException {
        JDController.getInstance().addControlListener(this);
    }

    @Override
    protected void initSettings(ConfigContainer config) {
    }

    @Override
    public String getConfigID() {
        return "addons.awesomebar";
    }

    @Override
    public String getAuthor() {
        return "Lorenzo van Matterhorn";
    }

    @Override
    public String getDescription() {
        return JDL.L("jd.plugins.optional.awesomebar.awesomebar.description", "");
    }

    @Override
    public ArrayList<MenuAction> getMenuAction() {
        return null;
    }

    public void controlEvent(ControlEvent event) {
        if (event.getEventID() == ControlEvent.CONTROL_INIT_COMPLETE) {
            ActionController.register(toolbarAction);
            ToolbarController.setActions(ActionController.getActions());
        }
    }

    @Override
    public AddonPanel getGUI() {
        return null;
    }

    @Override
    protected void initExtension() throws StartException {
    }

}