package jd.gui.swing.jdgui.views.settings.panels.accountmanager.orderpanel.dialog;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComponent;

import jd.gui.swing.jdgui.views.settings.panels.accountmanager.orderpanel.AccountInterface;
import jd.gui.swing.jdgui.views.settings.panels.accountmanager.orderpanel.AccountWrapper;
import jd.gui.swing.jdgui.views.settings.panels.accountmanager.orderpanel.GroupWrapper;

import org.appwork.swing.exttable.columns.ExtComboColumn;
import org.appwork.utils.Application;
import org.appwork.utils.swing.renderer.RendererMigPanel;
import org.jdownloader.controlling.hosterrule.AccountGroup.Rules;
import org.jdownloader.gui.translate._GUI;

public class GroupRuleColumn extends ExtComboColumn<AccountInterface, Rules> {
    private final JComponent empty = new RendererMigPanel("ins 0", "[]", "[]");

    public GroupRuleColumn() {
        super(_GUI.T.GroupRuleColumn_GroupRuleColumn_distrubutionrule_(), new DefaultComboBoxModel<Rules>(getRules()));
    }

    private static Rules[] getRules() {
        final List<Rules> ret = new ArrayList<Rules>(Arrays.asList(Rules.values()));
        if (Application.isJared(null)) {
            ret.remove(Rules.BALANCED);
        }
        return ret.toArray(new Rules[0]);
    }

    protected String modelItemToString(final Rules selectedItem) {
        if (selectedItem == null) {
            return "";
        } else {
            return selectedItem.translate();
        }
    }

    @Override
    protected Rules getSelectedItem(AccountInterface object) {
        if (object instanceof GroupWrapper) {
            final GroupWrapper group = ((GroupWrapper) object);
            return group.getRule();
        } else {
            return null;
        }
    }

    @Override
    public JComponent getRendererComponent(AccountInterface value, boolean isSelected, boolean hasFocus, int row, int column) {
        if (value instanceof AccountWrapper) {
            return empty;
        } else {
            final JComponent ret = super.getRendererComponent(value, isSelected, hasFocus, row, column);
            return ret;
        }
    }

    @Override
    protected void setSelectedItem(AccountInterface object, Rules value) {
        if (object instanceof GroupWrapper) {
            GroupWrapper group = ((GroupWrapper) object);
            group.setRule(value);
        }
    }
}
