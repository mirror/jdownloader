package jd.updater.panel;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;

import net.miginfocom.swing.MigLayout;

public class UpdaterGuiPanel extends JPanel {
    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    private JProgressBar      bar;
    private JLabel            label;

    public UpdaterGuiPanel() {
        super(new MigLayout("ins 0, wrap 1", "[grow]", "[][]"));
        layoutPanel();

    }

    private void layoutPanel() {
        // init components
        label = new JLabel("Please wait. Installing Updates!");
        bar = new JProgressBar(0, 100);

        // layout
        add(label);
        add(bar, "width 100:300:n,pushx,growx");

    }

}
