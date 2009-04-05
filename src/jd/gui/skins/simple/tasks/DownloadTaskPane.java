package jd.gui.skins.simple.tasks;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ImageIcon;
import javax.swing.JButton;

import net.miginfocom.swing.MigLayout;

public class DownloadTaskPane extends TaskButton implements ActionListener {

    /**
     * 
     */
    private static final long serialVersionUID = -9134449913836967453L;
    public static final int ACTION_SHOW_PANEL = 1;
    public static final int ACTION_STARTSTOP = 2;

    private JButton startStop;

    public DownloadTaskPane(String string, ImageIcon ii) {
        super(string, ii, "downloadtask");
        this.setLayout(new MigLayout("ins 0, wrap 1", "[fill,grow]", "[fill][grow]"));
    }

    public void actionPerformed(ActionEvent e) {

        if (e.getSource() == startStop) {
            this.broadcastEvent(new ActionEvent(this, ACTION_STARTSTOP, ((JButton) e.getSource()).getName()));
            return;
        }

    }

}
