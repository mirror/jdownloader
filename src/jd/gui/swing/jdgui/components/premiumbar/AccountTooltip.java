package jd.gui.swing.jdgui.components.premiumbar;

import java.awt.Color;
import java.util.LinkedList;
import java.util.List;

import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

import jd.controlling.AccountController;
import jd.gui.swing.jdgui.views.settings.panels.accountmanager.AccountEntry;
import jd.plugins.Account;
import jd.plugins.AccountInfo;

import org.appwork.swing.components.tooltips.PanelToolTip;
import org.appwork.swing.components.tooltips.TooltipPanel;
import org.jdownloader.DomainInfo;
import org.jdownloader.updatev2.gui.LAFOptions;

public class AccountTooltip extends PanelToolTip {
    private Color                 color;
    private AccountListTable      table;
    private AccountListTableModel model;
    private PremiumStatus         owner;

    /**
     * @param owner
     * 
     */

    public AccountTooltip(PremiumStatus owner, DomainInfo domainInfo) {

        super(new TooltipPanel("ins 0,wrap 1", "[grow,fill]", "[][grow,fill]"));
        this.owner = owner;
        color = (LAFOptions.getInstance().getColorForTooltipForeground());
        List<Account> accs = AccountController.getInstance().list();

        final LinkedList<AccountEntry> domains = new LinkedList<AccountEntry>();
        for (Account acc : accs) {
            AccountInfo ai = acc.getAccountInfo();
            if (domainInfo == DomainInfo.getInstance(acc.getHoster())) {
                domains.add(new AccountEntry(acc));

            }

        }

        table = new AccountListTable(model = new AccountListTableModel(this));
        model.setData(domains);
        model.addTableModelListener(new TableModelListener() {

            @Override
            public void tableChanged(TableModelEvent e) {
                AccountTooltip.this.owner.redraw();
                table.getTableHeader().repaint();
            }
        });
        table.getTableHeader().setOpaque(false);
        panel.add(table.getTableHeader());
        panel.add(table);
        // panel.setPreferredSize(new Dimension(500, 100));
    }

    public void update() {

        table.getModel().fireTableStructureChanged();
    }
}
