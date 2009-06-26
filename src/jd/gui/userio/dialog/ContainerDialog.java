package jd.gui.userio.dialog;

import javax.swing.JComponent;
import javax.swing.JPanel;

public class ContainerDialog extends AbstractDialog {

    private static final long serialVersionUID = -348017625663435924L;
    private JPanel panel;

    public ContainerDialog(int flags, String title, JPanel panel, String ok, String cancel) {
        super(flags, title, null, ok, cancel);
        this.panel = panel;
        init();
    }

    @Override
    public JComponent contentInit() {
        return panel;
    }

}
