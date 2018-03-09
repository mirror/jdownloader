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
import jd.controlling.linkcrawler.CrawledPackage;
import jd.controlling.packagecontroller.AbstractNode;
import jd.gui.swing.jdgui.JDGui;
import jd.gui.swing.jdgui.views.settings.ConfigurationView;
import jd.gui.swing.jdgui.views.settings.panels.pluginsettings.PluginSettings;
import jd.plugins.DownloadLink;

import org.appwork.storage.config.JsonConfig;
import org.appwork.swing.action.BasicAction;
import org.appwork.swing.components.JScrollPopupMenu;
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
    private boolean alwaysVisible;

    public VariantColumn(boolean alwaysVisible) {
        super(_GUI.T.VariantColumn_VariantColumn_name_(), null);
        this.alwaysVisible = alwaysVisible;
    }

    @Override
    protected Icon createDropDownIcon() {
        return new AbstractIcon(IconKey.ICON_POPDOWNLARGE, -1);
    }

    @Override
    public JPopupMenu createPopupMenu() {
        return new JScrollPopupMenu() {
            {
                setMaximumVisibleRows(20);
            }

            public void setVisible(final boolean b) {
                super.setVisible(b);
                if (!b) {
                    lastHide = System.currentTimeMillis();
                    // editing = null;
                    // updateIcon(true);
                } else {
                    // updateIcon(false);
                }
            };
        };
    }

    @Override
    protected JComponent getPopupElement(LinkVariant object, boolean selected, AbstractNode value) {
        JComponent ret = null;
        if (value instanceof CrawledLink) {
            if (((CrawledLink) value).hasVariantSupport()) {
                ret = ((CrawledLink) value).gethPlugin().getVariantPopupComponent(((CrawledLink) value).getDownloadLink());
            }
        }
        if (ret != null) {
            return ret;
        }
        return super.getPopupElement(object, selected, value);
    }

    @Override
    protected void fillPopup(final JPopupMenu popup, AbstractNode value, LinkVariant selected, ComboBoxModel<LinkVariant> dm) {
        JComponent ret = null;
        if (value instanceof CrawledLink) {
            final CrawledLink link = (CrawledLink) value;
            if (link.hasVariantSupport()) {
                if (link.gethPlugin().fillVariantsPopup(this, popup, value, selected, dm)) {
                    return;
                }
            }
            fillPopupWithVariants(popup, value, selected, dm);
            popup.add(new JSeparator());
            JMenu m = new JMenu(_GUI.T.VariantColumn_fillPopup_add());
            m.setIcon(new AbstractIcon(IconKey.ICON_ADD, 18));
            fillPopupWithAddAdditionalSubmenu(popup, m, dm, link);
            fillPopupWithPluginSettingsButton(popup, link);
        }
    }

    public void fillPopupWithPluginSettingsButton(final JPopupMenu popup, final CrawledLink link) {
        popup.add(new JMenuItem(new BasicAction() {
            {
                setSmallIcon(new BadgeIcon(new AbstractIcon(IconKey.ICON_SETTINGS, 18), DomainInfo.getInstance(link.getDownloadLink().getDefaultPlugin().getHost()).getIcon(10), 0, 0).crop(18, 18));
                setName(_GUI.T.VariantColumn_fillPopup_settings(link.getDownloadLink().getDefaultPlugin().getHost()));
            }

            @Override
            public void actionPerformed(final ActionEvent e) {
                popup.setVisible(false);
                JsonConfig.create(GraphicalUserInterfaceSettings.class).setConfigViewVisible(true);
                JDGui.getInstance().setContent(ConfigurationView.getInstance(), true);
                ConfigurationView.getInstance().setSelectedSubPanel(PluginSettings.class);
                ConfigurationView.getInstance().getSubPanel(PluginSettings.class).setPlugin(link.getDownloadLink().getDefaultPlugin().getLazyP());
            }
        }));
    }

    public void fillPopupWithAddAdditionalSubmenu(final JPopupMenu popup, JMenu m, ComboBoxModel<LinkVariant> dm, final CrawledLink link) {
        final HashSet<String> dupeSet = new HashSet<String>();
        final CrawledPackage parent = link.getParentNode();
        final boolean readL = parent.getModifyLock().readLock();
        try {
            for (CrawledLink cl : parent.getChildren()) {
                dupeSet.add(cl.getLinkID());
            }
        } finally {
            parent.getModifyLock().readUnlock(readL);
        }
        for (int i = 0; i < dm.getSize(); i++) {
            final LinkVariant o = dm.getElementAt(i);
            ExtMenuItem mi;
            m.add(mi = new ExtMenuItem(new BasicAction() {
                private CrawledLink cl;
                {
                    DownloadLink dl = link.getDownloadLink();
                    final DownloadLink dllink = new DownloadLink(link.getDownloadLink().getDefaultPlugin(), link.getDownloadLink().getView().getDisplayName(), link.getDownloadLink().getHost(), link.getDownloadLink().getPluginPatternMatcher(), true);
                    dllink.setProperties(link.getDownloadLink().getProperties());
                    dllink.setProperty("DUMMY", true);
                    cl = new CrawledLink(dllink);
                    setSmallIcon(o._getIcon(link));
                    setName(o._getName(link));
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
    }

    public void fillPopupWithVariants(final JPopupMenu popup, AbstractNode value, LinkVariant selected, ComboBoxModel<LinkVariant> dm) {
        super.fillPopup(popup, value, selected, dm);
    }

    @Override
    protected String modelItemToString(LinkVariant selectedItem, AbstractNode value) {
        if (selectedItem == null) {
            return null;
        }
        return selectedItem._getName(value);
    }

    @Override
    protected String getTooltipText(AbstractNode obj) {
        LinkVariant variant = getSelectedItem(obj);
        if (variant == null) {
            return null;
        }
        return variant._getTooltipDescription(obj);
    }

    protected Icon modelItemToIcon(LinkVariant selectedItem, AbstractNode value) {
        if (selectedItem == null) {
            return null;
        }
        return selectedItem._getIcon(value);
    }

    @Override
    public boolean isEditable(final AbstractNode object) {
        if (object instanceof CrawledLink) {
            if (((CrawledLink) object).hasVariantSupport()) {
                return ((CrawledLink) object).gethPlugin().hasVariantToChooseFrom(((CrawledLink) object).getDownloadLink());
            }
        } else if (false && object instanceof DownloadLink) {
            /* DownloadTable does not have VariantSupport yet */
            if (((DownloadLink) object).hasVariantSupport()) {
                return ((DownloadLink) object).getDefaultPlugin().hasVariantToChooseFrom(((DownloadLink) object));
            }
        }
        return false;
    }

    protected LinkVariant getSelectedItem(AbstractNode object) {
        if (object instanceof CrawledLink) {
            if (!((CrawledLink) object).hasVariantSupport()) {
                return null;
            }
            return ((CrawledLink) object).gethPlugin().getActiveVariantByLink(((CrawledLink) object).getDownloadLink());
        } else if (false && object instanceof DownloadLink) {
            /* DownloadTable does not have VariantSupport yet */
            if (!((DownloadLink) object).hasVariantSupport()) {
                return null;
            }
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
        List<? extends LinkVariant> variants;
        if (object instanceof CrawledLink) {
            if (!((CrawledLink) object).hasVariantSupport()) {
                return null;
            }
            variants = ((CrawledLink) object).gethPlugin().getVariantsByLink(((CrawledLink) object).getDownloadLink());
            return new VariantsModel(variants);
        } else if (object instanceof DownloadLink) {
            if (!((DownloadLink) object).hasVariantSupport()) {
                return null;
            }
            variants = ((DownloadLink) object).getDefaultPlugin().getVariantsByLink(((DownloadLink) object));
            return new VariantsModel(variants);
        }
        return null;
    }

    @Override
    public boolean isVisible(boolean savedValue) {
        return (autoVisible || alwaysVisible) && savedValue;
    }

    public boolean setAutoVisible(boolean b) {
        if (b == autoVisible) {
            return false;
        }
        this.autoVisible = b;
        return true;
    }
}
