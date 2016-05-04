package org.jdownloader.plugins.components.youtube.configpanel;

import java.awt.AlphaComposite;
import java.awt.Composite;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import javax.swing.JPopupMenu;
import javax.swing.JSeparator;

import org.appwork.storage.config.handler.BooleanKeyHandler;
import org.appwork.swing.components.CheckBoxIcon;
import org.appwork.swing.exttable.ExtColumn;
import org.appwork.uio.InputDialogInterface;
import org.appwork.uio.UIOManager;
import org.appwork.utils.CounterMap;
import org.appwork.utils.StringUtils;
import org.appwork.utils.swing.dialog.InputDialog;
import org.jdownloader.actions.AppAction;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.AbstractIcon;
import org.jdownloader.plugins.components.youtube.VariantIDStorable;
import org.jdownloader.plugins.components.youtube.variants.AudioVariant;
import org.jdownloader.plugins.components.youtube.variants.DescriptionVariant;
import org.jdownloader.plugins.components.youtube.variants.ImageVariant;
import org.jdownloader.plugins.components.youtube.variants.SubtitleVariant;
import org.jdownloader.plugins.components.youtube.variants.VideoVariant;
import org.jdownloader.settings.staticreferences.CFG_YOUTUBE;
import org.jdownloader.updatev2.gui.LAFOptions;

import jd.gui.swing.jdgui.BasicJDTable;

public class VariantsMapTable extends BasicJDTable<AbstractVariantWrapper> {

    public VariantsMapTable(VariantsMapTableModel model) {
        super(model);
        setSearchEnabled(true);
        setColumnBottonVisibility(false);
    }

    @Override
    public VariantsMapTableModel getModel() {
        return (VariantsMapTableModel) super.getModel();
    }

    @Override
    protected JPopupMenu onContextMenu(JPopupMenu popup, AbstractVariantWrapper contextObject, final List<AbstractVariantWrapper> selection, ExtColumn<AbstractVariantWrapper> column, MouseEvent mouseEvent) {

        popup.add(new AppAction() {
            {
                setSmallIcon(new AbstractIcon(IconKey.ICON_ADD, 20));
                setName(_GUI.T.youtube_config_add_collection());
            }

            @Override
            public void actionPerformed(ActionEvent e) {

                ArrayList<VariantIDStorable> variants = new ArrayList<VariantIDStorable>();

                HashSet<String> type = new HashSet<String>();
                HashSet<String> extensions = new HashSet<String>();

                AbstractVariantWrapper best = null;

                for (AbstractVariantWrapper w : selection) {
                    variants.add(w.getVariableIDStorable());
                    if (best == null || best.variant.compareTo(w.variant) == -1) {
                        best = w;
                    }
                    extensions.add(w.variant.getContainer().getLabel());
                    if (w.variant instanceof AudioVariant) {
                        type.add(w.variant.getGroup().getLabel());
                    }
                    if (w.variant instanceof SubtitleVariant) {
                        type.add(w.variant.getGroup().getLabel());
                    }
                    if (w.variant instanceof DescriptionVariant) {
                        type.add(w.variant.getGroup().getLabel());
                    }
                    if (w.variant instanceof ImageVariant) {
                        type.add(w.variant.getGroup().getLabel());
                    }
                    if (w.variant instanceof VideoVariant) {

                        switch (((VideoVariant) w.variant).getProjection()) {
                        case ANAGLYPH_3D:

                            type.add("3D " + w.variant.getGroup().getLabel());
                            break;
                        case NORMAL:
                            type.add(w.variant.getGroup().getLabel());
                            break;
                        case SPHERICAL:
                            type.add("360° " + w.variant.getGroup().getLabel());
                            break;
                        case SPHERICAL_3D:
                            type.add("3D 360° " + w.variant.getGroup().getLabel());

                            break;
                        }
                    }
                }

                StringBuilder sb = new StringBuilder();
                if (type.size() == 1) {
                    sb.append(type.iterator().next());

                }
                if (extensions.size() == 1) {
                    if (sb.length() > 0) {
                        sb.append(" ");
                    }
                    sb.append(extensions.iterator().next());

                }
                if (sb.length() > 0) {
                    sb.append(" ");
                }
                sb.append("Max " + best.variant._getName(null));

                String name = UIOManager.I().show(InputDialogInterface.class, new InputDialog(0, _GUI.T.lit_name(), "", sb.toString(), null, null, null)).getText();
                if (StringUtils.isNotEmpty(name)) {
                    YoutubeVariantCollection link = new YoutubeVariantCollection(name, variants);
                    List<YoutubeVariantCollection> links = CFG_YOUTUBE.CFG.getCollections();
                    links.add(link);
                    CFG_YOUTUBE.CFG.setCollections(links);
                }
            }

        });

        popup.add(new JSeparator());
        popup.add(new AppAction() {
            {
                setSmallIcon(new CheckBoxIcon(true));
                setName(_GUI.T.lit_enable());
            }

            @Override
            public void actionPerformed(ActionEvent e) {
                for (AbstractVariantWrapper w : selection) {
                    w.setEnabled(true);
                }
                getModel().updateEnabledMap();
            }

        });

        popup.add(new AppAction() {
            {
                setSmallIcon(new CheckBoxIcon(false));
                setName(_GUI.T.lit_disable());
            }

            @Override
            public void actionPerformed(ActionEvent e) {
                for (AbstractVariantWrapper w : selection) {
                    w.setEnabled(false);
                }
                getModel().updateEnabledMap();
            }

        });
        return popup;
    }

    private final AlphaComposite alpha = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.3f);

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D) g;
        Composite comp = g2.getComposite();
        final Rectangle visibleRect = this.getVisibleRect();
        g2.setComposite(alpha);
        if (getModel().getTableData().size() == 0) {
            g2.setColor(LAFOptions.getInstance().getColorForTableAccountErrorRowBackground());
            g2.fillRect(visibleRect.x, visibleRect.y, visibleRect.x + visibleRect.width, visibleRect.y + visibleRect.height);
            g2.setComposite(comp);
            g2.setFont(g2.getFont().deriveFont(g2.getFont().getStyle() ^ Font.BOLD));
            String str = _GUI.T.youtube_empty_table();
            g2.setColor(LAFOptions.getInstance().getColorForTableAccountErrorRowForeground());
            g2.drawString(str, (getWidth() - g2.getFontMetrics().stringWidth(str)) / 2, (int) (getHeight() * 0.5d));
        }

        g2.setComposite(comp);
    }

    protected void initAlternateRowHighlighter() {
        final BooleanKeyHandler enabled = LAFOptions.TABLE_ALTERNATE_ROW_HIGHLIGHT_ENABLED;
        if (enabled != null && enabled.isEnabled()) {
            // this.getModel().addExtComponentRowHighlighter(new
            // AlternateHighlighter<AbstractVariantWrapper>((LAFOptions.getInstance().getColorForTableAlternateRowForeground()),
            // (LAFOptions.getInstance().getColorForTableAlternateRowBackground()), null));

        }
    }

    public void load() {
        getModel().load();
    }

    public void save() {
        getModel().save();
    }

    public void setSelectionByLink(YoutubeVariantCollection link) {
        if (link == null) {
            return;
        }

        ArrayList<AbstractVariantWrapper> selection = new ArrayList<AbstractVariantWrapper>();
        HashSet<String> idSet = link.createUniqueIDSet();

        for (AbstractVariantWrapper v : getModel().getTableData()) {
            if (idSet.contains(v.getVariableIDStorable().createUniqueID())) {
                selection.add(v);
            } else if (StringUtils.equals(v.getVariableIDStorable().getContainer(), link.getGroupingID())) {
                selection.add(v);
            } else if (StringUtils.equals(v.getVariableIDStorable().createGroupingID(), link.getGroupingID())) {
                selection.add(v);
            }

        }
        getModel().setSelectedObjects(selection);
        scrollToSelection(0);
    }

    public void onEnabledMapUpdate(CounterMap<String> enabledMap) {
    }

}
