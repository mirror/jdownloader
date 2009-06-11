package jd.gui.userio.dialog;

import javax.swing.JComponent;
import javax.swing.JPanel;

public class ContainerDialog extends AbstractDialog {

    /**
     * 
     */
    private static final long serialVersionUID = -348017625663435924L;
    private JPanel panel;

    public ContainerDialog(int i, String title, JPanel p, String ok, String cancel) {
        super(i, title, null, ok, cancel);
        this.panel = p;
        init();
    }

    @Override
    public JComponent contentInit() {
        // TODO Auto-generated method stub
        return panel;
    }

}
