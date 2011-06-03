package jd.gui.swing.jdgui.views.settings.panels.linkgrabberfilter;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JToolBar;

import jd.gui.swing.jdgui.views.settings.components.SettingsComponent;
import jd.gui.swing.jdgui.views.settings.components.StateUpdateListener;
import net.miginfocom.swing.MigLayout;

import org.appwork.utils.swing.HelpNotifier;
import org.appwork.utils.swing.HelpNotifierCallbackListener;
import org.jdownloader.gui.translate._GUI;

public class LinkgrabberFilter extends JPanel implements SettingsComponent {
    private static final long serialVersionUID = 6070464296168772795L;
    private JToolBar          tb;
    private FilterTable       table;
    private JTextField        txt;
    private JButton           test;

    public LinkgrabberFilter() {
        super(new MigLayout("ins 0,wrap 1", "[grow,fill]", "[][grow,fill][]"));
        tb = new JToolBar();
        tb.setFloatable(false);
        table = new FilterTable();
        table.addMouseListener(new ContextMenuListener(this));
        tb.add(new JButton(new NewAction(this)));
        tb.add(new JButton(new RemoveAction(this)));

        txt = new JTextField();
        test = new JButton(new TestAction(this));

        HelpNotifier.register(txt, new HelpNotifierCallbackListener() {

            public void onHelpNotifyShown(JComponent c) {
                test.setEnabled(false);
            }

            public void onHelpNotifyHidden(JComponent c) {
                test.setEnabled(true);
            }

        }, _GUI._.settings_linkgrabber_filter_test_helpurl());
        add(new JScrollPane(table));
        add(tb);

        add(txt, "split 2,growx,pushx");

        add(test, "shrinkx,height 22!");
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

    public String getTestText() {
        if (txt.getText().equals(_GUI._.settings_linkgrabber_filter_test_helpurl()) || txt.getText().trim().length() == 0) return null;
        return txt.getText();
    }

    public void addStateUpdateListener(StateUpdateListener listener) {
        throw new IllegalStateException("Not implemented");
    }
}
