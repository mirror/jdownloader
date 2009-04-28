package jd.gui.skins.simple.startmenu;

import jd.gui.skins.simple.startmenu.actions.AddContainerAction;
import jd.gui.skins.simple.startmenu.actions.AddLinksAction;
import jd.gui.skins.simple.startmenu.actions.AddUrlAction;

public class AddLinksMenu extends JStartMenu {

    private static final long serialVersionUID = -3531629185758097151L;

    public AddLinksMenu() {
        super("gui.menu.add", "gui.images.add", "gui.menu.add.desc");
        this.add(new AddLinksAction());
        this.add(new AddContainerAction());
        this.add(new AddUrlAction());
    }

}
