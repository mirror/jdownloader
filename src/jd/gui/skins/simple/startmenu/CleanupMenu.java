package jd.gui.skins.simple.startmenu;


import jd.gui.skins.simple.startmenu.actions.CleanupDownloads;
import jd.gui.skins.simple.startmenu.actions.CleanupPackages;


public class CleanupMenu extends JStartMenu {

    private static final long serialVersionUID = 2238339685705371437L;

    public CleanupMenu() {
        super("gui.menu.remove", "gui.images.delete", "gui.menu.remove.desc");

        this.add(new CleanupDownloads());
        this.add(new CleanupPackages());
    }

}
