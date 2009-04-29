package jd.gui.skins.simple.startmenu;

import jd.gui.skins.simple.startmenu.actions.RemoveAllAction;
import jd.gui.skins.simple.startmenu.actions.RemoveDisabledAction;
import jd.gui.skins.simple.startmenu.actions.RemoveDupesAction;
import jd.gui.skins.simple.startmenu.actions.RemoveFailedAction;
import jd.gui.skins.simple.startmenu.actions.RemoveFinishedAction;

public class CleanupMenu extends JStartMenu {

    private static final long serialVersionUID = 2238339685705371437L;

    public CleanupMenu() {
        super("gui.menu.remove", "gui.images.delete", "gui.menu.remove.desc");
        this.add(new RemoveFinishedAction());
        this.add(new RemoveFailedAction());
        this.add(new RemoveDisabledAction());
        this.add(new RemoveDupesAction());
        this.add(new RemoveAllAction());
    }

}
