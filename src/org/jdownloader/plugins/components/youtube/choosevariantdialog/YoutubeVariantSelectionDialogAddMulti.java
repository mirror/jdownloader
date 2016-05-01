package org.jdownloader.plugins.components.youtube.choosevariantdialog;

import java.util.List;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.event.ListSelectionEvent;

import org.appwork.swing.MigPanel;
import org.appwork.swing.components.ExtCheckBox;
import org.appwork.utils.CounterMap;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.plugins.components.youtube.configpanel.AbstractVariantWrapper;
import org.jdownloader.plugins.components.youtube.variants.SubtitleVariant;
import org.jdownloader.plugins.components.youtube.variants.VariantInfo;
import org.jdownloader.settings.staticreferences.CFG_YOUTUBE;

public class YoutubeVariantSelectionDialogAddMulti extends YoutubeVariantSelectionDialog {

    private CounterMap<String> matchingLinks;
    private int                linksCount;

    private MigPanel    alternativePanel;
    private ExtCheckBox checkbox;
    private JLabel      alternativesLabel;

    public YoutubeVariantSelectionDialogAddMulti(CounterMap<String> matchingLinks, int linkCount, List<VariantInfo> variants) {
        super(null, _GUI.T.youtube_variant_add_variant_dialog_title(), _GUI.T.lit_add(), variants);
        this.matchingLinks = matchingLinks;
        this.linksCount = linkCount;

    }

    @Override
    public JComponent layoutDialogContent() {
        JComponent ret = super.layoutDialogContent();
        alternativePanel = new MigPanel("ins 0", "[grow,fill]0[]", "[]");

        alternativePanel.add(alternativesLabel = new JLabel(_GUI.T.youtube_variant_add_variant_dialog_help()), "");
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

    }

    @Override
    protected CustomVariantsMapTable createTable(CustomVariantsMapTableModel model) {
        CustomVariantsMapTable ret = super.createTable(model);
        ret.getSelectionModel().setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        return ret;
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
        return _GUI.T.youtube_add_variants_help(linksCount);
    }
}
