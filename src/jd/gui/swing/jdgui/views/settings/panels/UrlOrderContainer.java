package jd.gui.swing.jdgui.views.settings.panels;

import javax.swing.JScrollPane;

import jd.gui.swing.jdgui.views.settings.components.SettingsComponent;
import jd.gui.swing.jdgui.views.settings.panels.urlordertable.UrlOrderTable;

public class UrlOrderContainer extends org.appwork.swing.MigPanel implements SettingsComponent {

    private UrlOrderTable urlOrder;

    public UrlOrderContainer(UrlOrderTable urlOrder) {
        super("ins 0", "[grow,fill]", "[]");
        this.urlOrder = urlOrder;
        JScrollPane sp = new JScrollPane(urlOrder);
        sp.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);

        add(sp);
    }

    @Override
    public String getConstraints() {
        return null;
        // return "height n:n:" + (urlOrder.getPreferredSize().height + 32);
    }

    @Override
    public boolean isMultiline() {
        return true;
    }
}
