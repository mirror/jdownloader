package jd.gui.skins.simple.tasks;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JSeparator;

import jd.config.ConfigEntry.PropertyType;
import jd.event.ControlEvent;
import jd.event.ControlListener;
import jd.gui.skins.simple.ContentPanel;
import jd.gui.skins.simple.components.Linkgrabber.LinkGrabberV2TreeTableAction;
import jd.gui.skins.simple.config.ConfigPanel;
import jd.utils.JDLocale;
import jd.utils.JDTheme;
import jd.utils.JDUtilities;
import net.miginfocom.swing.MigLayout;

public class LinkGrabberTaskPane extends TaskPanel implements ActionListener, ControlListener {

    private JButton add_all;
    private JButton add_selected;
    public static final int ACTION_SHOW_PANEL = 1;
    public static final int ACTION_ADD_ALL = 100;

    public LinkGrabberTaskPane(String string, ImageIcon ii) {
        super(string, ii, "linkgrabber");
        this.setLayout(new MigLayout("ins 0, wrap 1", "[fill]"));
        JDUtilities.getController().addControlListener(this);
        initGUI();
    }

    private void initGUI() {
        this.add_all = addButton(this.createButton(JDLocale.L("gui.linkgrabberv2.addall", "Add all packages"), JDTheme.II("gui.images.add", 16, 16)));
        this.add_selected = addButton(this.createButton(JDLocale.L("gui.linkgrabberv2.addselected", "Add selected package(s)"), JDTheme.II("gui.images.add", 16, 16)));
    }

    private JButton addButton(JButton bt) {
        bt.addActionListener(this);
        bt.setHorizontalAlignment(JButton.LEFT);
        add(bt, "alignx leading");
        return bt;
    }

    /**
     * 
     */
    private static final long serialVersionUID = -7720749076951577192L;

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == add_all) {
            this.broadcastEvent(new ActionEvent(this, LinkGrabberV2TreeTableAction.ADD_ALL, ((JButton) e.getSource()).getName()));
            return;
        }
        if (e.getSource() == add_selected) {
            this.broadcastEvent(new ActionEvent(this, LinkGrabberV2TreeTableAction.ADD_SELECTED, ((JButton) e.getSource()).getName()));
            return;
        }
    }

    public void controlEvent(ControlEvent event) {

    }

}
