package jd.gui.skins.simple.tasks;

import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;

import javax.swing.ImageIcon;
/**
 * This TaskPanel does not have a content pane and thus just is an button with an icon
 * @author coalado
 *
 */
abstract public class TaskButton extends TaskPanel {
    public TaskButton(String string, ImageIcon ii, String pid) {
        super(string, ii, pid);
        super.setDeligateCollapsed(true);
    }

    private boolean collapsed = false;

    public boolean isCollapsed() {
        return collapsed;
    }
    public void setDeligateCollapsed(boolean collapsed) {
        setCollapsed(collapsed);           
    }
    public void setCollapsed(boolean collapsed) {
        super.setCollapsed(true);
        boolean oldValue = isCollapsed();
        this.collapsed = collapsed;

        firePropertyChange("collapsed", oldValue, isCollapsed());
    
    }
    public void mouseReleased(MouseEvent e) {
              
        broadcastEvent(new ActionEvent(this, ACTION_CLICK, "Toggle"));
    }

}
