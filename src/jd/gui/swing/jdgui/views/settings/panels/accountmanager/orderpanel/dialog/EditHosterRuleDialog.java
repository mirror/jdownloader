package jd.gui.swing.jdgui.views.settings.panels.accountmanager.orderpanel.dialog;

import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import jd.gui.swing.jdgui.JDGui;
import jd.gui.swing.jdgui.views.settings.panels.accountmanager.RefreshAction;
import jd.gui.swing.jdgui.views.settings.panels.accountmanager.orderpanel.AccountInterface;
import jd.gui.swing.jdgui.views.settings.panels.accountmanager.orderpanel.AccountWrapper;
import jd.gui.swing.jdgui.views.settings.panels.accountmanager.orderpanel.GroupWrapper;

import org.appwork.swing.MigPanel;
import org.appwork.swing.components.ExtButton;
import org.appwork.swing.exttable.tree.TreeNodeInterface;
import org.appwork.utils.locale._AWU;
import org.appwork.utils.swing.SwingUtils;
import org.appwork.utils.swing.dialog.AbstractDialog;
import org.jdownloader.controlling.hosterrule.AccountGroup;
import org.jdownloader.controlling.hosterrule.AccountReference;
import org.jdownloader.controlling.hosterrule.AccountUsageRule;
import org.jdownloader.controlling.hosterrule.FreeAccountReference;
import org.jdownloader.controlling.hosterrule.HosterRuleController;
import org.jdownloader.gui.translate._GUI;

public class EditHosterRuleDialog extends AbstractDialog<Integer> {

    private AccountUsageRule         rule;
    private HosterPriorityTableModel model;
    private HosterPriorityTable      table;

    public EditHosterRuleDialog(AccountUsageRule editing) {
        super(0, _GUI._.EditHosterRuleDialog_EditHosterRuleDialog_title_(editing.getHoster()), null, _AWU.T.lit_save(), null);
        this.rule = editing;
    }

    @Override
    protected int getPreferredHeight() {
        return Math.max(450, Math.min(table.getPreferredSize().height + 45, JDGui.getInstance().getMainFrame().getHeight()));
    }

    @Override
    protected int getPreferredWidth() {
        return Math.max(450, JDGui.getInstance().getMainFrame().getWidth() - 40);
    }

    @Override
    protected Integer createReturnValue() {

        return getReturnmask();

    }

    @Override
    public JComponent layoutDialogContent() {

        MigPanel ret = new MigPanel("ins 0, wrap 1", "[grow,fill]", "[][grow,fill][]");

        JTextArea txt = new JTextArea();
        SwingUtils.setOpaque(txt, false);
        txt.setEditable(false);
        txt.setLineWrap(true);
        txt.setWrapStyleWord(true);
        txt.setFocusable(false);
        // txt.setEnabled(false);
        txt.setText(_GUI._.EditHosterRuleDialog_layoutDialogContent_description_(rule.getHoster()));

        ret.add(txt, "gaptop 0,spanx,growx,pushx,gapbottom 5,wmin 10");

        model = new HosterPriorityTableModel();

        ArrayList<AccountInterface> ls = new ArrayList<AccountInterface>();
        for (AccountGroup a : rule.getAccounts()) {
            ls.add(new GroupWrapper(a));
        }
        model.setTreeData(ls, true);
        table = new HosterPriorityTable(model);
        ret.add(new JScrollPane(table));
        setMinimumSize(new Dimension(450, 300));
        MigPanel tb = new MigPanel("ins 0", "[][][][][grow,fill]", "");
        tb.setOpaque(false);
        ExtButton refreshButton;
        tb.add(refreshButton = new ExtButton(new RefreshAction()), "sg 2,height 26!");
        tb.add(new ExtButton(new AddGroupAction(model)), "sg 2,height 26!");

        ret.add(tb);
        return ret;
    }

    public AccountUsageRule getRule() {
        AccountUsageRule ret = new AccountUsageRule(rule.getHoster());
        ret.setEnabled(rule.isEnabled());
        ret.setOwner(HosterRuleController.getInstance());
        ArrayList<AccountGroup> accounts = new ArrayList<AccountGroup>();

        for (AccountInterface ai : model.getTree()) {
            if (ai instanceof GroupWrapper) {
                List<AccountReference> childs = new ArrayList<AccountReference>();
                for (TreeNodeInterface child : ((GroupWrapper) ai).getChildren()) {
                    if (FreeAccountReference.isFreeAccount(((AccountWrapper) child).getAccount())) {
                        AccountReference ar = new FreeAccountReference(rule.getHoster());
                        ar.setEnabled(((AccountWrapper) child).isEnabled());
                        childs.add(ar);
                    } else {
                        AccountReference ar = new AccountReference(((AccountWrapper) child).getAccount().getAccount());
                        ar.setEnabled(((AccountWrapper) child).isEnabled());
                        childs.add(ar);
                    }

                }

                AccountGroup group = new AccountGroup(childs);
                group.setName(((GroupWrapper) ai).getName());
                group.setRule(((GroupWrapper) ai).getRule());
                accounts.add(group);
            }
        }
        ret.setAccounts(accounts);
        return ret;
    }
}
