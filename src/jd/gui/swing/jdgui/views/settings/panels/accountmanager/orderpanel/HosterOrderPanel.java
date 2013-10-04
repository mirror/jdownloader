package jd.gui.swing.jdgui.views.settings.panels.accountmanager.orderpanel;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.AbstractAction;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import jd.controlling.AccountController;
import jd.controlling.AccountControllerEvent;
import jd.controlling.AccountControllerListener;
import jd.controlling.accountchecker.AccountChecker;
import jd.controlling.accountchecker.AccountCheckerEventListener;
import jd.gui.swing.jdgui.interfaces.SwitchPanel;
import jd.gui.swing.jdgui.views.settings.panels.accountmanager.AccountManager;
import jd.gui.swing.jdgui.views.settings.panels.accountmanager.BuyAction;
import jd.gui.swing.jdgui.views.settings.panels.accountmanager.PremiumAccountTable;
import jd.gui.swing.jdgui.views.settings.panels.accountmanager.RefreshAction;
import net.miginfocom.swing.MigLayout;

import org.appwork.swing.MigPanel;
import org.appwork.swing.components.ExtButton;
import org.appwork.swing.exttable.utils.MinimumSelectionObserver;
import org.appwork.utils.swing.EDTRunner;
import org.appwork.utils.swing.SwingUtils;
import org.jdownloader.controlling.hosterrule.AccountUsageRule;
import org.jdownloader.controlling.hosterrule.HosterRuleController;
import org.jdownloader.controlling.hosterrule.HosterRuleControllerListener;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;

public class HosterOrderPanel extends SwitchPanel implements ActionListener, AccountControllerListener, AccountCheckerEventListener, HosterRuleControllerListener {

    private HosterRuleTableModel model;
    private HosterRuleTable      table;
    private MigPanel             tb;
    private ExtButton            refreshButton;
    private ExtButton            buyButton;
    private ExtButton            newButton;
    private ExtButton            removeButton;

    public HosterOrderPanel(final AccountManager accountManager) {
        super(new MigLayout("ins 0, wrap 1", "[grow,fill]", "[][grow,fill][]"));

        model = new HosterRuleTableModel();
        table = new HosterRuleTable(model);

        JTextArea txt = new JTextArea();
        SwingUtils.setOpaque(txt, false);
        txt.setEditable(false);
        txt.setLineWrap(true);
        txt.setWrapStyleWord(true);
        txt.setFocusable(false);
        // txt.setEnabled(false);
        txt.setText(_GUI._.HosterOrderPanel_HosterOrderPanel_description_());

        HosterRuleController.getInstance().getEventSender().addListener(this, true);

        AccountController.getInstance().getBroadcaster().addListener(this, true);
        AccountChecker.getInstance().getEventSender().addListener(this, true);

        tb = new MigPanel("ins 0", "[][][][][grow,fill]", "");
        tb.setOpaque(false);

        NewRuleAction na;
        tb.add(newButton = new ExtButton(na = new NewRuleAction()), "sg 1,height 26!");
        na.putValue(AbstractAction.SMALL_ICON, NewTheme.I().getIcon("add", 20));
        RemoveAction ra;
        tb.add(removeButton = new ExtButton(ra = new RemoveAction(table)), "sg 1,height 26!");
        table.getSelectionModel().addListSelectionListener(new MinimumSelectionObserver(table, ra, 1));

        tb.add(buyButton = new ExtButton(new BuyAction((PremiumAccountTable) null)), "sg 2,height 26!");
        tb.add(refreshButton = new ExtButton(new RefreshAction()), "sg 2,height 26!");
        add(txt, "gaptop 0,spanx,growx,pushx,gapbottom 5,wmin 10");
        add(new JScrollPane(table));

        add(tb);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        // if (searchCombobox.getSelectedItem() == null) {
        // model.refresh(AccountController.getInstance().getPriority(null));
        // } else {
        // model.refresh(AccountController.getInstance().getPriority((DomainInfo) searchCombobox.getSelectedItem()));
        // }

    }

    @Override
    protected void onShow() {
        updateTable();
    }

    @Override
    protected void onHide() {

    }

    @Override
    public void onAccountControllerEvent(AccountControllerEvent event) {
        if (isShown()) {
            updateTable();
        }

    }

    protected void updateTable() {
        // HashSet<DomainInfo> domains = new HashSet<DomainInfo>();
        // HashSet<String> plugins = new HashSet<String>();
        // final AtomicBoolean refreshRequired = new AtomicBoolean();
        // for (Account acc : AccountController.getInstance().list()) {
        //
        // AccountInfo ai = acc.getAccountInfo();
        // if (ai != null) {
        // Object supported = ai.getProperty("multiHostSupport", Property.NULL);
        // if (supported != null && supported instanceof List) {
        // for (Object support : (List<?>) supported) {
        // if (support instanceof String) {
        //
        // LazyHostPlugin plg = HostPluginController.getInstance().get((String) support);
        // if (plg != null && plugins.add(plg.getClassname())) {
        // domains.add(DomainInfo.getInstance(plg.getHost()));
        // }
        // }
        // }
        // } else {
        // domains.add(DomainInfo.getInstance(acc.getHoster()));
        // }
        // } else {
        // refreshRequired.set(true);
        // }
        //
        // }
        //
        // final ArrayList<DomainInfo> lst = new ArrayList<DomainInfo>(domains);
        // Collections.sort(lst, new Comparator<DomainInfo>() {
        //
        // @Override
        // public int compare(DomainInfo o1, DomainInfo o2) {
        // return o1.getTld().compareTo(o2.getTld());
        // }
        // });
        // lst.add(0, null);
        new EDTRunner() {

            @Override
            protected void runInEDT() {
                model._fireTableStructureChanged(new ArrayList<AccountUsageRule>(HosterRuleController.getInstance().list()), true);
                // Object before = searchCombobox.getSelectedItem();
                // searchCombobox.setList(lst);
                // if (refreshRequired.get()) {
                // new RefreshAction().actionPerformed(null);
                // }
                // searchCombobox.setSelectedItem(before);
            }
        };

    }

    @Override
    public void onCheckStarted() {
    }

    @Override
    public void onCheckStopped() {
        if (isShown()) {
            updateTable();
        }
    }

    @Override
    public void onRuleAdded(AccountUsageRule parameter) {
        if (isShown()) {
            updateTable();
            HosterRuleController.getInstance().showEditPanel(parameter);
        }
    }

    @Override
    public void onRuleDataUpdate() {
        if (isShown()) {
            table.repaint();

        }
    }

    @Override
    public void onRuleRemoved(AccountUsageRule parameter) {
        if (isShown()) {
            updateTable();
        }
    }

}
