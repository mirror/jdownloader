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
import jd.gui.skins.simple.config.ConfigPanel;
import jd.utils.JDLocale;
import jd.utils.JDTheme;
import jd.utils.JDUtilities;
import net.miginfocom.swing.MigLayout;

public class LinkGrabberTaskPane extends TaskPanel implements ActionListener, ControlListener {

    private JButton add_all;
    public static final int ACTION_ADD_ALL = 100;

    public LinkGrabberTaskPane(String string, ImageIcon ii) {
        super(string, ii, "linkgrabber");
        this.setLayout(new MigLayout("ins 0, wrap 1", "[fill]"));
        JDUtilities.getController().addControlListener(this);
        initGUI();
    }

    private void initGUI() {
        this.add_all = addButton(this.createButton(JDLocale.L("gui.config.tabLables.general", "general"), JDTheme.II("gui.images.config.home", 16, 16)));
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

    }

    public void controlEvent(ControlEvent event) {
        
    }

}
