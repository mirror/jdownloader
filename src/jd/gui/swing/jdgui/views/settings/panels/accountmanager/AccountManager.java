package jd.gui.swing.jdgui.views.settings.panels.accountmanager;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import jd.gui.swing.jdgui.views.settings.components.SettingsComponent;
import jd.gui.swing.jdgui.views.settings.components.StateUpdateListener;
import net.miginfocom.swing.MigLayout;

import org.appwork.app.gui.MigPanel;
import org.appwork.utils.swing.table.utils.MinimumSelectionObserver;

public class AccountManager extends JPanel implements SettingsComponent {
    private static final long   serialVersionUID = 1473756660999062848L;

    private MigPanel            tb;
    private PremiumAccountTable table;

    /**
     * Create a new instance of AccountManager. This is a singleton class.
     * Access the only existing instance by using {@link #getInstance()}.
     * 
     * @param accountManagerSettings
     */
    public AccountManager(AccountManagerSettings accountManagerSettings) {
        super(new MigLayout("ins 0,wrap 1", "[grow,fill]", "[grow,fill][][]"));

        tb = new MigPanel("ins 0", "[][][][][grow,fill]", "");

        table = new PremiumAccountTable(accountManagerSettings);

        tb.add(new JButton(new NewAction(table)), "sg 1");
        RemoveAction ra;
        tb.add(new JButton(ra = new RemoveAction(table, false)), "sg 1");
        table.getSelectionModel().addListSelectionListener(new MinimumSelectionObserver(table, ra, 1));

        tb.add(new JButton(new BuyAction(null, table)), "sg 1");
        tb.add(new JButton(new RefreshAction(null)), "sg 1");

        add(new JScrollPane(table));
        add(tb);

    }

    /**
     * @return the table
     */
    public PremiumAccountTable getTable() {
        return table;
    }

    public void addStateUpdateListener(StateUpdateListener listener) {
        throw new IllegalStateException("Not implemented");
    }

    public String getConstraints() {
        return "wmin 10,height 60:n:n,growy,pushy";
    }

    public boolean isMultiline() {
        return false;
    }

}
