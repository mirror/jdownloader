package jd.gui.swing.jdgui.actions;

public abstract class CustomToolbarAction extends ToolBarAction {

    private static final long serialVersionUID = -1783111085477999729L;

    public CustomToolbarAction(String menuKey) {
        super(menuKey, "gui.images.config.addons", 0);
        ActionController.unRegister(this);
    }

    /**
     * Gets called by a gui element. the custom action has to add herself to
     * this guielement
     * 
     * @param toolBar
     */
    abstract public void addTo(Object toolBar);

}
