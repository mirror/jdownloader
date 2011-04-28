package jd.gui.swing.jdgui.views.settings.panels.accountmanager;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JToolBar;

import jd.gui.swing.jdgui.views.settings.components.SettingsComponent;
import net.miginfocom.swing.MigLayout;

public class AccountManager extends JPanel implements SettingsComponent {
    private static final AccountManager INSTANCE = new AccountManager();

    /**
     * get the only existing instance of AccountManager. This is a singleton
     * 
     * @return
     */
    public static AccountManager getInstance() {
        return AccountManager.INSTANCE;
    }

    private JToolBar            tb;
    private PremiumAccountTable table;

    /**
     * Create a new instance of AccountManager. This is a singleton class.
     * Access the only existing instance by using {@link #getInstance()}.
     */
    private AccountManager() {
        super(new MigLayout("ins 0,wrap 1", "[grow,fill]", "[][grow,fill][]"));

        tb = new JToolBar();
        tb.setFloatable(false);
        table = new PremiumAccountTable();
        tb.add(new JButton(new NewAction(table)));
        tb.add(new JButton(new RemoveAction(table)));
        tb.add(new JButton(new BuyAction(table)));
        tb.add(new JButton(new RefreshAction(table)));
        add(tb);
        add(new JScrollPane(table));

    }

    public String getConstraints() {
        return "wmin 10,height 60:n:n";
    }

    public boolean isMultiline() {
        return false;
    }
}
