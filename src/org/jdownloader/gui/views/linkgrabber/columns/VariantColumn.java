package org.jdownloader.gui.views.linkgrabber.columns;

import java.util.List;

import javax.swing.ComboBoxModel;
import javax.swing.Icon;
import javax.swing.JComponent;

import jd.controlling.linkcollector.LinkCollector;
import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.packagecontroller.AbstractNode;
import jd.plugins.DownloadLink;

import org.appwork.swing.exttable.columns.ExtComboColumn;
import org.jdownloader.controlling.linkcrawler.LinkVariant;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.AbstractIcon;

public class VariantColumn extends ExtComboColumn<AbstractNode, LinkVariant> {

    private boolean autoVisible;

    public VariantColumn() {
        super(_GUI._.VariantColumn_VariantColumn_name_(), null);
    }

    @Override
    protected Icon createDropDownIcon() {
        return new AbstractIcon(IconKey.ICON_POPDOWNLARGE, -1);
    }

    @Override
    protected JComponent getPopupElement(LinkVariant object, boolean selected) {
        JComponent ret = null;
        if (object instanceof CrawledLink) {
            if (((CrawledLink) object).hasVariantSupport()) {
                ret = ((CrawledLink) object).gethPlugin().getVariantPopupComponent(((CrawledLink) object).getDownloadLink());
            }
        } else if (false && object instanceof DownloadLink) {
            /* DownloadTable does not have VariantSupport yet */
            if (((DownloadLink) object).hasVariantSupport()) {
                ret = ((DownloadLink) object).getDefaultPlugin().getVariantPopupComponent(((DownloadLink) object));
            }
        }
        if (ret != null) return ret;
        return super.getPopupElement(object, selected);
    }

    @Override
    protected String modelItemToString(LinkVariant selectedItem) {
        if (selectedItem == null) { return null; }
        return selectedItem.getName();
    }

    @Override
    protected String getTooltipText(AbstractNode obj) {
        return super.getTooltipText(obj);
    }

    protected Icon modelItemToIcon(LinkVariant selectedItem) {
        if (selectedItem == null) { return null; }
        return selectedItem.getIcon();
    }

    @Override
    public boolean isEditable(final AbstractNode object) {
        if (object instanceof CrawledLink) {
            if (((CrawledLink) object).hasVariantSupport()) { return ((CrawledLink) object).gethPlugin().hasVariantToChooseFrom(((CrawledLink) object).getDownloadLink()); }
        } else if (false && object instanceof DownloadLink) {
            /* DownloadTable does not have VariantSupport yet */
            if (((DownloadLink) object).hasVariantSupport()) { return ((DownloadLink) object).getDefaultPlugin().hasVariantToChooseFrom(((DownloadLink) object)); }
        }
        return false;
    }

    protected LinkVariant getSelectedItem(AbstractNode object) {
        if (object instanceof CrawledLink) {
            if (!((CrawledLink) object).hasVariantSupport()) return null;
            return ((CrawledLink) object).gethPlugin().getActiveVariantByLink(((CrawledLink) object).getDownloadLink());
        } else if (false && object instanceof DownloadLink) {
            /* DownloadTable does not have VariantSupport yet */
            if (!((DownloadLink) object).hasVariantSupport()) return null;
            return ((DownloadLink) object).getDefaultPlugin().getActiveVariantByLink(((DownloadLink) object));
        }
        return null;
    }

    @Override
    protected void setSelectedItem(AbstractNode object, LinkVariant value) {
        if (object instanceof CrawledLink) {
            LinkCollector.getInstance().setActiveVariantForLink((CrawledLink) object, value);
        } else if (object instanceof DownloadLink) {
            /* DownloadTable does not have VariantSupport yet */
        }
    }

    @Override
    public ComboBoxModel<LinkVariant> updateModel(ComboBoxModel<LinkVariant> dataModel, AbstractNode object) {
        List<LinkVariant> variants;
        if (object instanceof CrawledLink) {
            if (!((CrawledLink) object).hasVariantSupport()) return null;
            variants = ((CrawledLink) object).gethPlugin().getVariantsByLink(((CrawledLink) object).getDownloadLink());
            return new VariantsModel(variants);
        } else if (object instanceof DownloadLink) {
            if (!((DownloadLink) object).hasVariantSupport()) return null;
            variants = ((DownloadLink) object).getDefaultPlugin().getVariantsByLink(((DownloadLink) object));
            return new VariantsModel(variants);
        }
        return null;
    }

    @Override
    public boolean isVisible(boolean savedValue) {
        return autoVisible && savedValue;
    }

    public void setAutoVisible(boolean b) {
        this.autoVisible = b;
    }

}
