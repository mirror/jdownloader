package jd.plugins.optional.langfileeditor;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;

import jd.gui.skins.simple.SimpleGUI;
import jd.gui.skins.simple.SingletonPanel;
import jd.gui.skins.simple.tasks.TaskPanel;
import jd.utils.JDLocale;
import jd.utils.JDTheme;

public class LFETaskPane extends TaskPanel implements ActionListener {

    private static final long serialVersionUID = -5506038175097521342L;
    private JButton showGui;

    public LFETaskPane(String string) {
        super(string, JDTheme.II("gui.splash.languages", 24, 24), "lfe");

        initGUI();
    }

    private void initGUI() {
        this.addPanel(new SingletonPanel(LFEGui.class));

        showGui = createButton(JDLocale.L("plugins.optional.langfileeditor.taskpane", "Show LFE"), JDTheme.II("gui.splash.languages", 16, 16));
        add(showGui, D1_BUTTON_ICON);
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == showGui) {
            SimpleGUI.CURRENTGUI.getContentPane().display(this.getPanel(0));
            SimpleGUI.CURRENTGUI.getTaskPane().switcher(this);
        }
    }

}
