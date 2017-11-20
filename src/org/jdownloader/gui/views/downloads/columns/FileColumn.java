package org.jdownloader.gui.views.downloads.columns;

import java.awt.event.FocusEvent;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.Comparator;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;
import javax.swing.border.Border;

import jd.controlling.downloadcontroller.DownloadWatchDog;
import jd.controlling.linkcrawler.ArchiveCrawledPackage;
import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.CrawledPackage;
import jd.controlling.packagecontroller.AbstractNode;
import jd.controlling.packagecontroller.AbstractPackageChildrenNode;
import jd.controlling.packagecontroller.AbstractPackageNode;
import jd.gui.swing.jdgui.JDGui;
import jd.nutils.NaturalOrderComparator;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

import org.appwork.storage.config.ValidationException;
import org.appwork.storage.config.events.GenericConfigEventListener;
import org.appwork.storage.config.handler.KeyHandler;
import org.appwork.sunwrapper.sun.swing.SwingUtilities2Wrapper;
import org.appwork.swing.components.ExtMergedIcon;
import org.appwork.swing.exttable.ExtColumn;
import org.appwork.swing.exttable.ExtDefaultRowSorter;
import org.appwork.swing.exttable.columnmenu.LockColumnWidthAction;
import org.appwork.swing.exttable.columns.ExtTextColumn;
import org.appwork.utils.os.CrossSystem;
import org.jdownloader.extensions.ExtensionController;
import org.jdownloader.extensions.extraction.Archive;
import org.jdownloader.extensions.extraction.ExtractionExtension;
import org.jdownloader.extensions.extraction.bindings.crawledlink.CrawledLinkFactory;
import org.jdownloader.extensions.extraction.bindings.downloadlink.DownloadLinkArchiveFactory;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.components.packagetable.LinkTreeUtils;
import org.jdownloader.gui.views.components.packagetable.PackageControllerTableModel;
import org.jdownloader.gui.views.components.packagetable.PackageControllerTableModel.TOGGLEMODE;
import org.jdownloader.gui.views.components.packagetable.actions.SortPackagesDownloadOrdnerOnColumn;
import org.jdownloader.gui.views.downloads.action.OpenFileAction;
import org.jdownloader.images.NewTheme;
import org.jdownloader.settings.staticreferences.CFG_GUI;

public class FileColumn extends ExtTextColumn<AbstractNode> implements GenericConfigEventListener<Boolean> {
    /**
     *
     */
    private static final long serialVersionUID     = -2963955407564917958L;
    protected Border          leftGapBorder;
    private final Icon        iconPackageOpen;
    private final Icon        iconPackageClosed;
    private final Icon        iconArchive;
    private final Icon        iconArchiveOpen;
    protected Border          normalBorder;
    private boolean           selectAll            = false;
    private boolean           hideSinglePackage    = true;
    public final static int   EXPAND_COLLAPSE_AREA = 32 + 1/* leftGapBorder */+ 5 + 1/* super.defaultBorder */;

    public FileColumn() {
        super(_GUI.T.filecolumn_title());
        leftGapBorder = BorderFactory.createEmptyBorder(0, 32, 0, 0);
        normalBorder = BorderFactory.createEmptyBorder(0, 0, 0, 0);
        Icon open = NewTheme.I().getIcon(IconKey.ICON_MINUS, -1, false);
        Icon closed = NewTheme.I().getIcon(IconKey.ICON_PLUS, -1, false);
        iconPackageOpen = new ExtMergedIcon(open, 0, 0).add(NewTheme.I().getIcon(IconKey.ICON_PACKAGE_OPEN, 16), 16, 0);
        iconArchiveOpen = new ExtMergedIcon(open, 0, 0).add(NewTheme.I().getIcon(IconKey.ICON_RAR, 16), 16, 0);
        iconArchive = new ExtMergedIcon(closed, 0, 0).add(NewTheme.I().getIcon(IconKey.ICON_RAR, 16), 16, 0);
        iconPackageClosed = new ExtMergedIcon(closed, 0, 0).add(NewTheme.I().getIcon(IconKey.ICON_PACKAGE_CLOSED, 16), 16, 0);
        setClickcount(0);
        hideSinglePackage = CFG_GUI.HIDE_SINGLE_CHILD_PACKAGES.isEnabled();
        CFG_GUI.HIDE_SINGLE_CHILD_PACKAGES.getEventSender().addListener(this, true);
        this.setRowSorter(new ExtDefaultRowSorter<AbstractNode>() {
            private Comparator<String> comp = new NaturalOrderComparator();

            @Override
            public int compare(final AbstractNode o1, final AbstractNode o2) {
                String o1s = getStringValue(o1);
                String o2s = getStringValue(o2);
                if (o1s == null) {
                    o1s = "";
                }
                if (o2s == null) {
                    o2s = "";
                }
                if (this.getSortOrderIdentifier() == ExtColumn.SORT_ASC) {
                    return comp.compare(o1s, o2s);
                } else {
                    return comp.compare(o2s, o1s);
                }
            }
        });
    }

    /**
     * @return
     */
    public JPopupMenu createHeaderPopup() {
        return FileColumn.createColumnPopup(this, (getMinWidth() == getMaxWidth() && getMaxWidth() > 0));
    }

    public static JPopupMenu createColumnPopup(ExtColumn<AbstractNode> fileColumn, boolean isLocked) {
        final JPopupMenu ret = new JPopupMenu();
        LockColumnWidthAction action;
        boolean sepRequired = false;
        if (!isLocked && fileColumn.getModel().getTable().isColumnLockingFeatureEnabled()) {
            sepRequired = true;
            ret.add(new JCheckBoxMenuItem(action = new LockColumnWidthAction(fileColumn)));
        }
        if (fileColumn.isSortable(null)) {
            // if (sepRequired) {
            // ret.add(new JSeparator());
            // }
            sepRequired = true;
            ret.add(new SortPackagesDownloadOrdnerOnColumn(fileColumn));
            // ret.add(new SortPackagesAndLinksDownloadOrdnerOnColumn(this));
        }
        if (sepRequired) {
            ret.add(new JSeparator());
        }
        return ret;
    }

    @Override
    public boolean onDoubleClick(MouseEvent e, AbstractNode contextObject) {
        if (isExpandCollapseArea(e, contextObject)) {
            return false;
        }
        if (contextObject instanceof DownloadLink) {
            switch (CFG_GUI.CFG.getLinkDoubleClickAction()) {
            case NOTHING:
                java.awt.Toolkit.getDefaultToolkit().beep();
                break;
            case OPEN_FILE:
                if (CrossSystem.isOpenFileSupported()) {
                    new OpenFileAction(new File(((DownloadLink) contextObject).getFileOutput())).actionPerformed(null);
                }
                break;
            case OPEN_FOLDER:
                if (CrossSystem.isOpenFileSupported()) {
                    new OpenFileAction(LinkTreeUtils.getDownloadDirectory(((DownloadLink) contextObject).getParentNode())).actionPerformed(null);
                }
                break;
            case OPEN_PROPERTIES_PANEL:
                CFG_GUI.DOWNLOADS_TAB_PROPERTIES_PANEL_VISIBLE.setValue(CFG_GUI.DOWNLOADS_TAB_PROPERTIES_PANEL_VISIBLE.isEnabled() ? false : true);
                break;
            case RENAME:
                startEditing(contextObject);
                break;
            }
        } else if (contextObject instanceof CrawledLink) {
            switch (CFG_GUI.CFG.getLinkDoubleClickAction()) {
            case NOTHING:
                java.awt.Toolkit.getDefaultToolkit().beep();
                break;
            case OPEN_FILE:
                if (CrossSystem.isOpenFileSupported()) {
                    new OpenFileAction(LinkTreeUtils.getDownloadDirectory(contextObject)).actionPerformed(null);
                }
                break;
            case OPEN_FOLDER:
                if (CrossSystem.isOpenFileSupported()) {
                    new OpenFileAction(LinkTreeUtils.getDownloadDirectory(((CrawledLink) contextObject).getParentNode())).actionPerformed(null);
                }
                break;
            case OPEN_PROPERTIES_PANEL:
                CFG_GUI.LINKGRABBER_TAB_PROPERTIES_PANEL_VISIBLE.setValue(CFG_GUI.LINKGRABBER_TAB_PROPERTIES_PANEL_VISIBLE.isEnabled() ? false : true);
                break;
            case RENAME:
                startEditing(contextObject);
                break;
            }
        } else if (contextObject instanceof AbstractPackageNode) {
            switch (CFG_GUI.CFG.getPackageDoubleClickAction()) {
            case EXPAND_COLLAPSE_TOGGLE:
                if (e.isControlDown() && !e.isShiftDown()) {
                    ((PackageControllerTableModel) getModel()).toggleFilePackageExpand((AbstractPackageNode) contextObject, TOGGLEMODE.BOTTOM);
                } else if (e.isControlDown() && e.isShiftDown()) {
                    ((PackageControllerTableModel) getModel()).toggleFilePackageExpand((AbstractPackageNode) contextObject, TOGGLEMODE.TOP);
                } else {
                    ((PackageControllerTableModel) getModel()).toggleFilePackageExpand((AbstractPackageNode) contextObject, TOGGLEMODE.CURRENT);
                }
                break;
            case NOTHING:
                java.awt.Toolkit.getDefaultToolkit().beep();
                break;
            case OPEN_FOLDER:
                if (CrossSystem.isOpenFileSupported()) {
                    new OpenFileAction(LinkTreeUtils.getDownloadDirectory(contextObject)).actionPerformed(null);
                }
                break;
            case OPEN_PROPERTIES_PANEL:
                if (((AbstractPackageNode) contextObject).getControlledBy().equals(jd.controlling.downloadcontroller.DownloadController.getInstance())) {
                    CFG_GUI.DOWNLOADS_TAB_PROPERTIES_PANEL_VISIBLE.setValue(CFG_GUI.DOWNLOADS_TAB_PROPERTIES_PANEL_VISIBLE.isEnabled() ? false : true);
                } else if (((AbstractPackageNode) contextObject).getControlledBy().equals(jd.controlling.linkcollector.LinkCollector.getInstance())) {
                    CFG_GUI.LINKGRABBER_TAB_PROPERTIES_PANEL_VISIBLE.setValue(CFG_GUI.LINKGRABBER_TAB_PROPERTIES_PANEL_VISIBLE.isEnabled() ? false : true);
                }
                break;
            case RENAME:
                startEditing(contextObject);
                break;
            }
        }
        return true;
    }

    @Override
    public boolean isEnabled(AbstractNode obj) {
        if (obj instanceof CrawledPackage) {
            return ((CrawledPackage) obj).getView().isEnabled();
        }
        return obj.isEnabled();
    }

    @Override
    public boolean isSortable(AbstractNode obj) {
        return true;
    }

    @Override
    public int getDefaultWidth() {
        return 350;
    }

    @Override
    public boolean isEditable(AbstractNode obj) {
        return true;
    }

    public boolean isExpandCollapseArea(final MouseEvent e, final AbstractNode obj) {
        if (obj instanceof AbstractPackageNode && (e.getPoint().x - getBounds().x) < EXPAND_COLLAPSE_AREA) {
            return true;
        }
        return false;
    }

    @Override
    public boolean onRenameClick(final MouseEvent e, final AbstractNode obj) {
        if (isExpandCollapseArea(e, obj)) {
            return false;
        }
        startEditing(obj);
        return true;
    }

    protected boolean isEditable(final AbstractNode obj, final boolean enabled) {
        return isEditable(obj);
    }

    @Override
    protected void setStringValue(final String value, final AbstractNode object) {
        if (object instanceof FilePackage) {
            ((FilePackage) object).setName(value);
        } else if (object instanceof CrawledPackage) {
            ((CrawledPackage) object).setName(value);
        } else if (object instanceof CrawledLink) {
            boolean isMultiArchive = false;
            try {
                ExtractionExtension archiver = ((ExtractionExtension) ExtensionController.getInstance().getExtension(ExtractionExtension.class)._getExtension());
                if (archiver != null) {
                    final CrawledLinkFactory archiveFactory = new CrawledLinkFactory(((CrawledLink) object));
                    final Archive archive = archiver.buildArchive(archiveFactory);
                    isMultiArchive = archive != null && archive.getArchiveFiles().size() > 1;
                }
            } catch (Throwable e) {
                org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().log(e);
            }
            ((CrawledLink) object).setName(value);
            if (isMultiArchive) {
                String title = _GUI.T.FileColumn_setStringValue_title_();
                String msg = _GUI.T.FileColumn_setStringValue_msg_();
                final Icon icon = NewTheme.I().getIcon(IconKey.ICON_WARNING, 32);
                JDGui.help(title, msg, icon);
            }
        } else if (object instanceof DownloadLink) {
            boolean isMultiArchive = false;
            try {
                ExtractionExtension archiver = ((ExtractionExtension) ExtensionController.getInstance().getExtension(ExtractionExtension.class)._getExtension());
                if (archiver != null) {
                    final DownloadLinkArchiveFactory archiveFactory = new DownloadLinkArchiveFactory(((DownloadLink) object));
                    final Archive archive = archiver.buildArchive(archiveFactory);
                    isMultiArchive = archive != null && archive.getArchiveFiles().size() > 1;
                }
            } catch (Throwable e) {
                org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().log(e);
            }
            DownloadWatchDog.getInstance().renameLink(((DownloadLink) object), value);
            if (isMultiArchive) {
                String title = _GUI.T.FileColumn_setStringValue_title_();
                String msg = _GUI.T.FileColumn_setStringValue_msg_();
                final Icon icon = NewTheme.I().getIcon(IconKey.ICON_WARNING, 32);
                JDGui.help(title, msg, icon);
            }
        }
    }

    @Override
    protected Icon getIcon(AbstractNode value) {
        if (false && value instanceof ArchiveCrawledPackage) {
            return (((AbstractPackageNode<?, ?>) value).isExpanded() ? iconArchiveOpen : iconArchive);
        } else if (value instanceof AbstractPackageNode) {
            return (((AbstractPackageNode<?, ?>) value).isExpanded() ? iconPackageOpen : iconPackageClosed);
        } else if (value instanceof DownloadLink) {
            return (((DownloadLink) value).getLinkInfo().getIcon());
        } else if (value instanceof CrawledLink) {
            return (((CrawledLink) value).getLinkInfo().getIcon());
        }
        return null;
    }

    public void configureRendererComponent(AbstractNode value, boolean isSelected, boolean hasFocus, int row, int column) {
        this.rendererIcon.setIcon(this.getIcon(value));
        String str = this.getStringValue(value);
        if (str == null) {
            // under substance, setting setText(null) somehow sets the label
            // opaque.
            str = "";
        }
        if (getTableColumn() != null) {
            this.rendererField.setText(SwingUtilities2Wrapper.clipStringIfNecessary(rendererField, rendererField.getFontMetrics(rendererField.getFont()), str, getTableColumn().getWidth() - rendererIcon.getPreferredSize().width - 32));
        } else {
            this.rendererField.setText(str);
        }
        if (value instanceof AbstractPackageNode) {
            renderer.setBorder(normalBorder);
        } else if (value instanceof AbstractPackageChildrenNode) {
            AbstractPackageNode parent = ((AbstractPackageNode) ((AbstractPackageChildrenNode) value).getParentNode());
            if (parent != null && parent.getChildren().size() == 1 && hideSinglePackage) {
                renderer.setBorder(normalBorder);
            } else {
                renderer.setBorder(leftGapBorder);
            }
        }
    }

    @Override
    public void configureEditorComponent(AbstractNode value, boolean isSelected, int row, int column) {
        super.configureEditorComponent(value, isSelected, row, column);
        if (value instanceof AbstractPackageNode) {
            selectAll = true;
            editor.setBorder(normalBorder);
        } else {
            selectAll = false;
            editor.setBorder(leftGapBorder);
        }
    }

    @Override
    public void focusLost(FocusEvent e) {
        super.focusLost(e);
    }

    @Override
    public void focusGained(final FocusEvent e) {
        String txt = editorField.getText();
        int point = txt.lastIndexOf(".");
        int pointPart = txt.lastIndexOf(".part");
        if (pointPart > 0) {
            point = pointPart;
        }
        /* select filename only, try to keep the extension/filetype */
        if (point > 0 && selectAll == false) {
            editorField.select(0, point);
        } else {
            this.editorField.selectAll();
        }
    }

    @Override
    public boolean isHidable() {
        return true;
    }

    @Override
    public final String getStringValue(AbstractNode value) {
        if (value instanceof DownloadLink) {
            //
            return ((DownloadLink) value).getView().getDisplayName();
        }
        return value.getName();
    }

    @Override
    public void onConfigValidatorError(KeyHandler<Boolean> keyHandler, Boolean invalidValue, ValidationException validateException) {
    }

    @Override
    public void onConfigValueModified(KeyHandler<Boolean> keyHandler, Boolean newValue) {
        hideSinglePackage = newValue;
    }
}
