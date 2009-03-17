package jd.gui.skins.simple.tasks;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JTextPane;

import jd.utils.JDLocale;
import jd.utils.JDTheme;
import net.miginfocom.swing.MigLayout;

public class DownloadTaskPane extends TaskPanel implements ActionListener {

    /**
     * 
     */
    private static final long serialVersionUID = -9134449913836967453L;
    public static final int ACTION_SHOW_PANEL = 1;

    public DownloadTaskPane(String string, ImageIcon ii) {
        super(string, ii, "downloadtask");
        this.setLayout(new MigLayout("ins 0, wrap 2", "[][fill]"));
        initGUI();
    }

    private void initGUI() {
        add(new JLabel(JDTheme.II("gui.images.next")));
        JTextPane tp = new JTextPane();
        tp.setText(JDLocale.L("gui.taskpanes.download.info","Click Start/Stop in \r\n the toolbar to \r\ndownload your links"));
        tp.setEditable(false);
        add(tp);

    }

    public void actionPerformed(ActionEvent e) {
        // TODO Auto-generated method stub

    }

}
