package jd.gui.swing.jdgui.views.settings.panels.accountmanager;

import javax.swing.AbstractAction;
import javax.swing.JScrollPane;

import org.appwork.swing.MigPanel;
import org.appwork.swing.components.ExtButton;
import org.appwork.swing.exttable.utils.MinimumSelectionObserver;
import org.jdownloader.gui.IconKey;
import org.jdownloader.images.AbstractIcon;

import jd.gui.swing.jdgui.interfaces.SwitchPanel;
import jd.plugins.Account;
import net.miginfocom.swing.MigLayout;

public class AccountListPanel extends SwitchPanel {

    private MigPanel            tb;
    private PremiumAccountTable table;
    private ExtButton           newButton;

    private ExtButton           removeButton;

    private ExtButton           buyButton;

    private ExtButton           refreshButton;

    public AccountListPanel(AccountManager accountManager) {
        super(new MigLayout("ins 0,wrap 1", "[grow,fill]", "[grow,fill][]"));

        table = new PremiumAccountTable(this);

        tb = new MigPanel("ins 0", "[][][][][grow,fill]", "");
        tb.setOpaque(false);

        NewAction na;
        tb.add(newButton = new ExtButton(na = new NewAction()), "sg 1,height 26!");
        na.putValue(AbstractAction.SMALL_ICON, new AbstractIcon(IconKey.ICON_ADD, 20));
        RemoveAction ra;
        tb.add(removeButton = new ExtButton(ra = new RemoveAction(table)), "sg 1,height 26!");
        table.getSelectionModel().addListSelectionListener(new MinimumSelectionObserver(table, ra, 1));

        tb.add(buyButton = new ExtButton(new BuyAction(table)), "sg 2,height 26!");
        tb.add(refreshButton = new ExtButton(new RefreshAction()), "sg 2,height 26!");
        add(new JScrollPane(table));
        add(tb);
    }

    @Override
    protected void onShow() {
    }

    @Override
    protected void onHide() {
    }

    public void selectAccount(Account account) {
        for (AccountEntry ae : table.getModel().getTableData()) {
            if (account.equals(ae.getAccount())) {
                table.getModel().setSelectedObject(ae);
            }
        }
    }

}
