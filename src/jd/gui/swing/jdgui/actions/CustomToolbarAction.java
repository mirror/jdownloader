/**
 * 
 */
package jd.gui.swing.jdgui.actions;


/**
 * @author unkown
 * 
 */
public abstract class CustomToolbarAction extends ToolBarAction {

    public CustomToolbarAction(String menuKey) {
        super(menuKey, "gui.splash.plugins", 0);
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
