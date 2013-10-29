package org.jdownloader.gui.views.downloads.bottombar;

import java.lang.reflect.InvocationTargetException;

import javax.swing.Icon;
import javax.swing.JComponent;

import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

import org.appwork.storage.config.JsonConfig;
import org.appwork.utils.swing.EDTHelper;
import org.jdownloader.controlling.contextmenu.MenuItemData;
import org.jdownloader.controlling.contextmenu.MenuLink;
import org.jdownloader.extensions.ExtensionNotLoadedException;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.SelectionInfo;
import org.jdownloader.gui.views.components.PseudoCombo;
import org.jdownloader.gui.views.components.packagetable.PackageControllerTableModelFilter;
import org.jdownloader.gui.views.downloads.View;
import org.jdownloader.gui.views.downloads.table.DownloadsTable;
import org.jdownloader.gui.views.downloads.table.DownloadsTableModel;
import org.jdownloader.plugins.FinalLinkState;
import org.jdownloader.settings.GraphicalUserInterfaceSettings;
import org.jdownloader.settings.staticreferences.CFG_GUI;

public class QuickFilterMenuItem extends MenuItemData implements MenuLink {
    public static final class FilterCombo extends PseudoCombo<View> {
        private final DownloadsTable                                         table;
        private PackageControllerTableModelFilter<FilePackage, DownloadLink> filter = new PackageControllerTableModelFilter<FilePackage, DownloadLink>() {

                                                                                        public void reset() {
                                                                                        }

                                                                                        public boolean isFiltered(DownloadLink v) {
                                                                                            switch (selectedItem) {
                                                                                            case RUNNING:
                                                                                                return !v.isEnabled() || v.getDownloadLinkController() == null;
                                                                                            case SUCCESSFUL:
                                                                                                return !(FinalLinkState.CheckFinished(v.getFinalLinkState()));
                                                                                            case FAILED:
                                                                                                return !(FinalLinkState.CheckFailed(v.getFinalLinkState()));
                                                                                            case TODO:
                                                                                                return v.getFinalLinkState() != null || v.getDownloadLinkController() != null;
                                                                                            }
                                                                                            return false;
                                                                                        }

                                                                                        public boolean isFiltered(FilePackage e) {
                                                                                            return false;
                                                                                        }

                                                                                        public boolean highlightFilter() {
                                                                                            return true;
                                                                                        }
                                                                                    };
        protected static FilterCombo                                         INSTANCE;

        public static FilterCombo getInstance() {
            return new EDTHelper<FilterCombo>() {

                @Override
                public FilterCombo edtRun() {
                    if (INSTANCE != null) return INSTANCE;
                    INSTANCE = new FilterCombo();
                    return INSTANCE;
                }

            }.getReturnValue();

        }

        @Override
        protected Icon getIcon(View v, boolean closed) {
            return v.getIcon();
        }

        @Override
        protected String getLabel(View v, boolean closed) {
            return v.getLabel();
        }

        private FilterCombo() {
            super(new View[] { View.ALL, View.RUNNING, View.FAILED, View.SUCCESSFUL, View.TODO });
            this.setToolTipText(_GUI._.PseudoCombo_PseudoCombo_tt_());
            this.table = (DownloadsTable) DownloadsTableModel.getInstance().getTable();
            if (JsonConfig.create(GraphicalUserInterfaceSettings.class).isSaveDownloadViewCrossSessionEnabled()) {
                setSelectedItem((View) CFG_GUI.DOWNLOAD_VIEW.getValue());
            } else {
                setSelectedItem(View.ALL);
            }
        }

        @Override
        public void onChanged(View value) {
            if (View.ALL.equals(value)) {
                table.getModel().removeFilter(filter);
            } else {
                table.getModel().addFilter(filter);
            }
            table.getModel().recreateModel(true);
            CFG_GUI.DOWNLOAD_VIEW.setValue(value);
        }
    }

    public QuickFilterMenuItem() {
        super();
        setName(_GUI._.QuickFilterMenuItem_QuickFilterMenuItem());
        setIconKey(IconKey.ICON_FILTER);
        setVisible(true);
        //
    }

    public JComponent createItem(SelectionInfo<?, ?> selection) throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, ClassNotFoundException, NoSuchMethodException, SecurityException, ExtensionNotLoadedException {

        return FilterCombo.getInstance();
    }
}
