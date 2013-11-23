package org.jdownloader.gui.views.downloads.columns;

import java.awt.event.FocusEvent;
import java.awt.event.MouseEvent;
import java.io.File;

import javax.swing.JPopupMenu;
import javax.swing.JTextField;

import jd.controlling.packagecontroller.AbstractNode;
import jd.controlling.packagecontroller.AbstractPackageNode;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import net.miginfocom.swing.MigLayout;

import org.appwork.swing.exttable.columns.ExtTextColumn;
import org.appwork.utils.os.CrossSystem;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.SelectionInfo;
import org.jdownloader.gui.views.components.packagetable.LinkTreeUtils;
import org.jdownloader.gui.views.downloads.action.SetDownloadFolderInDownloadTableAction;

public class DownloadFolderColumn extends ExtTextColumn<AbstractNode> {
    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    public JPopupMenu createHeaderPopup() {

        return FileColumn.createColumnPopup(this, getMinWidth() == getMaxWidth() && getMaxWidth() > 0);

    }

    public DownloadFolderColumn() {
        super(_GUI._.LinkGrabberTableModel_initColumns_folder());
        setClickcount(0);

        editorField.setBorder(new JTextField().getBorder());
        // bt.setRolloverEffectEnabled(true);
        editor.setLayout(new MigLayout("ins 1 4 1 0", "[grow,fill][][]", "[fill,grow]"));
        editor.removeAll();
        editor.add(this.editorField, "height 20!");

    }

    @Override
    public void focusGained(final FocusEvent e) {
    }

    @Override
    public boolean isEditable(AbstractNode obj) {
        return false;
    }

    @Override
    public boolean onRenameClick(MouseEvent e, AbstractNode clicked) {
        // JDGui.help(_GUI._.literall_usage_tipp(), _GUI._.DownloadFolderColumn_onSingleClick_object_(), NewTheme.I().getIcon("smart", 48));

        new SetDownloadFolderInDownloadTableAction(new SelectionInfo<FilePackage, DownloadLink>(clicked, null)).actionPerformed(null);
        return true;
    }

    @Override
    public boolean onDoubleClick(MouseEvent e, AbstractNode value) {

        File ret = LinkTreeUtils.getDownloadDirectory(value);
        if (ret != null && ret.exists() && ret.isDirectory()) {

            CrossSystem.openFile(ret);

        }
        return true;

    }

    @Override
    public boolean onSingleClick(MouseEvent e, AbstractNode obj) {

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
