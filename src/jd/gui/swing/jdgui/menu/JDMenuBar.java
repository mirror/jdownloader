package jd.gui.swing.jdgui.menu;

import java.awt.Component;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.lang.reflect.InvocationTargetException;

import javax.swing.AbstractButton;
import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JMenuBar;

import jd.SecondLevelLaunch;

import org.appwork.utils.swing.EDTRunner;
import org.jdownloader.controlling.contextmenu.MenuContainer;
import org.jdownloader.controlling.contextmenu.MenuItemData;
import org.jdownloader.controlling.contextmenu.gui.MenuBuilder;
import org.jdownloader.extensions.ExtensionNotLoadedException;
import org.jdownloader.gui.mainmenu.MainMenuManager;

public class JDMenuBar extends JMenuBar implements MouseListener {
    private static final JDMenuBar INSTANCE = new JDMenuBar();

    /**
     * get the only existing instance of JDMenuBar. This is a singleton
     * 
     * @return
     */
    public static JDMenuBar getInstance() {
        return JDMenuBar.INSTANCE;
    }

    private static final long serialVersionUID = 6758718947311901334L;

    /**
     * Create a new instance of JDMenuBar. This is a singleton class. Access the only existing instance by using {@link #getInstance()}.
     */
    private JDMenuBar() {
        super();
        // updateLayout();
        SecondLevelLaunch.GUI_COMPLETE.executeWhenReached(new Runnable() {

            public void run() {

                new EDTRunner() {

                    @Override
                    protected void runInEDT() {
                        updateLayout();
                        // add(new FileMenu());
                        // // add(new EditMenu());
                        // add(new SettingsMenu());
                        // add(AddonsMenu.getInstance());
                        //
                        // add(new AboutMenu());
                    }
                };
            }
        });

        this.addMouseListener(this);

    }

    protected void addImpl(Component comp, Object constraints, int index) {
        super.addImpl(comp, constraints, index);
        comp.addMouseListener(this);
    }

    public void updateLayout() {

        removeAll();

        new MenuBuilder(MainMenuManager.getInstance(), this, null, MainMenuManager.getInstance().getMenuData()) {

            @Override
            protected void addContainer(JComponent root, MenuItemData inst) throws InstantiationException, IllegalAccessException, InvocationTargetException, ClassNotFoundException, NoSuchMethodException, ExtensionNotLoadedException {
                final JMenu submenu = (JMenu) inst.addTo(root, selection);

                // final Field f = KeyEvent.class.getField("VK_" + Character.toUpperCase(mnemonic));
                // final int m = (Integer) f.get(null);
                // putValue(AbstractAction.MNEMONIC_KEY, m);
                // putValue(AbstractAction.DISPLAYED_MNEMONIC_INDEX_KEY, getName().indexOf(m));
                if (submenu != null) {
                    applyMnemonic(root, submenu);
                    if (root == JDMenuBar.this) submenu.setIcon(null);
                    createLayer(submenu, (MenuContainer) inst);
                }
            }

            @Override
            protected void addAction(JComponent root, MenuItemData inst) throws InstantiationException, IllegalAccessException, InvocationTargetException, ClassNotFoundException, NoSuchMethodException, ExtensionNotLoadedException {
                JComponent comp = inst.addTo(root, selection);
                if (comp instanceof AbstractButton) {
                    applyMnemonic(root, (AbstractButton) comp);
                }
            }

        }.run();

        if (getMenuCount() == 0) {

            new MenuBuilder(MainMenuManager.getInstance(), this, null, MainMenuManager.getInstance().createDefaultStructure()) {

                @Override
                protected void addContainer(JComponent root, MenuItemData inst) throws InstantiationException, IllegalAccessException, InvocationTargetException, ClassNotFoundException, NoSuchMethodException, ExtensionNotLoadedException {
                    final JMenu submenu = (JMenu) inst.addTo(root, selection);
                    if (root == JDMenuBar.this) submenu.setIcon(null);
                    createLayer(submenu, (MenuContainer) inst);
                }

                @Override
                protected void addAction(JComponent root, MenuItemData inst) throws InstantiationException, IllegalAccessException, InvocationTargetException, ClassNotFoundException, NoSuchMethodException, ExtensionNotLoadedException {
                    inst.addTo(root, selection);
                }

            }.run();
        }
        ;
        // MenuContainerRoot dat = MainMenuManager.getInstance().getMenuData();
        // for(MenuItemData mid:dat.getItems()){
        // if(!mid.showItem(null)) continue;
        repaint();

    }

    @Override
    public void mouseClicked(MouseEvent e) {
    }

    @Override
    public void mousePressed(MouseEvent e) {
        if (e.isPopupTrigger() || e.getButton() == 3) {

            MainMenuManager.getInstance().openGui();
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
    }

    @Override
    public void mouseEntered(MouseEvent e) {
    }

    @Override
    public void mouseExited(MouseEvent e) {
    }

}
