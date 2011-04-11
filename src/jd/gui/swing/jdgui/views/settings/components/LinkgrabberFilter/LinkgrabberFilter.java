package jd.gui.swing.jdgui.views.settings.components.LinkgrabberFilter;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JToolBar;

import jd.gui.swing.jdgui.views.settings.components.SettingsComponent;
import net.miginfocom.swing.MigLayout;

import org.appwork.utils.swing.HelpNotifier;
import org.appwork.utils.swing.HelpNotifierCallbackListener;
import org.jdownloader.extensions.antireconnect.translate.T;

public class LinkgrabberFilter extends JPanel implements SettingsComponent {
    private JToolBar    tb;
    private FilterTable table;
    private JTextField  txt;
    private JButton     test;

    public LinkgrabberFilter() {
        super(new MigLayout("ins 0,wrap 1", "[grow,fill]", "[][grow,fill][]"));
        tb = new JToolBar();
        tb.setFloatable(false);
        table = new FilterTable();
        tb.add(new JButton(new NewAction(table)));
        tb.add(new JButton(new RemoveAction(table)));

        txt = new JTextField();
        test = new JButton(new TestAction(table));
        add(tb);
        HelpNotifier.register(txt, new HelpNotifierCallbackListener() {

            public void onHelpNotifyShown(JComponent c) {
            }

            public void onHelpNotifyHidden(JComponent c) {
            }

        }, T._.settings_linkgrabber_filter_test_helpurl());
        add(new JScrollPane(table));
        add(txt, "split 2,growx,pushx");
        add(test, "shrinkx");
    }

    public String getConstraints() {
        return "wmin 10,height 60:n:n";
    }

    public boolean isMultiline() {
        return false;
    }
}
