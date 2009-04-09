package jd.plugins.optional.langfileeditor;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;

import jd.gui.skins.simple.SimpleGUI;
import jd.gui.skins.simple.SingletonPanel;
import jd.gui.skins.simple.tasks.DownloadTaskPane;
import jd.gui.skins.simple.tasks.TaskButton;
import jd.utils.JDTheme;

public class LFETaskPane extends TaskButton {

    private static final long serialVersionUID = -5506038175097521342L;
    private JButton showGui;

    public LFETaskPane(String string) {
        super(string, JDTheme.II("gui.splash.languages", 24, 24), "lfe");

        initGUI();

        addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                System.out.println(e.getActionCommand());
                switch (e.getID()) {
                case DownloadTaskPane.ACTION_TOGGLE:

                    SimpleGUI.CURRENTGUI.getContentPane().display(getPanel(0));
                    break;

                }

            }
        });
    }

    private void initGUI() {
        this.addPanel(new SingletonPanel(LFEGui.class));

     
    }

    public void actionPerformed(ActionEvent e) {
        // TODO Auto-generated method stub
        
    }

}
