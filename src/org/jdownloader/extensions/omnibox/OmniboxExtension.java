package org.jdownloader.extensions.omnibox;

import javax.swing.JMenuItem;

import jd.plugins.AddonPanel;
import jd.utils.locale.JDL;

import org.jdownloader.extensions.AbstractExtension;
import org.jdownloader.extensions.ExtensionConfigPanel;
import org.jdownloader.extensions.StartException;
import org.jdownloader.extensions.StopException;
import org.jdownloader.extensions.omnibox.omni.Omni;
import org.jdownloader.extensions.omnibox.omni.gui.AwesomeProposalPanel;
import org.jdownloader.extensions.omnibox.omni.gui.AwesomeToolbarPanel;

public class OmniboxExtension extends AbstractExtension<OmniboxConfig> {

    private AwesomeToolbarPanel  toolbarPanel;
    private AwesomeProposalPanel proposalPanel = null;
    private Omni                 omni          = new Omni();

    public Omni getAwesome() {
        return omni;
    }

    public ExtensionConfigPanel<OmniboxExtension> getConfigPanel() {
        return null;
    }

    public boolean hasConfigPanel() {
        return false;
    }

    public OmniboxExtension() throws StartException {
        super(JDL.L("jd.plugins.optional.awesomebar.awesomebar", null));

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
    protected void stop() throws StopException {

    }

    @Override
    protected void start() throws StartException {

    }

    /*
     * @Override protected void initSettings(ConfigContainer config) { }
     */

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
    public java.util.ArrayList<JMenuItem> getMenuAction() {
        return null;
    }

    @Override
    public AddonPanel<AbstractExtension<OmniboxConfig>> getGUI() {
        return null;
    }

    @Override
    protected void initExtension() throws StartException {
    }

}