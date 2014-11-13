package jd.gui.swing.jdgui.views.settings.panels.anticaptcha;

import javax.swing.JScrollPane;

import jd.gui.swing.jdgui.views.settings.components.SettingsComponent;

import org.appwork.utils.swing.SwingUtils;

public class SolverOrderContainer extends org.appwork.swing.MigPanel implements SettingsComponent {

    private SolverOrderTable solverOrder;

    public SolverOrderContainer(SolverOrderTable urlOrder) {
        super("ins 0", "[grow,fill]", "[]");
        this.solverOrder = urlOrder;
        JScrollPane sp = new JScrollPane(urlOrder);
        sp.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
        SwingUtils.setOpaque(this, false);
        add(sp);
    }

    @Override
    public String getConstraints() {
        // return null;
        return "height " + (solverOrder.getPreferredSize().height + 27) + "!, wmin 10";

    }

    @Override
    public boolean isMultiline() {
        return true;
    }
}
