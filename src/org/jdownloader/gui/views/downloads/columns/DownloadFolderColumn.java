package org.jdownloader.gui.views.downloads.columns;

import java.awt.event.FocusEvent;
import java.awt.event.MouseEvent;
import java.io.File;

import javax.swing.JTextField;

import jd.controlling.packagecontroller.AbstractNode;
import jd.controlling.packagecontroller.AbstractPackageNode;
import jd.gui.swing.jdgui.JDGui;
import net.miginfocom.swing.MigLayout;

import org.appwork.scheduler.DelayedRunnable;
import org.appwork.swing.components.tooltips.ToolTipController;
import org.appwork.swing.exttable.columns.ExtTextColumn;
import org.appwork.utils.os.CrossSystem;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.components.packagetable.LinkTreeUtils;
import org.jdownloader.gui.views.downloads.context.SetDownloadFolderInDownloadTableAction;
import org.jdownloader.images.NewTheme;

public class DownloadFolderColumn extends ExtTextColumn<AbstractNode> {
    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    private DelayedRunnable   clickDelayer;
    private AbstractNode      clicked;

    public DownloadFolderColumn() {
        super(_GUI._.LinkGrabberTableModel_initColumns_folder());
        setClickcount(2);
        editorField.setBorder(new JTextField().getBorder());
        // bt.setRolloverEffectEnabled(true);
        editor.setLayout(new MigLayout("ins 1 4 1 0", "[grow,fill][][]", "[fill,grow]"));
        editor.removeAll();
        editor.add(this.editorField, "height 20!");
        clickDelayer = new DelayedRunnable(ToolTipController.EXECUTER, 200) {
            @Override
            public void delayedrun() {
                new SetDownloadFolderInDownloadTableAction(clicked, null).actionPerformed(null);
            }
        };
    }

    @Override
    public void focusGained(final FocusEvent e) {
    }

    @Override
    public boolean isEditable(AbstractNode obj) {
        return false;
    }

    @Override
    protected boolean onDoubleClick(MouseEvent e, AbstractNode value) {
        if (CrossSystem.isOpenFileSupported() && value != null && clickDelayer.stop()) {

            File ret = LinkTreeUtils.getDownloadDirectory(value);
            if (ret != null && ret.exists() && ret.isDirectory()) CrossSystem.openFile(ret);
            return true;
        }
        return false;
    }

    @Override
    protected boolean onSingleClick(MouseEvent e, AbstractNode obj) {
        clicked = obj;
        clickDelayer.resetAndStart();
        JDGui.help(_GUI._.literall_usage_tipp(), _GUI._.DownloadFolderColumn_onSingleClick_object_(), NewTheme.I().getIcon("smart", 48));
        return false;
    }

    @Override
    public boolean isSortable(AbstractNode obj) {
        return true;
    }

    @Override
    public boolean isEnabled(final AbstractNode obj) {
        if (obj instanceof AbstractPackageNode) { return ((AbstractPackageNode) obj).getView().isEnabled(); }
        return obj.isEnabled();
    }

    protected boolean isEditable(final AbstractNode obj, final boolean enabled) {
        /* needed so we can edit even is row is disabled */
        return isEditable(obj);
    }

    @Override
    public String getStringValue(AbstractNode value) {
        File ret = LinkTreeUtils.getDownloadDirectory(value);
        if (ret != null) return ret.toString();
        return null;
    }

}
