package jd.gui.swing.jdgui.views.settings.panels.linkgrabberfilter;

import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

import jd.gui.swing.jdgui.views.settings.components.SettingsComponent;
import jd.gui.swing.jdgui.views.settings.components.StateUpdateListener;
import net.miginfocom.swing.MigLayout;

import org.appwork.app.gui.MigPanel;
import org.appwork.swing.components.ExtButton;
import org.appwork.swing.components.ExtCheckBox;
import org.appwork.swing.exttable.utils.MinimumSelectionObserver;
import org.jdownloader.controlling.filter.LinkFilterSettings;
import org.jdownloader.gui.translate._GUI;

public class LinkgrabberFilter extends JPanel implements SettingsComponent {
    private static final long serialVersionUID = 6070464296168772795L;
    private MigPanel          tb;
    private FilterTable       table;
    private ExtButton         btadd;
    private ExtCheckBox       enable;
    private ExtButton         btImport;
    private ExtButton         btExport;

    public LinkgrabberFilter() {
        super(new MigLayout("ins 0,wrap 1", "[grow,fill]", "[grow,fill][]"));
        tb = new MigPanel("ins 0", "[][][grow,fill][][][][]", "[]");
        table = new FilterTable();
        tb.add(btadd = new ExtButton(new NewAction(this)), "height 26!,sg 1");
        RemoveAction ra;
        ExtButton btRemove;
        tb.add(btRemove = new ExtButton(ra = new RemoveAction(table)), "height 26!,sg 1");
        tb.add(Box.createHorizontalGlue());

        tb.add(btImport = new ExtButton(new ImportAction(table)), "height 26!,sg 2");

        tb.add(btExport = new ExtButton(new ExportAction()), "height 26!,sg 2,gapright 15");

        table.getExtTableModel().addTableModelListener(new TableModelListener() {

            public void tableChanged(TableModelEvent e) {
                btExport.setEnabled(table.getRowCount() > 0);
            }
        });

        tb.add(new JLabel(_GUI._.LinkgrabberFilter_LinkgrabberFilter_enable()));
        enable = new ExtCheckBox(LinkFilterSettings.LINK_FILTER_ENABLED, table, btadd, btRemove);

        tb.add(enable);

        table.getSelectionModel().addListSelectionListener(new MinimumSelectionObserver(table, ra, 1));

        add(new JScrollPane(table));

        add(tb);

    }

    public FilterTable getTable() {
        return table;
    }

    public String getConstraints() {

        return "height 60:n:n,pushy,growy";
    }

    public boolean isMultiline() {
        return false;
    }

    public void addStateUpdateListener(StateUpdateListener listener) {
        throw new IllegalStateException("Not implemented");
    }
}
