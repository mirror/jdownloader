package org.jdownloader.gui.views.downloads.bottombar;

import java.lang.reflect.InvocationTargetException;

import javax.swing.Icon;
import javax.swing.JComponent;

import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

import org.appwork.storage.config.JsonConfig;
import org.appwork.utils.NullsafeAtomicReference;
import org.appwork.utils.swing.EDTHelper;
import org.jdownloader.controlling.contextmenu.MenuItemData;
import org.jdownloader.controlling.contextmenu.MenuLink;
import org.jdownloader.extensions.ExtensionNotLoadedException;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
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
        private final DownloadsTable                                                                  table;

        protected static FilterCombo                                                                  INSTANCE;

        private NullsafeAtomicReference<PackageControllerTableModelFilter<FilePackage, DownloadLink>> appliedFilter = new NullsafeAtomicReference<PackageControllerTableModelFilter<FilePackage, DownloadLink>>(null);

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
            super(new View[] { View.ALL, View.RUNNING, View.FAILED, View.SKIPPED, View.SUCCESSFUL, View.TODO });
            this.setToolTipText(_GUI._.PseudoCombo_PseudoCombo_tt_());
            this.table = (DownloadsTable) DownloadsTableModel.getInstance().getTable();
            View view = (View) CFG_GUI.DOWNLOAD_VIEW.getValue();
            if (view == null || !JsonConfig.create(GraphicalUserInterfaceSettings.class).isSaveDownloadViewCrossSessionEnabled()) {
                view = View.ALL;
            }
            setSelectedItem(view);
        }

        @Override
        public void onChanged(View value) {
            PackageControllerTableModelFilter<FilePackage, DownloadLink> newFilter = getFilter(value);
            PackageControllerTableModelFilter<FilePackage, DownloadLink> oldFilter = appliedFilter.getAndSet(newFilter);
            if (oldFilter != null) table.getModel().removeFilter(oldFilter);
            if (newFilter != null) table.getModel().addFilter(newFilter);
            table.getModel().recreateModel(true);
            CFG_GUI.DOWNLOAD_VIEW.setValue(value);
        }

        private PackageControllerTableModelFilter<FilePackage, DownloadLink> getFilter(View value) {
            if (value == null) return null;
            switch (value) {
            case ALL:
                return null;
            case SKIPPED:
                return new PackageControllerTableModelFilter<FilePackage, DownloadLink>() {

                    @Override
                    public boolean isFilteringPackageNodes() {
                        return false;
                    }

                    @Override
                    public boolean isFilteringChildrenNodes() {
                        return true;
                    }

                    @Override
                    public boolean isFiltered(DownloadLink v) {
                        return v.getSkipReason() == null;
                    }

                    @Override
                    public boolean isFiltered(FilePackage e) {
                        return false;
                    }

                    @Override
                    public int getComplexity() {
                        return 0;
                    }
                };
            case FAILED:
                return new PackageControllerTableModelFilter<FilePackage, DownloadLink>() {

                    @Override
                    public boolean isFilteringPackageNodes() {
                        return false;
                    }

                    @Override
                    public boolean isFilteringChildrenNodes() {
                        return true;
                    }

                    @Override
                    public boolean isFiltered(DownloadLink v) {
                        return !(FinalLinkState.CheckFailed(v.getFinalLinkState()));
                    }

                    @Override
                    public boolean isFiltered(FilePackage e) {
                        return false;
                    }

                    @Override
                    public int getComplexity() {
                        return 0;
                    }
                };
            case RUNNING:
                return new PackageControllerTableModelFilter<FilePackage, DownloadLink>() {

                    @Override
                    public boolean isFilteringPackageNodes() {
                        return false;
                    }

                    @Override
                    public boolean isFilteringChildrenNodes() {
                        return true;
                    }

                    @Override
                    public boolean isFiltered(DownloadLink v) {
                        return !v.isEnabled() || v.getDownloadLinkController() == null;
                    }

                    @Override
                    public boolean isFiltered(FilePackage e) {
                        return false;
                    }

                    @Override
                    public int getComplexity() {
                        return 0;
                    }
                };
            case SUCCESSFUL:
                return new PackageControllerTableModelFilter<FilePackage, DownloadLink>() {

                    @Override
                    public boolean isFilteringPackageNodes() {
                        return false;
                    }

                    @Override
                    public boolean isFilteringChildrenNodes() {
                        return true;
                    }

                    @Override
                    public boolean isFiltered(DownloadLink v) {
                        return !(FinalLinkState.CheckFinished(v.getFinalLinkState()));
                    }

                    @Override
                    public boolean isFiltered(FilePackage e) {
                        return false;
                    }

                    @Override
                    public int getComplexity() {
                        return 0;
                    }
                };
            case TODO:
                return new PackageControllerTableModelFilter<FilePackage, DownloadLink>() {

                    @Override
                    public boolean isFilteringPackageNodes() {
                        return false;
                    }

                    @Override
                    public boolean isFilteringChildrenNodes() {
                        return true;
                    }

                    @Override
                    public boolean isFiltered(DownloadLink v) {
                        return v.getFinalLinkState() != null || v.getDownloadLinkController() != null;
                    }

                    @Override
                    public boolean isFiltered(FilePackage e) {
                        return false;
                    }

                    @Override
                    public int getComplexity() {
                        return 0;
                    }
                };
            }
            return null;
        }

    }

    public QuickFilterMenuItem() {
        super();
        setName(_GUI._.QuickFilterMenuItem_QuickFilterMenuItem());
        setIconKey(IconKey.ICON_FILTER);
        setVisible(true);
        //
    }

    public JComponent createItem() throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, ClassNotFoundException, NoSuchMethodException, SecurityException, ExtensionNotLoadedException {

        return FilterCombo.getInstance();
    }
}
