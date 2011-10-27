package org.jdownloader.gui.views.linkgrabber;

import java.awt.event.MouseEvent;

import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.CrawledPackage;
import jd.controlling.packagecontroller.AbstractNode;

import org.appwork.swing.exttable.columns.ExtTextColumn;
import org.appwork.utils.os.CrossSystem;
import org.jdownloader.gui.translate._GUI;

public class UrlColumn extends ExtTextColumn<AbstractNode> {
    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    public UrlColumn() {
        super(_GUI._.LinkGrabberTableModel_initColumns_url());
        this.setClickcount(1);
    }

    @Override
    public boolean isEditable(AbstractNode obj) {

        return true;
    }

    @Override
    protected void onSingleClick(MouseEvent e, AbstractNode obj) {
        super.onSingleClick(e, obj);
    }

    @Override
    protected void onDoubleClick(MouseEvent e, AbstractNode obj) {
        CrossSystem.openURLOrShowMessage(getStringValue(obj));
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
            return null;
        } else {
            return ((CrawledLink) value).getDownloadLink().getBrowserUrl();
        }
    }

}
