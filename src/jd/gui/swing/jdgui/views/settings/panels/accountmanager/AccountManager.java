package jd.gui.swing.jdgui.views.settings.panels.accountmanager;

import javax.swing.AbstractAction;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import jd.gui.swing.jdgui.views.settings.components.SettingsComponent;
import jd.gui.swing.jdgui.views.settings.components.StateUpdateListener;
import net.miginfocom.swing.MigLayout;

import org.appwork.swing.MigPanel;
import org.appwork.swing.components.ExtButton;
import org.appwork.swing.exttable.utils.MinimumSelectionObserver;
import org.jdownloader.images.NewTheme;

public class AccountManager extends JPanel implements SettingsComponent {
    private static final long   serialVersionUID = 1473756660999062848L;

    private MigPanel            tb;
    private PremiumAccountTable table;

    private ExtButton           newButton;

    private ExtButton           removeButton;

    private ExtButton           buyButton;

    private ExtButton           refreshButton;

    /**
     * Create a new instance of AccountManager. This is a singleton class. Access the only existing instance by using {@link #getInstance()}
     * .
     * 
     * @param accountManagerSettings
     */
    public AccountManager(AccountManagerSettings accountManagerSettings) {
        super(new MigLayout("ins 0,wrap 1", "[grow,fill]", "[grow,fill][]"));

        tb = new MigPanel("ins 0", "[][][][][grow,fill]", "");
        tb.setOpaque(false);
        setOpaque(false);
        table = new PremiumAccountTable(accountManagerSettings);

        NewAction na;
        tb.add(newButton = new ExtButton(na = new NewAction()), "sg 1,height 26!");
        na.putValue(AbstractAction.SMALL_ICON, NewTheme.I().getIcon("add", 20));
        RemoveAction ra;
        tb.add(removeButton = new ExtButton(ra = new RemoveAction(table)), "sg 1,height 26!");
        table.getSelectionModel().addListSelectionListener(new MinimumSelectionObserver(table, ra, 1));

        tb.add(buyButton = new ExtButton(new BuyAction(table)), "sg 2,height 26!");
        tb.add(refreshButton = new ExtButton(new RefreshAction()), "sg 2,height 26!");

        add(new JScrollPane(table));
        add(tb, "");

    }

    public void setEnabled(boolean enabled) {
        refreshButton.setEnabled(enabled);
        buyButton.setEnabled(enabled);
        removeButton.setEnabled(enabled);
        newButton.setEnabled(enabled);
        table.setEnabled(enabled);
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
