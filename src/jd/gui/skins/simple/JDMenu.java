package jd.gui.skins.simple;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;

import jd.config.MenuItem;

public class JDMenu extends JMenu {

    private static final long serialVersionUID = 7827312050360423912L;

    public JDMenu(String l) {
        super(l);
    }

    public static JMenuItem getJMenuItem(final MenuItem mi) {
        JMenuItem m;
        switch (mi.getID()) {
        case MenuItem.SEPARATOR:
            return null;
        case MenuItem.NORMAL:
            m = new JMenuItem(new JDMenuAction(mi));
            if (mi.getAccelerator() != null) m.setAccelerator(KeyStroke.getKeyStroke(mi.getAccelerator()));
            return m;
        case MenuItem.TOGGLE:
            JCheckBoxMenuItem m2 = new JCheckBoxMenuItem(new JDMenuAction(mi));
            m2.setSelected(mi.isSelected());

            if (mi.getAccelerator() != null) m2.setAccelerator(KeyStroke.getKeyStroke(mi.getAccelerator()));
            return m2;
        case MenuItem.CONTAINER:
            JMenu m3 = new JMenu(mi.getTitle());
            JMenuItem c;
            if (mi.getSize() > 0) for (int i = 0; i < mi.getSize(); i++) {
                c = getJMenuItem(mi.get(i));
                if (c == null) {
                    m3.addSeparator();
                } else {
                    m3.add(c);
                }
            }
            m3.addMenuListener(new MenuListener() {

                public void menuCanceled(MenuEvent e) {
                }

                public void menuDeselected(MenuEvent e) {
                }

                public void menuSelected(MenuEvent e) {
                    JMenu m = (JMenu) e.getSource();
                    m.removeAll();
                    JMenuItem c;
                    if (mi.getSize() == 0) m.setEnabled(false);
                    for (int i = 0; i < mi.getSize(); i++) {
                        c = getJMenuItem(mi.get(i));
                        if (c == null) {
                            m.addSeparator();
                        } else {
                            m.add(c);
                        }

                    }
                }

            });

            return m3;
        }
        return null;
    }

}
