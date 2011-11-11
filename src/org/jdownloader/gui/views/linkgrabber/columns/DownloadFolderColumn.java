package org.jdownloader.gui.views.linkgrabber.columns;

import java.awt.event.ActionEvent;
import java.awt.event.FocusEvent;
import java.awt.event.MouseEvent;
import java.io.File;

import javax.swing.JTextField;

import jd.controlling.linkcrawler.CrawledPackage;
import jd.controlling.packagecontroller.AbstractNode;
import net.miginfocom.swing.MigLayout;

import org.appwork.swing.action.BasicAction;
import org.appwork.swing.components.ExtButton;
import org.appwork.swing.exttable.columns.ExtTextColumn;
import org.appwork.utils.os.CrossSystem;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.linkgrabber.contextmenu.OpenDownloadFolderAction;
import org.jdownloader.gui.views.linkgrabber.contextmenu.SetDownloadFolderAction;
import org.jdownloader.images.NewTheme;

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
                SetDownloadFolderAction sna = new SetDownloadFolderAction(editing);
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
                new OpenDownloadFolderAction(editing).actionPerformed(e);
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
        if (object instanceof CrawledPackage && value != null) {
            File file = new File(value);
            if (SetDownloadFolderAction.isDownloadFolderValid(file)) ((CrawledPackage) object).setDownloadFolder(value);
        }
    }

    @Override
    public String getStringValue(AbstractNode value) {
        if (value instanceof CrawledPackage) {
            return ((CrawledPackage) value).getDownloadFolder();
        } else {
            return null;
        }
    }

}
