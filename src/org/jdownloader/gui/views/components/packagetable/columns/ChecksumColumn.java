package org.jdownloader.gui.views.components.packagetable.columns;

import javax.swing.JPopupMenu;

import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.CrawledPackage;
import jd.controlling.packagecontroller.AbstractNode;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.download.HashInfo;

import org.appwork.swing.exttable.columns.ExtTextColumn;
import org.appwork.utils.StringUtils;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.downloads.columns.FileColumn;

public class ChecksumColumn extends ExtTextColumn<AbstractNode> {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    public ChecksumColumn() {
        super(_GUI.T.checksumcolumnmd5());
    }

    public JPopupMenu createHeaderPopup() {
        return FileColumn.createColumnPopup(this, getMinWidth() == getMaxWidth() && getMaxWidth() > 0);
    }

    public void configureEditorComponent(final AbstractNode value, final boolean isSelected, final int row, final int column) {
        this.editorField.removeActionListener(this);
        String str = this.getEditorStringValue(value);
        if (str == null) {
            // under substance, setting setText(null) somehow sets the label
            // opaque.
            str = "";
        }
        this.editorField.setText(str);
        this.editorField.addActionListener(this);
        this.editorIconLabel.setIcon(this.getIcon(value));
    }

    private String getEditorStringValue(AbstractNode value) {
        final DownloadLink dl;
        if (value instanceof CrawledLink) {
            dl = ((CrawledLink) value).getDownloadLink();
        } else if (value instanceof DownloadLink) {
            dl = ((DownloadLink) value);
        } else {
            return null;
        }
        if (dl != null) {
            final HashInfo hashInfo = dl.getHashInfo();
            if (hashInfo != null) {
                return hashInfo.getHash();
            }
        }
        return null;
    }

    @Override
    public boolean isDefaultVisible() {
        return false;
    }

    @Override
    protected boolean isEditable(final AbstractNode obj, final boolean enabled) {
        return isEditable(obj);
    }

    @Override
    public boolean isEditable(AbstractNode obj) {
        if (obj instanceof CrawledLink) {
            return true;
        }
        if (obj instanceof DownloadLink) {
            return true;
        }
        return false;
    }

    @Override
    public boolean isEnabled(final AbstractNode obj) {
        if (obj instanceof CrawledPackage) {
            return ((CrawledPackage) obj).getView().isEnabled();
        }
        if (obj instanceof FilePackage) {
            return ((FilePackage) obj).getView().isEnabled();
        }
        return obj.isEnabled();
    }

    @Override
    protected void setStringValue(String value, AbstractNode object) {
        final DownloadLink dl;
        if (object instanceof CrawledLink) {
            dl = ((CrawledLink) object).getDownloadLink();
        } else if (object instanceof DownloadLink) {
            dl = ((DownloadLink) object);
        } else {
            return;
        }
        if (dl != null) {
            value = value.trim();
            final HashInfo hashInfo;
            if (StringUtils.isEmpty(value)) {
                hashInfo = new HashInfo("", HashInfo.TYPE.NONE, true, true);
            } else {
                hashInfo = HashInfo.parse(value, true, true);
            }
            dl.setHashInfo(hashInfo);
        }
    }

    @Override
    public String getStringValue(AbstractNode value) {
        final DownloadLink dl;
        if (value instanceof CrawledLink) {
            dl = ((CrawledLink) value).getDownloadLink();
        } else if (value instanceof DownloadLink) {
            dl = ((DownloadLink) value);
        } else {
            return null;
        }
        if (dl != null) {
            final HashInfo hashInfo = dl.getHashInfo();
            if (hashInfo != null) {
                return "[" + hashInfo.getType().name() + "] " + hashInfo.getHash();
            }
        }
        return null;
    }

}
