package jd.gui.swing.jdgui.views.settings.panels.basicauthentication;

import javax.swing.AbstractAction;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import jd.controlling.IOEQ;
import jd.controlling.authentication.AuthenticationController;
import jd.gui.swing.jdgui.views.settings.components.SettingsComponent;
import jd.gui.swing.jdgui.views.settings.components.StateUpdateListener;
import net.miginfocom.swing.MigLayout;

import org.appwork.app.gui.MigPanel;
import org.appwork.swing.components.ExtButton;
import org.appwork.swing.exttable.utils.MinimumSelectionObserver;
import org.jdownloader.images.NewTheme;

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
        NewAction na;
        tb.add(new ExtButton(na = new NewAction(table)), "sg 1,height 26!");
        na.putValue(AbstractAction.SMALL_ICON, NewTheme.I().getIcon("add", 20));
        RemoveAction ra;
        tb.add(new ExtButton(ra = new RemoveAction(table)), "sg 1,height 26!");
        table.getSelectionModel().addListSelectionListener(new MinimumSelectionObserver(table, ra, 1));
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
        IOEQ.add(new Runnable() {

            public void run() {
                table.getExtTableModel()._fireTableStructureChanged(AuthenticationController.getInstance().list(), false);
            }

        }, true);
    }
}
