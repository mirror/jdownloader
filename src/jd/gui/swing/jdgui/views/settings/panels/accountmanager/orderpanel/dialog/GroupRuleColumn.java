package jd.gui.swing.jdgui.views.settings.panels.accountmanager.orderpanel.dialog;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComponent;

import jd.gui.swing.jdgui.views.settings.panels.accountmanager.orderpanel.AccountInterface;
import jd.gui.swing.jdgui.views.settings.panels.accountmanager.orderpanel.AccountWrapper;
import jd.gui.swing.jdgui.views.settings.panels.accountmanager.orderpanel.GroupWrapper;

import org.appwork.swing.exttable.columns.ExtComboColumn;
import org.appwork.utils.swing.renderer.RendererMigPanel;
import org.jdownloader.controlling.hosterrule.AccountGroup.Rules;
import org.jdownloader.gui.translate._GUI;

public class GroupRuleColumn extends ExtComboColumn<AccountInterface, Rules> {
    private final JComponent empty = new RendererMigPanel("ins 0", "[]", "[]");

    public GroupRuleColumn() {
        super(_GUI._.GroupRuleColumn_GroupRuleColumn_distrubutionrule_(), new DefaultComboBoxModel<Rules>(Rules.values()));
    }

    protected String modelItemToString(final Rules selectedItem) {
        if (selectedItem == null) return "";

        return selectedItem.translate();

    }

    @Override
    protected Rules getSelectedItem(AccountInterface object) {
        if (object instanceof GroupWrapper) {
            GroupWrapper group = ((GroupWrapper) object);
            return group.getRule();
        }
        return null;
    }

    @Override
    public JComponent getRendererComponent(AccountInterface value, boolean isSelected, boolean hasFocus, int row, int column) {
        if (value instanceof AccountWrapper) { return empty; }
        JComponent ret = super.getRendererComponent(value, isSelected, hasFocus, row, column);

        return ret;
    }

    @Override
    protected void setSelectedItem(AccountInterface object, Rules value) {
        if (object instanceof GroupWrapper) {
            GroupWrapper group = ((GroupWrapper) object);
            group.setRule(value);
        }
    }

}
