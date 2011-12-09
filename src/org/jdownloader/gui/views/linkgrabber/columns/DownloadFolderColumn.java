package org.jdownloader.gui.views.linkgrabber.columns;

import java.awt.event.ActionEvent;
import java.awt.event.FocusEvent;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.ArrayList;

import javax.swing.JTextField;

import jd.controlling.linkcrawler.CrawledPackage;
import jd.controlling.packagecontroller.AbstractNode;
import net.miginfocom.swing.MigLayout;

import org.appwork.swing.action.BasicAction;
import org.appwork.swing.components.ExtButton;
import org.appwork.swing.exttable.columns.ExtTextColumn;
import org.appwork.utils.StringUtils;
import org.appwork.utils.os.CrossSystem;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.linkgrabber.contextmenu.OpenDownloadFolderAction;
import org.jdownloader.gui.views.linkgrabber.contextmenu.SetDownloadFolderAction;
import org.jdownloader.images.NewTheme;
import org.jdownloader.settings.GeneralSettings;

public class DownloadFolderColumn extends ExtTextColumn<AbstractNode> {
    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    private AbstractNode      editing;
    private ExtButton         open;

    public DownloadFolderColumn() {
        super(_GUI._.LinkGrabberTableModel_initColumns_folder());
        setClickcount(2);

        editorField.setBorder(new JTextField().getBorder());
        ExtButton bt = new ExtButton(new BasicAction() {
            /**
             * 
             */
            private static final long serialVersionUID = 5187588042473800725L;

            {
                setSmallIcon(NewTheme.I().getIcon("edit", 16));
                setTooltipText(_GUI._.DownloadFolderColiumn_edit_tt_());
            }

            public void actionPerformed(ActionEvent e) {
                editorField.selectAll();
                noset = true;
                SetDownloadFolderAction sna = new SetDownloadFolderAction(editing, null);
                sna.actionPerformed(null);
                if (sna.newValueSet()) {
                    DownloadFolderColumn.this.stopCellEditing();
                } else {
                    noset = false;
                }
            }
        });
        open = new ExtButton(new BasicAction() {
            /**
             * 
             */
            private static final long serialVersionUID = -2832597849544070872L;

            {
                setSmallIcon(NewTheme.I().getIcon("load", 16));
                setTooltipText(_GUI._.DownloadFolderColiumn_open_tt_());
            }

            public void actionPerformed(ActionEvent e) {
                noset = true;
                new OpenDownloadFolderAction((ArrayList<CrawledPackage>) editing).actionPerformed(e);
                DownloadFolderColumn.this.stopCellEditing();
            }
        });
        // bt.setRolloverEffectEnabled(true);
        editor.setLayout(new MigLayout("ins 1 4 1 0", "[grow,fill][][]", "[fill,grow]"));
        editor.removeAll();
        editor.add(this.editorField, "height 20!");
        editor.add(bt, "height 20!,width 20!");
        editor.add(open, "height 20!,width 20!");

    }

    @Override
    public void configureEditorComponent(AbstractNode value, boolean isSelected, int row, int column) {
        super.configureEditorComponent(value, isSelected, row, column);
        this.editing = value;
        noset = false;
        boolean enabled = false;
        if (CrossSystem.isOpenFileSupported() && value != null && value instanceof CrawledPackage) {
            File file = new File(((CrawledPackage) value).getDownloadFolder());
            if (file.exists() && file.isDirectory()) enabled = true;
        }
        open.setEnabled(enabled);
    }

    @Override
    public void focusGained(final FocusEvent e) {

    }

    @Override
    public boolean isEditable(AbstractNode obj) {
        return obj instanceof CrawledPackage;
    }

    @Override
    protected void onSingleClick(MouseEvent e, AbstractNode obj) {
        super.onSingleClick(e, obj);
    }

    @Override
    protected void onDoubleClick(MouseEvent e, AbstractNode obj) {

    }

    @Override
    public boolean isSortable(AbstractNode obj) {
        return true;
    }

    @Override
    protected void setStringValue(String value, AbstractNode object) {
        if (StringUtils.isEmpty(value)) return;
        if (object instanceof CrawledPackage) {
            ((CrawledPackage) object).setDownloadFolder(value);
        }
    }

    @Override
    public boolean isEnabled(final AbstractNode obj) {
        if (obj instanceof CrawledPackage) { return ((CrawledPackage) obj).getView().isEnabled(); }
        return obj.isEnabled();
    }

    protected boolean isEditable(final AbstractNode obj, final boolean enabled) {

        return isEditable(obj);
    }

    @Override
    public String getStringValue(AbstractNode value) {
        if (value instanceof CrawledPackage) {
            String folder = ((CrawledPackage) value).getDownloadFolder();
            if (isAbsolute(folder)) {
                return folder;
            } else {
                return new File(GeneralSettings.DEFAULT_DOWNLOAD_FOLDER.getValue(), folder).toString();
            }
        } else {
            return null;
        }
    }

    private boolean isAbsolute(String path) {
        if (StringUtils.isEmpty(path)) return false;
        if (CrossSystem.isWindows() && path.matches(".://.+")) return true;
        if (CrossSystem.isWindows() && path.matches(".:\\\\.+")) return true;
        if (path.startsWith("/")) return true;
        return false;
    }

}
