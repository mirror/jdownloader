package jd.gui.swing.jdgui.views.settings.panels.basicauthentication;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import jd.gui.swing.jdgui.views.settings.components.SettingsComponent;
import jd.gui.swing.jdgui.views.settings.components.StateUpdateListener;
import net.miginfocom.swing.MigLayout;

import org.appwork.app.gui.MigPanel;

public class BasicAuthenticationPanel extends JPanel implements SettingsComponent {
    private static final long                     serialVersionUID = 1L;
    private static final BasicAuthenticationPanel INSTANCE         = new BasicAuthenticationPanel();

    /**
     * get the only existing instance of AccountManager. This is a singleton
     * 
     * @return
     */
    public static BasicAuthenticationPanel getInstance() {
        return BasicAuthenticationPanel.INSTANCE;
    }

    private MigPanel  tb;
    private AuthTable table;

    /**
     * Create a new instance of AccountManager. This is a singleton class.
     * Access the only existing instance by using {@link #getInstance()}.
     */
    private BasicAuthenticationPanel() {
        super(new MigLayout("ins 0,wrap 1", "[grow,fill]", "[grow,fill][]"));

        tb = new MigPanel("ins 0", "[][][grow,fill]", "");

        table = new AuthTable();
        tb.add(new JButton(new NewAction(table)), "sg 1");
        tb.add(new JButton(new RemoveAction(table)), "sg 1");

        add(new JScrollPane(table));
        add(tb);

    }

    public void addStateUpdateListener(StateUpdateListener listener) {
        throw new IllegalStateException("Not implemented");
    }

    public String getConstraints() {
        return "wmin 10,height 60:n:n,pushy,growy";
    }

    public boolean isMultiline() {
        return false;
    }

    public void update() {
        ((AuthTableModel) table.getExtTableModel()).update();
    }
}
