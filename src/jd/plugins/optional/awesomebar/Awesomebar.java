package jd.plugins.optional.awesomebar;

import java.awt.Color;
import java.awt.Font;
import java.util.ArrayList;


import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import jd.PluginWrapper;
import jd.gui.swing.jdgui.actions.ActionController;
import jd.gui.swing.jdgui.actions.CustomToolbarAction;
import jd.gui.swing.jdgui.components.toolbar.ToolBar;
import jd.gui.swing.jdgui.menu.MenuAction;
import jd.gui.swing.jdgui.settings.panels.gui.ToolbarController;
import jd.plugins.OptionalPlugin;
import jd.plugins.PluginOptional;
import net.miginfocom.swing.MigLayout;

@OptionalPlugin(rev = "$Revision: 10379 $", id = "addons.awesomebar", hasGui = true, interfaceversion = 5)
public class Awesomebar extends PluginOptional  {
    
    private CustomToolbarAction toolbarAction;
    
    public Awesomebar(PluginWrapper wrapper) {
        super(wrapper);
        this.toolbarAction = new CustomToolbarAction("addons.awesomebar") {
            private static final long serialVersionUID = 3329576457034851778L;
            private JPanel cp;

            /**
             * is called before every toolbar rebuild
             */
            @Override
            public void init() {
            }

            // cannot be disabled

            public boolean force() {
                return true;
            }

            /**
             * Gets called when initializing the instance
             */
            @Override
            public void initDefaults() {
                cp = new JPanel(new MigLayout("ins 1"));
                // textfield
                JTextField bar = new JTextField();
                cp.add(bar, "width 150px!, wrap");
                JLabel desc = new JLabel("description!");
                desc.setForeground(Color.gray);
                desc.setFont(new Font("Arial", Font.PLAIN, 9));
                desc.setSize(150, 9);
                cp.add(desc,"width 150px!, height 9px!");
            }

            /**
             * has to add something to the toolbar
             */
            @Override
            public void addTo(Object toolBar) {
                // Toolbara ctions might be used by other components, too
                // iot's up to the custom action to implement them
                if (toolBar instanceof ToolBar) {
                    ToolBar tb = (ToolBar) toolBar;
                    tb.add(cp, "");
                }

            }

        };
        initAddon();
    }

    @Override
    public boolean initAddon() {
        setGuiEnable(true);
        return false;
    }

    @Override
    public void onExit() {
        
    }

    @Override
    public ArrayList<MenuAction> createMenuitems() {
        return null;
    }
    
    @Override
    public void setGuiEnable(boolean b) {
        System.out.println("setGuiEnable");
        if (b) {
            ActionController.register(toolbarAction);
            ToolbarController.setActions(ActionController.getActions());

        } else {
            ActionController.unRegister(toolbarAction);
            ToolbarController.setActions(ActionController.getActions());
        }
    }
    
}