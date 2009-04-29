package jd.gui.skins.simple.startmenu;

import jd.gui.skins.simple.startmenu.actions.AboutAction;
import jd.gui.skins.simple.startmenu.actions.KnowledgeAction;
import jd.gui.skins.simple.startmenu.actions.LatestChanges;

public class AboutMenu extends JStartMenu {

    private static final long serialVersionUID = 1899581616146592295L;

    public AboutMenu() {
        super("gui.menu.about", "gui.images.help", "gui.menu.about.desc");
        this.setIconTextGap(15);
        this.add(new AboutAction());
        this.add(new KnowledgeAction());
        this.add(new LatestChanges());

    }

}
