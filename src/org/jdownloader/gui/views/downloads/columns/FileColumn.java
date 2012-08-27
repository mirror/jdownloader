package org.jdownloader.gui.views.downloads.columns;

import java.awt.event.FocusEvent;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.logging.Level;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.border.Border;

import jd.controlling.linkcrawler.ArchiveCrawledPackage;
import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.CrawledPackage;
import jd.controlling.packagecontroller.AbstractNode;
import jd.controlling.packagecontroller.AbstractPackageChildrenNode;
import jd.controlling.packagecontroller.AbstractPackageNode;
import jd.gui.swing.jdgui.JDGui;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

import org.appwork.swing.exttable.columns.ExtTextColumn;
import org.appwork.utils.StringUtils;
import org.appwork.utils.logging.Log;
import org.appwork.utils.os.CrossSystem;
import org.jdownloader.extensions.ExtensionController;
import org.jdownloader.extensions.extraction.ExtractionExtension;
import org.jdownloader.extensions.extraction.bindings.crawledlink.CrawledLinkFactory;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.components.packagetable.LinkTreeUtils;
import org.jdownloader.gui.views.downloads.context.OpenFileAction;
import org.jdownloader.images.NewTheme;
import org.jdownloader.settings.staticreferences.CFG_GUI;

import sun.swing.SwingUtilities2;

public class FileColumn extends ExtTextColumn<AbstractNode> {

    /**
     * 
     */
    private static final long serialVersionUID = -2963955407564917958L;
    protected Border          leftGapBorder;
    private ImageIcon         iconPackageOpen;
    private ImageIcon         iconPackageClosed;
    private ImageIcon         iconArchive;
    private ImageIcon         iconArchiveOpen;
    protected Border          normalBorder;
    private boolean           selectAll        = false;

    public FileColumn() {
        super(_GUI._.filecolumn_title());
        leftGapBorder = BorderFactory.createEmptyBorder(0, 32, 0, 0);
        normalBorder = BorderFactory.createEmptyBorder(0, 0, 0, 0);
        iconPackageOpen = NewTheme.I().getIcon("tree_package_open", 32);
        iconArchiveOpen = NewTheme.I().getIcon("tree_archive_open", 32);
        iconArchive = NewTheme.I().getIcon("tree_archive", 32);
        iconPackageClosed = NewTheme.I().getIcon("tree_package_closed", 32);
        setClickcount(0);
    }

    public boolean isPaintWidthLockIcon() {

        return false;
    }

    @Override
    protected boolean onDoubleClick(MouseEvent e, AbstractNode contextObject) {

        if (e.getPoint().x - getBounds().x < 30) { return false; }
        if (contextObject instanceof DownloadLink) {
            if (CrossSystem.isOpenFileSupported()) {
                new OpenFileAction(new File(((DownloadLink) contextObject).getFileOutput())).actionPerformed(null);

            }
        } else if (contextObject instanceof CrawledLink) {
            if (CrossSystem.isOpenFileSupported()) {
                new OpenFileAction(LinkTreeUtils.getDownloadDirectory(contextObject)).actionPerformed(null);

            }
        } else if (contextObject instanceof FilePackage) {
            if (CrossSystem.isOpenFileSupported()) {

                new OpenFileAction(LinkTreeUtils.getDownloadDirectory(contextObject)).actionPerformed(null);

            }
        } else if (contextObject instanceof CrawledPackage) {
            if (CrossSystem.isOpenFileSupported()) {
                new OpenFileAction(LinkTreeUtils.getDownloadDirectory(contextObject)).actionPerformed(null);

            }
        }

        return true;
    }

    @Override
    public boolean isEnabled(AbstractNode obj) {
        if (obj instanceof CrawledPackage) { return ((CrawledPackage) obj).getView().isEnabled(); }
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

    protected boolean onRenameClick(final MouseEvent e, final AbstractNode obj) {
        if (e.getPoint().x - getBounds().x < 30) { return false; }
        startEditing(obj);
        return true;

    }

    protected boolean isEditable(final AbstractNode obj, final boolean enabled) {
        return isEditable(obj);
    }

    @Override
    protected void setStringValue(final String value, final AbstractNode object) {
        if (StringUtils.isEmpty(value)) return;
        if (object instanceof FilePackage) {
            ((FilePackage) object).setName(value);
        } else if (object instanceof CrawledPackage) {
            ((CrawledPackage) object).setName(value);
        } else if (object instanceof CrawledLink) {
            boolean isMultiArchive = false;

            try {
                ExtractionExtension archiver = ((ExtractionExtension) ExtensionController.getInstance().getExtension(ExtractionExtension.class)._getExtension());
                if (archiver != null) {
                    CrawledLinkFactory clf = new CrawledLinkFactory(((CrawledLink) object));
                    isMultiArchive = archiver.isMultiPartArchive(clf);
                }
            } catch (Throwable e) {
                Log.exception(Level.SEVERE, e);
            }

            ((CrawledLink) object).setName(value);

            if (isMultiArchive) {
                String title = _GUI._.FileColumn_setStringValue_title_();
                String msg = _GUI._.FileColumn_setStringValue_msg_();
                ImageIcon icon = NewTheme.I().getIcon("warning", 32);
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
            return (((DownloadLink) value).getIcon());
        } else if (value instanceof CrawledLink) { return (((CrawledLink) value).getIcon()); }
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
            this.rendererField.setText(SwingUtilities2.clipStringIfNecessary(rendererField, rendererField.getFontMetrics(rendererField.getFont()), str, getTableColumn().getWidth() - rendererIcon.getPreferredSize().width - 32));
        } else {
            this.rendererField.setText(str);
        }
        if (value instanceof AbstractPackageNode) {
            renderer.setBorder(normalBorder);
        } else if (value instanceof AbstractPackageChildrenNode) {
            AbstractPackageNode parent = ((AbstractPackageNode) ((AbstractPackageChildrenNode) value).getParentNode());
            if (parent != null && parent.getChildren().size() == 1 && CFG_GUI.HIDE_SINGLE_CHILD_PACKAGES.isEnabled()) {
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
        /* select filename only, try to keep the extension/filetype */
        if (point > 0 && selectAll == false) {
            editorField.select(0, point);
        } else {
            this.editorField.selectAll();
        }

    }

    @Override
    public boolean isHidable() {
        return false;
    }

    @Override
    public final String getStringValue(AbstractNode value) {
        return value.getName();
    }

}
