package jd.gui.swing.jdgui.menu;

import javax.swing.JMenuBar;

public class JDMenuBar extends JMenuBar {

    private static final long serialVersionUID = 6758718947311901334L;

    public JDMenuBar() {
        super();

        add(new FileMenu());
        add(new EditMenu());
        add(PremiumMenu.getInstance());
        add(AddonsMenu.getInstance());
        add(WindowMenu.getInstance());
        add(new AboutMenu());
    }

}
