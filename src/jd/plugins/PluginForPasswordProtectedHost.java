package jd.plugins;

import java.awt.event.ActionEvent;
import java.util.ArrayList;

import jd.PluginWrapper;
import jd.config.MenuItem;
import jd.event.ControlEvent;
import jd.event.ControlListener;
import jd.utils.JDUtilities;

abstract public class PluginForPasswordProtectedHost extends PluginForHost implements ControlListener {

    public PluginForPasswordProtectedHost(PluginWrapper wrapper) {
        super(wrapper);
        JDUtilities.getController().addControlListener(this);
    }

    public void controlEvent(ControlEvent event) {
        if (event.getID() == ControlEvent.CONTROL_LINKLIST_CONTEXT_MENU) {
            ArrayList<MenuItem> entries = (ArrayList<MenuItem>) event.getParameter();

            MenuItem m;
            //Als id was 100% eindeutiges nehmen ;-P
            entries.add(m = new MenuItem("Testitem", 11111));
            m.setActionListener(this);
        }

    }

    public void actionPerformed(ActionEvent e) {
        super.actionPerformed(e);
        if (e.getID() == 1) {
            System.out.println("HUHU");

        }

    }

}
