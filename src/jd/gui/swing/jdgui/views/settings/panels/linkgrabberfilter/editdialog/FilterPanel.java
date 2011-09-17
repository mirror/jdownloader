package jd.gui.swing.jdgui.views.settings.panels.linkgrabberfilter.editdialog;

import java.awt.Component;
import java.awt.event.MouseListener;

import org.appwork.app.gui.MigPanel;

public class FilterPanel extends MigPanel {

    /**
     * 
     */
    private static final long serialVersionUID = -8988327708287410467L;

    public FilterPanel(String cols, String rows) {
        super("ins 0", cols, rows);
    }

    public FilterPanel(String constraints, String cols, String rows) {
        super(constraints, cols, rows);
    }

    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        for (Component c : getComponents()) {
            c.setEnabled(enabled);
        }
    }

    public synchronized void addMouseListener(MouseListener l) {
        super.addMouseListener(l);
        for (Component c : getComponents()) {
            c.addMouseListener(l);
        }

    }

}
