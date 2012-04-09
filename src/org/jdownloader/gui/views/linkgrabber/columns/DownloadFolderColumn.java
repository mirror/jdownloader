package org.jdownloader.gui.views.linkgrabber.columns;

import java.awt.event.ActionEvent;
import java.awt.event.FocusEvent;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.ArrayList;

import javax.swing.JTextField;

import jd.controlling.IOEQ;
import jd.controlling.linkcollector.LinkCollector;
import jd.controlling.linkcollector.LinknameCleaner;
import jd.controlling.linkcollector.VariousCrawledPackage;
import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.CrawledPackage;
import jd.controlling.packagecontroller.AbstractNode;
import net.miginfocom.swing.MigLayout;

import org.appwork.swing.action.BasicAction;
import org.appwork.swing.components.ExtButton;
import org.appwork.swing.exttable.columns.ExtTextColumn;
import org.appwork.utils.StringUtils;
import org.appwork.utils.event.queue.QueueAction;
import org.appwork.utils.os.CrossSystem;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.linkgrabber.LinkTreeUtils;
import org.jdownloader.gui.views.linkgrabber.contextmenu.SetDownloadFolderAction;
import org.jdownloader.images.NewTheme;
import org.jdownloader.translate._JDT;

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

                CrossSystem.openFile(LinkTreeUtils.getDownloadDirectory(editing));

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
        } else if (CrossSystem.isOpenFileSupported() && value != null && value instanceof CrawledLink) {
            value = ((CrawledLink) value).getParentNode();
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
        return true;
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
        } else if (object instanceof CrawledLink) {

            CrawledPackage p = ((CrawledLink) object).getParentNode();
            if (new File(value).equals(new File(p.getDownloadFolder()))) return;
            if (!(p instanceof VariousCrawledPackage)) {
                try {
                    if (p.getDownloadFolder().equals(value)) return;
                    Dialog.getInstance().showConfirmDialog(Dialog.LOGIC_DONOTSHOW_BASED_ON_TITLE_ONLY | Dialog.STYLE_SHOW_DO_NOT_DISPLAY_AGAIN,

                    _JDT._.SetDownloadFolderAction_actionPerformed_(p.getName()), _JDT._.SetDownloadFolderAction_msg(p.getName(), 1), null, _JDT._.SetDownloadFolderAction_yes(), _JDT._.SetDownloadFolderAction_no());
                    p.setDownloadFolder(value);
                    LinkCollector.getInstance().refreshData();
                    return;
                } catch (DialogClosedException e) {
                    return;
                } catch (DialogCanceledException e) {

                }
            }
            final CrawledPackage pkg = new CrawledPackage();
            pkg.setExpanded(true);
            pkg.setCreated(System.currentTimeMillis());
            if (p instanceof VariousCrawledPackage) {
                pkg.setName(LinknameCleaner.cleanFileName(object.getName()));
            } else {
                pkg.setName(p.getName());
            }
            pkg.setDownloadFolder(value);
            final ArrayList<CrawledLink> links = new ArrayList<CrawledLink>();
            links.add((CrawledLink) object);
            IOEQ.getQueue().add(new QueueAction<Object, RuntimeException>(org.appwork.utils.event.queue.Queue.QueuePriority.HIGH) {

                @Override
                protected Object run() {
                    LinkCollector.getInstance().addmoveChildren(pkg, links, -1);
                    return null;
                }

            });

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
                return new File(org.jdownloader.settings.staticreferences.CFG_GENERAL.DEFAULT_DOWNLOAD_FOLDER.getValue(), folder).toString();
            }
        } else if (value instanceof CrawledLink) {
            value = ((CrawledLink) value).getParentNode();
            if (value != null) {
                String folder = ((CrawledPackage) value).getDownloadFolder();
                if (isAbsolute(folder)) {
                    return folder;
                } else {
                    return new File(org.jdownloader.settings.staticreferences.CFG_GENERAL.DEFAULT_DOWNLOAD_FOLDER.getValue(), folder).toString();
                }
            }
        }

        return null;

    }

    private boolean isAbsolute(String path) {
        if (StringUtils.isEmpty(path)) return false;

        if (CrossSystem.isWindows() && path.matches(".:/.*")) return true;
        if (CrossSystem.isWindows() && path.matches(".:\\\\.*")) return true;
        if (path.startsWith("/")) return true;
        return false;
    }

}
