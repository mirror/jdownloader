package jd.gui.swing.jdgui.components.toolbar;

import java.util.ArrayList;
import java.util.HashMap;

import jd.gui.swing.jdgui.components.toolbar.actions.AbstractToolbarAction;
import jd.gui.swing.jdgui.components.toolbar.actions.AutoReconnectToggleAction;
import jd.gui.swing.jdgui.components.toolbar.actions.ClipBoardToggleAction;
import jd.gui.swing.jdgui.components.toolbar.actions.ExitToolbarAction;
import jd.gui.swing.jdgui.components.toolbar.actions.GlobalPremiumSwitchToggleAction;
import jd.gui.swing.jdgui.components.toolbar.actions.OpenDefaultDownloadFolderAction;
import jd.gui.swing.jdgui.components.toolbar.actions.PauseDownloadsAction;
import jd.gui.swing.jdgui.components.toolbar.actions.ReconnectAction;
import jd.gui.swing.jdgui.components.toolbar.actions.ShowSettingsAction;
import jd.gui.swing.jdgui.components.toolbar.actions.StartDownloadsAction;
import jd.gui.swing.jdgui.components.toolbar.actions.StopDownloadsAction;
import jd.gui.swing.jdgui.components.toolbar.actions.UpdateAction;

import org.appwork.exceptions.WTFException;
import org.appwork.storage.config.JsonConfig;

public class ToolbarManager {
    private static final ToolbarManager INSTANCE = new ToolbarManager();

    /**
     * get the only existing instance of ToolbarManager. This is a singleton
     * 
     * @return
     */
    public static ToolbarManager getInstance() {
        return ToolbarManager.INSTANCE;
    }

    private HashMap<String, AbstractToolbarAction> map;
    private ArrayList<AbstractToolbarAction>       list;

    public AbstractToolbarAction[] getList() {
        return list.toArray(new AbstractToolbarAction[] {});
    }

    private ToolbarSettings config;

    /**
     * Create a new instance of ToolbarManager. This is a singleton class.
     * Access the only existing instance by using {@link #getInstance()}.
     */
    private ToolbarManager() {
        config = JsonConfig.create(ToolbarSettings.class);
        this.map = new HashMap<String, AbstractToolbarAction>();
        list = new ArrayList<AbstractToolbarAction>();
        addIntern(StartDownloadsAction.getInstance());
        addIntern(PauseDownloadsAction.getInstance());
        addIntern(StopDownloadsAction.getInstance());
        addIntern(Seperator.getInstance());
        addIntern(ClipBoardToggleAction.getInstance());
        addIntern(AutoReconnectToggleAction.getInstance());
        addIntern(GlobalPremiumSwitchToggleAction.getInstance());
        addIntern(Seperator.getInstance());
        addIntern(ReconnectAction.getInstance());
        addIntern(UpdateAction.getInstance());
        addIntern(OpenDefaultDownloadFolderAction.getInstance());

        addIntern(ShowSettingsAction.getInstance());
        addIntern(ExitToolbarAction.getInstance());

        update();

    }

    public void update() {

        ArrayList<ActionConfig> order = config.getSetup();
        if (order != null && order.size() > 0) {
            list.clear();
            for (ActionConfig ac : order) {
                AbstractToolbarAction action = map.get(ac.getId());
                if (action != null && ac.isVisible()) {
                    list.add(action);
                }
            }

        }
    }

    public void add(AbstractToolbarAction action) {
        addIntern(action);
        update();
        MainToolBar.getInstance().updateToolbar();
    }

    private void addIntern(AbstractToolbarAction action) {
        if (action != Seperator.getInstance() && map.containsKey(action.getID())) throw new WTFException("Please Choose a different id for " + action + " OVERRIDE  public String getID() {");
        map.put(action.getID(), action);
        if (action.isDefaultVisible()) list.add(action);
    }

    private void removeIntern(AbstractToolbarAction action) {
        map.remove(action.getID());
        list.remove(action);
    }

    public void remove(AbstractToolbarAction action) {
        removeIntern(action);
        update();
        MainToolBar.getInstance().updateToolbar();
    }

}
