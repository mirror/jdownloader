package jd.gui.swing.jdgui.views.settings.panels.accountmanager;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import jd.gui.swing.jdgui.views.settings.components.SettingsComponent;
import jd.gui.swing.jdgui.views.settings.components.StateUpdateListener;
import net.miginfocom.swing.MigLayout;

import org.appwork.app.gui.MigPanel;

public class AccountManager extends JPanel implements SettingsComponent {
    private static final long           serialVersionUID = 1473756660999062848L;
    private static final AccountManager INSTANCE         = new AccountManager();

    /**
     * get the only existing instance of AccountManager. This is a singleton
     * 
     * @return
     */
    public static AccountManager getInstance() {
        return AccountManager.INSTANCE;
    }

    private MigPanel            tb;
    private PremiumAccountTable table;

    /**
     * Create a new instance of AccountManager. This is a singleton class.
     * Access the only existing instance by using {@link #getInstance()}.
     */
    private AccountManager() {
        super(new MigLayout("ins 0,wrap 1", "[grow,fill]", "[grow,fill][][]"));

        tb = new MigPanel("ins 0", "[][][][][grow,fill]", "");

        table = new PremiumAccountTable();
        tb.add(new JButton(new NewAction(table)), "sg 1");
        tb.add(new JButton(new RemoveAction(table)), "sg 1");
        tb.add(new JButton(new BuyAction(null, table)), "sg 1");
        tb.add(new JButton(new RefreshAction(table)), "sg 1");

        add(new JScrollPane(table));
        add(tb);

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
