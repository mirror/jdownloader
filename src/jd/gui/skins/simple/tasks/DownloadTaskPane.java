package jd.gui.skins.simple.tasks;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.UIManager;

import jd.gui.skins.simple.SimpleGUI;
import jd.gui.skins.simple.components.SpeedMeterPanel;
import jd.utils.JDTheme;
import jd.utils.JDUtilities;
import net.miginfocom.swing.MigLayout;

public class DownloadTaskPane extends TaskPanel implements ActionListener {

    /**
     * 
     */
    private static final long serialVersionUID = -9134449913836967453L;
    public static final int ACTION_SHOW_PANEL = 1;
    private SpeedMeterPanel speedmeter;

    public DownloadTaskPane(String string, ImageIcon ii) {
        super(string, ii, "downloadtask");
        this.setLayout(new MigLayout("ins 0, wrap 1", "[fill,grow]"));
        initGUI();
    }

    private void initGUI() {
       // add(new JLabel(JDTheme.II("gui.images.next")));
        if (JDUtilities.getSubConfig(SimpleGUI.GUICONFIGNAME).getBooleanProperty(SimpleGUI.PARAM_SHOW_SPEEDMETER, true)) {
            speedmeter = new SpeedMeterPanel();
            add(speedmeter);
        }

    }

    public void actionPerformed(ActionEvent e) {
        // TODO Auto-generated method stub

    }

}
