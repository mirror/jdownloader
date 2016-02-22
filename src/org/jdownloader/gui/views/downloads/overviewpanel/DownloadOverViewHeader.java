package org.jdownloader.gui.views.downloads.overviewpanel;

import java.awt.Insets;

import javax.swing.JPopupMenu;
import javax.swing.JSeparator;

import org.appwork.swing.components.ExtButton;
import org.jdownloader.controlling.AggregatedNumbers;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.components.CheckboxMenuItem;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.linkgrabber.properties.AbstractPanelHeader;
import org.jdownloader.images.NewTheme;
import org.jdownloader.settings.staticreferences.CFG_GUI;
import org.jdownloader.updatev2.gui.LAFOptions;

public class DownloadOverViewHeader extends AbstractPanelHeader {

    private final DownloadOverview overView;

    public DownloadOverViewHeader(DownloadOverview overView) {
        super(_GUI.T.OverViewHeader_OverViewHeader_(), NewTheme.I().getIcon(IconKey.ICON_DOWNLOAD, 16));
        this.overView = overView;
    }

    protected void onCloseAction() {
    }

    @Override
    protected void onSettings(ExtButton options) {
        final JPopupMenu pu = new JPopupMenu();
        final CheckboxMenuItem total = new CheckboxMenuItem(_GUI.T.OverViewHeader_actionPerformed_total_(), CFG_GUI.OVERVIEW_PANEL_TOTAL_INFO_VISIBLE);
        final CheckboxMenuItem filtered = new CheckboxMenuItem(_GUI.T.OverViewHeader_actionPerformed_visible_only_(), CFG_GUI.OVERVIEW_PANEL_VISIBLE_ONLY_INFO_VISIBLE);
        final CheckboxMenuItem selected = new CheckboxMenuItem(_GUI.T.OverViewHeader_actionPerformed_selected_(), CFG_GUI.OVERVIEW_PANEL_SELECTED_INFO_VISIBLE);
        pu.add(new CheckboxMenuItem(_GUI.T.OverViewHeader_disabled(), CFG_GUI.OVERVIEW_PANEL_DOWNLOAD_PANEL_INCLUDE_DISABLED_LINKS));
        pu.add(new CheckboxMenuItem(_GUI.T.OverViewHeader_actionPerformed_smart_(), CFG_GUI.OVERVIEW_PANEL_SMART_INFO_VISIBLE, total, filtered, selected));
        pu.add(new JSeparator(JSeparator.HORIZONTAL));
        pu.add(total);
        pu.add(filtered);
        pu.add(selected);
        pu.add(new JSeparator(JSeparator.HORIZONTAL));
        pu.add(new CheckboxMenuItem(_GUI.T.OverViewHeader_actionPerformed_quicksettings(), CFG_GUI.DOWNLOAD_PANEL_OVERVIEW_SETTINGS_VISIBLE));
        if (overView != null) {
            for (final DataEntry<AggregatedNumbers> de : overView.createDataEntries()) {
                if (de.getVisibleKeyHandler() != null) {
                    pu.add(new CheckboxMenuItem(de.getPopupLabel(), de.getVisibleKeyHandler()));
                }
            }
        }
        final Insets insets = LAFOptions.getInstance().getExtension().customizePopupBorderInsets();
        pu.show(options, -insets.left, -pu.getPreferredSize().height + insets.bottom);
    }
}
