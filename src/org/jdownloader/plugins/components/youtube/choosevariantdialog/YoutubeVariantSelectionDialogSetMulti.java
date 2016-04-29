package org.jdownloader.plugins.components.youtube.choosevariantdialog;

import java.util.List;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import javax.swing.event.ListSelectionEvent;

import org.appwork.swing.MigPanel;
import org.appwork.swing.components.ExtCheckBox;
import org.appwork.utils.CounterMap;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.plugins.components.youtube.configpanel.AbstractVariantWrapper;
import org.jdownloader.plugins.components.youtube.variants.SubtitleVariant;
import org.jdownloader.plugins.components.youtube.variants.VariantGroup;
import org.jdownloader.plugins.components.youtube.variants.VariantInfo;
import org.jdownloader.settings.staticreferences.CFG_YOUTUBE;

public class YoutubeVariantSelectionDialogSetMulti extends YoutubeVariantSelectionDialog {

    private CounterMap<String> matchingLinks;
    private int                linksCount;
    private VariantGroup       group;
    private MigPanel           alternativePanel;
    private ExtCheckBox        checkbox;
    private JLabel             alternativesLabel;

    public YoutubeVariantSelectionDialogSetMulti(CounterMap<String> matchingLinks, VariantGroup g, int links, List<VariantInfo> variants) {
        super(g, _GUI.T.youtube_variant_selection_dialog_title2(g.getLabel(), links), _GUI.T.lit_choose(), variants);
        this.matchingLinks = matchingLinks;
        this.group = g;
        this.linksCount = links;
    }

    @Override
    public JComponent layoutDialogContent() {
        JComponent ret = super.layoutDialogContent();
        alternativePanel = new MigPanel("ins 0", "[grow,fill]0[]", "[]");

        alternativePanel.add(alternativesLabel = new JLabel(_GUI.T.youtube_mass_change_or_add_choose_help()), "");
        alternativesLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        alternativePanel.add(checkbox = new ExtCheckBox(CFG_YOUTUBE.CHOOSE_ALTERNATIVE_FOR_MASS_CHANGE_OR_ADD_DIALOG), "hidemode 3,gapleft 2,height " + alternativesLabel.getPreferredSize().height + "!");
        if (linksCount > 1) {
            ret.add(alternativePanel, "spanx,growx,pushx");
        }
        valueChanged(null);
        return ret;
    }

    public boolean isAutoAlternativesEnabled() {
        return checkbox.isSelected();
    }

    @Override
    public void valueChanged(ListSelectionEvent e) {
        super.valueChanged(e);

        List<AbstractVariantWrapper> selectedObject = table.getModel().getSelectedObjects(1);
        if (selectedObject == null || selectedObject.size() == 0) {
            checkbox.setVisible(false);
            alternativesLabel.setEnabled(false);
            alternativesLabel.setText(_GUI.T.youtube_mass_change_or_add_choose_help());

        } else {

            AbstractVariantWrapper s = table.getModel().getTableData().get(table.getSelectedRow());
            int matching = matchingLinks.getInt(s.getVariableIDStorable().createUniqueID());
            if (matching == linksCount) {
                checkbox.setVisible(false);
                alternativesLabel.setEnabled(false);
                alternativesLabel.setText(_GUI.T.youtube_mass_change_or_add_choose_ok(group.getLabel(), linksCount));
            } else {
                checkbox.setVisible(true);
                alternativesLabel.setEnabled(true);
                alternativesLabel.setText(_GUI.T.youtube_mass_change_or_add_choose_alternative(group.getLabel(), matching, linksCount));
            }

        }

    }

    @Override
    protected CustomVariantsMapTableModel createTableModel() {
        return new CustomVariantsMapTableModel(variantWrapperList, null) {
            private AutoResizingIntColumn matches;

            @Override
            protected void onStructureChanged(List<AbstractVariantWrapper> newtableData) {
                super.onStructureChanged(newtableData);
                if (matches != null) {
                    setColumnVisible(matches, linksCount > 1);
                }
            }

            @Override
            protected void initColumns() {
                super.initColumns();
                addColumn(matches = new AutoResizingIntColumn(_GUI.T.youtube_matching_links()) {
                    @Override
                    public String getStringValue(AbstractVariantWrapper value) {
                        if (value.variant instanceof SubtitleVariant) {
                            return matchingLinks.getInt(value.variant._getUniqueId()) + "/" + linksCount;
                        }
                        return matchingLinks.getInt(value.getVariableIDStorable().createUniqueID()) + "/" + linksCount;
                    }

                    @Override
                    public boolean isAutoWidthEnabled() {
                        return true;
                    }

                    @Override
                    public int getInt(AbstractVariantWrapper value) {
                        if (value.variant instanceof SubtitleVariant) {
                            return matchingLinks.getInt(value.variant._getUniqueId());
                        }
                        return matchingLinks.getInt(value.getVariableIDStorable().createUniqueID());
                    }

                });
            }
        };
    }

    @Override
    protected String getDescriptionText() {
        return _GUI.T.youtube_coose_variant_help();
    }
}
