package jd.gui.swing.jdgui.views.settings.panels.packagizer;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import jd.gui.swing.jdgui.views.settings.components.SettingsComponent;
import jd.gui.swing.jdgui.views.settings.components.StateUpdateListener;
import net.miginfocom.swing.MigLayout;

import org.appwork.app.gui.MigPanel;
import org.appwork.swing.exttable.utils.MinimumSelectionObserver;

public class PackagizerFilter extends JPanel implements SettingsComponent {
    private static final long serialVersionUID = 6070464296168772795L;
    private MigPanel          tb;
    private FilterTable       table;

    public PackagizerFilter() {
        super(new MigLayout("ins 0,wrap 1", "[grow,fill]", "[][grow,fill]"));
        tb = new MigPanel("ins 0", "[][]", "[]");
        table = new FilterTable();
        tb.add(new JButton(new NewAction(table)), "height 26!,sg 1");
        RemoveAction ra;
        tb.add(new JButton(ra = new RemoveAction(table)), "height 26!,sg 1");
        table.getSelectionModel().addListSelectionListener(new MinimumSelectionObserver(table, ra, 1));

        add(new JScrollPane(table));
        add(tb);

    }

    public FilterTable getTable() {
        return table;
    }

    public String getConstraints() {
        return "wmin 10,height 60:n:n";
    }

    public boolean isMultiline() {
        return false;
    }

    public void addStateUpdateListener(StateUpdateListener listener) {
        throw new IllegalStateException("Not implemented");
    }
}
