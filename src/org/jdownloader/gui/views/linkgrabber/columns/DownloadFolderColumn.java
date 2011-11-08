package org.jdownloader.gui.views.linkgrabber.columns;

import java.awt.event.ActionEvent;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JTextField;

import jd.controlling.linkcrawler.CrawledPackage;
import jd.controlling.packagecontroller.AbstractNode;
import net.miginfocom.swing.MigLayout;

import org.appwork.swing.action.BasicAction;
import org.appwork.swing.components.ExtButton;
import org.appwork.swing.exttable.columns.ExtTextColumn;
import org.appwork.utils.Application;
import org.appwork.utils.os.CrossSystem;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.Dialog.FileChooserSelectionMode;
import org.appwork.utils.swing.dialog.Dialog.FileChooserType;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;
import org.jdownloader.settings.GeneralSettings;

public class DownloadFolderColumn extends ExtTextColumn<AbstractNode> {
    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    private AbstractNode      editing;

    public DownloadFolderColumn() {
        super(_GUI._.LinkGrabberTableModel_initColumns_folder());
        setClickcount(2);
        // setFolder

        editorField.addMouseListener(new MouseAdapter() {

            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() >= 2) {
                    editorField.selectAll();
                    try {
                        Dialog.getInstance().showFileChooser("test", "Choose file", FileChooserSelectionMode.FILES_AND_DIRECTORIES, null, false, FileChooserType.OPEN_DIALOG, null);
                    } catch (DialogCanceledException e1) {
                        // TODO Auto-generated catch block
                        e1.printStackTrace();
                    } catch (DialogClosedException e1) {
                        // TODO Auto-generated catch block
                        e1.printStackTrace();
                    }
                }
            }
        });

        editorField.setBorder(new JTextField().getBorder());
        ExtButton bt = new ExtButton(new BasicAction() {
            {
                setSmallIcon(NewTheme.I().getIcon("edit", 16));
                setTooltipText(_GUI._.DownloadFolderColiumn_edit_tt_());
            }

            public void actionPerformed(ActionEvent e) {
                editorField.selectAll();
                try {
                    Dialog.getInstance().showFileChooser("test", "Choose file", FileChooserSelectionMode.FILES_AND_DIRECTORIES, null, false, FileChooserType.OPEN_DIALOG, null);
                } catch (DialogCanceledException e1) {
                    // TODO Auto-generated catch block
                    e1.printStackTrace();
                } catch (DialogClosedException e1) {
                    // TODO Auto-generated catch block
                    e1.printStackTrace();
                }
            }
        });
        ExtButton open = new ExtButton(new BasicAction() {
            {
                setSmallIcon(NewTheme.I().getIcon("load", 16));
                setTooltipText(_GUI._.DownloadFolderColiumn_open_tt_());
            }

            public void actionPerformed(ActionEvent e) {
                CrossSystem.openFile(Application.getResource(""));
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
        // set Folder
    }

    @Override
    public String getStringValue(AbstractNode value) {
        if (value instanceof CrawledPackage) {
            return GeneralSettings.DOWNLOAD_FOLDER.getValue();
        } else {
            return null;
        }
    }

}
