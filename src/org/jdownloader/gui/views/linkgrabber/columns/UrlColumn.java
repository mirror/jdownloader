package org.jdownloader.gui.views.linkgrabber.columns;

import java.awt.event.ActionEvent;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JTextField;

import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.CrawledPackage;
import jd.controlling.packagecontroller.AbstractNode;
import jd.plugins.DownloadLink;
import net.miginfocom.swing.MigLayout;

import org.appwork.swing.action.BasicAction;
import org.appwork.swing.components.ExtButton;
import org.appwork.swing.exttable.columns.ExtTextColumn;
import org.appwork.utils.os.CrossSystem;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;

public class UrlColumn extends ExtTextColumn<AbstractNode> {
    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    private ExtButton         bt;
    private AbstractNode      editing          = null;

    @Override
    public boolean isEnabled(final AbstractNode obj) {
        if (obj instanceof CrawledPackage) { return ((CrawledPackage) obj).getView().isEnabled(); }
        return obj.isEnabled();
    }

    protected boolean isEditable(final AbstractNode obj, final boolean enabled) {

        return isEditable(obj);
    }

    @Override
    public boolean isDefaultVisible() {
        return false;
    }

    public UrlColumn() {
        super(_GUI._.LinkGrabberTableModel_initColumns_url());
        this.setClickcount(2);
        editorField.addMouseListener(new MouseAdapter() {

            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() >= 2) {
                    editorField.selectAll();
                    if (isOpenURLAllowed(editing)) CrossSystem.openURLOrShowMessage(editorField.getText());
                    UrlColumn.this.stopCellEditing();
                }
            }
        });
        editorField.setBorder(new JTextField().getBorder());
        editorField.setEditable(false);
        bt = new ExtButton(new BasicAction() {
            /**
             * 
             */
            private static final long serialVersionUID = -5856576135198890532L;

            {
                setSmallIcon(NewTheme.I().getIcon("browse", 16));
                setTooltipText(_GUI._.UrlColumn_UrlColumn_open_tt_());
            }

            public void actionPerformed(ActionEvent e) {
                noset = true;
                if (isOpenURLAllowed(editing)) CrossSystem.openURLOrShowMessage(getStringValue(editing));
                UrlColumn.this.stopCellEditing();
            }
        });

        // bt.setRolloverEffectEnabled(true);
        editor.setLayout(new MigLayout("ins 1 4 1 0", "[grow,fill][]", "[fill,grow]"));
        editor.removeAll();
        editor.add(this.editorField, "height 20!");
        editor.add(bt, "height 20!,width 20!");

    }

    @Override
    public void configureEditorComponent(AbstractNode value, boolean isSelected, int row, int column) {
        super.configureEditorComponent(value, isSelected, row, column);
        editing = value;
        noset = false;
        bt.setEnabled(CrossSystem.isOpenBrowserSupported() && isOpenURLAllowed(value));
    }

    @Override
    public void focusGained(final FocusEvent e) {
    }

    @Override
    public boolean isEditable(AbstractNode obj) {
        return obj instanceof CrawledLink || obj instanceof DownloadLink;
    }

    private boolean isOpenURLAllowed(AbstractNode value) {
        DownloadLink dlLink = null;
        if (value instanceof CrawledLink) {
            dlLink = ((CrawledLink) value).getDownloadLink();
        } else if (value instanceof DownloadLink) {
            dlLink = (DownloadLink) value;
        }
        if (dlLink != null) {
            if (dlLink.getLinkType() == DownloadLink.LINKTYPE_NORMAL) return true;
            if (dlLink.getLinkType() == DownloadLink.LINKTYPE_CONTAINER && dlLink.gotBrowserUrl()) return true;
        }
        return false;
    }

    @Override
    public boolean onDoubleClick(MouseEvent e, AbstractNode obj) {
        return false;
    }

    @Override
    public boolean isSortable(AbstractNode obj) {
        return true;
    }

    @Override
    protected void setStringValue(String value, AbstractNode object) {
    }

    @Override
    public String getStringValue(AbstractNode value) {
        if (value instanceof CrawledPackage) {
            return null;
        } else if (value instanceof CrawledLink) {
            DownloadLink dlLink = ((CrawledLink) value).getDownloadLink();
            if (dlLink.getLinkType() == DownloadLink.LINKTYPE_CONTAINER) {
                if (dlLink.gotBrowserUrl()) return dlLink.getBrowserUrl();
                return null;
            }
            return dlLink.getBrowserUrl();
        } else if (value instanceof DownloadLink) {
            DownloadLink dlLink = ((DownloadLink) value);
            if (dlLink.getLinkType() == DownloadLink.LINKTYPE_CONTAINER) {
                if (dlLink.gotBrowserUrl()) return dlLink.getBrowserUrl();
                return null;
            }
            return dlLink.getBrowserUrl();
        }
        return null;
    }

}
