package org.jdownloader.gui.views.linkgrabber.columns;

import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import javax.swing.ComboBoxModel;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;

import jd.controlling.linkchecker.LinkChecker;
import jd.controlling.linkcollector.LinkCollector;
import jd.controlling.linkcrawler.CheckableLink;
import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.packagecontroller.AbstractNode;
import jd.gui.swing.jdgui.JDGui;
import jd.gui.swing.jdgui.views.settings.ConfigurationView;
import jd.gui.swing.jdgui.views.settings.panels.pluginsettings.PluginSettings;
import jd.plugins.DownloadLink;

import org.appwork.storage.config.JsonConfig;
import org.appwork.swing.action.BasicAction;
import org.appwork.swing.exttable.ExtMenuItem;
import org.appwork.swing.exttable.columns.ExtComboColumn;
import org.appwork.utils.event.queue.QueueAction;
import org.jdownloader.DomainInfo;
import org.jdownloader.controlling.linkcrawler.LinkVariant;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.AbstractIcon;
import org.jdownloader.images.BadgeIcon;
import org.jdownloader.settings.GraphicalUserInterfaceSettings;

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
    protected void fillPopup(final JPopupMenu popup, AbstractNode value, LinkVariant selected, ComboBoxModel<LinkVariant> dm) {
        super.fillPopup(popup, value, selected, dm);
        if (value instanceof CrawledLink) {
            final CrawledLink link = (CrawledLink) value;

            final HashSet<String> dupeSet = new HashSet<String>();

            LinkCollector lc = LinkCollector.getInstance();

            boolean readL = link.getParentNode().getModifyLock().readLock();

            try {

                for (CrawledLink cl : link.getParentNode().getChildren()) {
                    dupeSet.add(cl.getLinkID());

                }
            } finally {
                link.getParentNode().getModifyLock().readUnlock(readL);
            }
            popup.add(new JSeparator());
            JMenu m = new JMenu(_GUI._.VariantColumn_fillPopup_add());
            m.setIcon(new AbstractIcon(IconKey.ICON_ADD, 18));

            for (int i = 0; i < dm.getSize(); i++) {
                final LinkVariant o = dm.getElementAt(i);

                ExtMenuItem mi;
                m.add(mi = new ExtMenuItem(new BasicAction() {

                    private CrawledLink cl;

                    {
                        final DownloadLink dllink = new DownloadLink(link.getDownloadLink().getDefaultPlugin(), link.getDownloadLink().getName(), link.getDownloadLink().getHost(), link.getDownloadLink().getDownloadURL(), true);
                        dllink.setProperties(link.getDownloadLink().getProperties());
                        cl = new CrawledLink(dllink);
                        setSmallIcon(o.getIcon());
                        setName(o.getName());

                        cl.getDownloadLink().getDefaultPlugin().setActiveVariantByLink(cl.getDownloadLink(), o);

                        setEnabled(!dupeSet.contains(cl.getLinkID()));
                    }

                    @Override
                    public void actionPerformed(final ActionEvent e) {
                        if (!isEnabled()) {

                            Toolkit.getDefaultToolkit().beep();
                            return;
                        }

                        final ArrayList<CrawledLink> list = new ArrayList<CrawledLink>();
                        list.add(cl);
                        dupeSet.add(cl.getLinkID());
                        // if (LinkCollector.getInstance().hasLinkID(cl.getLinkID())) {
                        //
                        // } else {
                        LinkCollector.getInstance().getQueue().add(new QueueAction<Void, RuntimeException>() {

                            @Override
                            protected Void run() throws RuntimeException {

                                LinkCollector.getInstance().moveOrAddAt(link.getParentNode(), list, link.getParentNode().indexOf(link) + 1);

                                java.util.List<CheckableLink> checkableLinks = new ArrayList<CheckableLink>(1);
                                checkableLinks.add(cl);
                                LinkChecker<CheckableLink> linkChecker = new LinkChecker<CheckableLink>(true);
                                linkChecker.check(checkableLinks);
                                return null;
                            }
                        });

                        // }
                        setEnabled(false);

                    }

                }));
                mi.setHideOnClick(false);
            }
            popup.add(m);

            popup.add(new JMenuItem(new BasicAction() {
                {

                    setSmallIcon(new BadgeIcon(new AbstractIcon(IconKey.ICON_SETTINGS, 18), DomainInfo.getInstance(link.getDownloadLink().getDefaultPlugin().getHost()).getIcon(10), 0, 0).crop(18, 18));
                    setName(_GUI._.VariantColumn_fillPopup_settings(link.getDownloadLink().getDefaultPlugin().getHost()));

                }

                @Override
                public void actionPerformed(final ActionEvent e) {
                    popup.setVisible(false);
                    JsonConfig.create(GraphicalUserInterfaceSettings.class).setConfigViewVisible(true);
                    JDGui.getInstance().setContent(ConfigurationView.getInstance(), true);
                    ConfigurationView.getInstance().setSelectedSubPanel(PluginSettings.class);
                    ConfigurationView.getInstance().getSubPanel(PluginSettings.class).setPlugin(link.getDownloadLink().getDefaultPlugin().getClass());
                }

            }));
        }
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
