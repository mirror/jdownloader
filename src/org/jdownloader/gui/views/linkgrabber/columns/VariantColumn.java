package org.jdownloader.gui.views.linkgrabber.columns;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashSet;
import java.util.List;

import javax.swing.AbstractButton;
import javax.swing.ComboBoxModel;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JPopupMenu;

import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.packagecontroller.AbstractNode;
import jd.plugins.DownloadLink;

import org.appwork.swing.exttable.columns.ExtComboColumn;
import org.jdownloader.controlling.linkcrawler.LinkVariant;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.AbstractIcon;

import sun.swing.SwingUtilities2;

public class VariantColumn extends ExtComboColumn<AbstractNode, LinkVariant> {

    public VariantColumn() {
        super(_GUI._.VariantColumn_VariantColumn_name_(), null);
    }

    @Override
    protected Icon createDropDownIcon() {
        return new AbstractIcon(IconKey.ICON_POPUPDOWN, -1);
    }

    @Override
    protected JComponent getPopupElement(LinkVariant object, boolean selected) {
        JComponent ret = null;
        if (object instanceof CrawledLink) {
            if (((CrawledLink) object).hasVariantSupport()) {
                ret = ((CrawledLink) object).gethPlugin().getVariantPopupComponent(((CrawledLink) object).getDownloadLink());
            }
        } else if (object instanceof DownloadLink) {
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

    protected boolean startEdit(final AbstractNode value, final int row) {
        final JPopupMenu popup = new JPopupMenu();
        try {
            final HashSet<LinkVariant> selected = new HashSet<LinkVariant>(getSelectedItems(value));
            final ComboBoxModel<LinkVariant> dm = updateModel(null, value);
            for (int i = 0; i < dm.getSize(); i++) {
                final LinkVariant o = dm.getElementAt(i);
                final JComponent bt = getPopupElement(o, selected.contains(o));
                if (bt instanceof AbstractButton) {
                    ((AbstractButton) bt).addActionListener(new ActionListener() {

                        @Override
                        public void actionPerformed(final ActionEvent e) {
                            setValue(o, value);
                            popup.setVisible(false);

                        }
                    });
                }
                popup.add(bt);
            }

            final Rectangle bounds = getModel().getTable().getCellRect(row, getIndex(), true);
            final Dimension pref = popup.getPreferredSize();
            popup.setPreferredSize(new Dimension(Math.max(pref.width, bounds.width), pref.height));
            popup.show(getModel().getTable(), bounds.x, bounds.y + bounds.height);
            return true;
        } catch (final Exception e1) {
            e1.printStackTrace();
        }
        return false;
    }

    @Override
    public boolean isEditable(final AbstractNode object) {
        if (object instanceof CrawledLink) {
            if (((CrawledLink) object).hasVariantSupport()) { return ((CrawledLink) object).gethPlugin().hasVariantToChooseFrom(((CrawledLink) object).getDownloadLink()); }
        } else if (object instanceof DownloadLink) {
            if (((DownloadLink) object).hasVariantSupport()) { return ((DownloadLink) object).getDefaultPlugin().hasVariantToChooseFrom(((DownloadLink) object)); }
        }
        return false;
    }

    @Override
    public void configureRendererComponent(final AbstractNode value, final boolean isSelected, final boolean hasFocus, final int row, final int column) {
        // TODO Auto-generated method stub
        List<LinkVariant> selected = getSelectedItems(value);
        rendererPanel.setEditable(isEditable(value));
        if (selected == null) {
            rendererField.setText(null);
            rendererIcon.setText(null);
            return;
        }

        rendererIcon.setIcon(null);
        StringBuilder sb = new StringBuilder();
        for (LinkVariant la : selected) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(modelItemToString(la));
        }
        String str = sb.toString();
        if (str == null) {
            // under substance, setting setText(null) somehow sets the label
            // opaque.
            str = "";
        }

        if (getTableColumn() != null) {
            rendererField.setText(SwingUtilities2.clipStringIfNecessary(rendererField, rendererField.getFontMetrics(rendererField.getFont()), str, getTableColumn().getWidth() - 18 - 5));
        } else {
            rendererField.setText(str);
        }

    }

    protected List<LinkVariant> getSelectedItems(AbstractNode object) {
        if (object instanceof CrawledLink) {
            if (!((CrawledLink) object).hasVariantSupport()) return null;
            return ((CrawledLink) object).gethPlugin().getActiveVariantsByLink(((CrawledLink) object).getDownloadLink());
        } else if (object instanceof DownloadLink) {
            if (!((DownloadLink) object).hasVariantSupport()) return null;
            return ((DownloadLink) object).getDefaultPlugin().getActiveVariantsByLink(((DownloadLink) object));
        }
        return null;
    }

    @Override
    protected void setSelectedItem(AbstractNode object, LinkVariant value) {
        if (object instanceof CrawledLink) {

            ((CrawledLink) object).gethPlugin().setActiveVariantByLink(((CrawledLink) object).getDownloadLink(), value);
        } else if (object instanceof DownloadLink) {

            ((DownloadLink) object).getDefaultPlugin().setActiveVariantByLink(((DownloadLink) object), value);
        }
    }

    @Override
    public ComboBoxModel<LinkVariant> updateModel(ComboBoxModel<LinkVariant> dataModel, AbstractNode object) {

        List<LinkVariant> variants;
        if (object instanceof CrawledLink) {
            if (!((CrawledLink) object).hasVariantSupport()) return null;
            variants = ((CrawledLink) object).gethPlugin().getVariantsByLink(((CrawledLink) object).getDownloadLink());
            return VariantsModel.get(variants);
        } else if (object instanceof DownloadLink) {
            if (!((DownloadLink) object).hasVariantSupport()) return null;
            variants = ((DownloadLink) object).getDefaultPlugin().getVariantsByLink(((DownloadLink) object));
            VariantsModel.get(variants);
        }
        return null;
    }

    @Override
    protected LinkVariant getSelectedItem(AbstractNode object) {
        return null;
    }

}
