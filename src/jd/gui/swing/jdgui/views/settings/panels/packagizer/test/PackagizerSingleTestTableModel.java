package jd.gui.swing.jdgui.views.settings.panels.packagizer.test;

import javax.swing.Icon;

import jd.controlling.linkcrawler.CrawledLink;
import jd.gui.swing.jdgui.views.settings.panels.packagizer.PackagizerFilterRuleDialog.RuleMatcher;

import org.appwork.swing.exttable.ExtTableModel;
import org.appwork.swing.exttable.columns.ExtFileSizeColumn;
import org.appwork.swing.exttable.columns.ExtTextColumn;
import org.appwork.utils.Files;
import org.jdownloader.controlling.packagizer.PackagizerController;
import org.jdownloader.controlling.packagizer.PackagizerRuleWrapper;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;

public class PackagizerSingleTestTableModel extends ExtTableModel<CrawledLink> {

    /**
	 * 
	 */
    private static final long serialVersionUID = -2808142012367413057L;
    private RuleMatcher       rule;

    public PackagizerSingleTestTableModel(RuleMatcher rule) {
        super("PackagizerSingleTestTableModel");
        this.rule = rule;
    }

    @Override
    protected void initColumns() {
        addColumn(new ExtTextColumn<CrawledLink>(_GUI._.PackagizerSingleTestTableModel_initColumns_matches_()) {

            /**
			 * 
			 */
            private static final long serialVersionUID = -8023575216919771671L;

            public int getDefaultWidth() {

                return 60;
            }

            public boolean isPaintWidthLockIcon() {
                return false;
            }

            protected boolean isDefaultResizable() {

                return false;
            }

            @Override
            protected Icon getIcon(CrawledLink value) {
                if (Boolean.TRUE.equals(rule.getMatches())) {
                    return NewTheme.I().getIcon("true", 16);
                } else {
                    return NewTheme.I().getIcon("false", 16);
                }
            }

            @Override
            public String getStringValue(CrawledLink value) {
                return null;
            }
        });

        addColumn(new ExtTextColumn<CrawledLink>(_GUI._.ResultTableModel_initColumns_link_()) {
            /**
			 * 
			 */
            private static final long serialVersionUID = -4783542565565393138L;

            {
                editorField.setEditable(false);
            }

            @Override
            public boolean isEditable(final CrawledLink obj) {
                return true;
            }

            public boolean isDefaultVisible() {
                return false;
            }

            @Override
            public String getStringValue(CrawledLink value) {

                return value.getURL();
            }
        });
        addColumn(new ExtTextColumn<CrawledLink>(_GUI._.ResultTableModel_initColumns_filename_()) {
            /**
			 * 
			 */
            private static final long serialVersionUID = 2177279417196723941L;

            public int getDefaultWidth() {
                return 150;
            }

            protected boolean isDefaultResizable() {

                return true;
            }

            @Override
            public String getStringValue(CrawledLink value) {

                return value.getName();
            }
        });
        addColumn(new ExtTextColumn<CrawledLink>(_GUI._.ResultTableModel_initColumns_online_()) {
            /**
			 * 
			 */
            private static final long serialVersionUID = 7302435270960407040L;

            @Override
            protected String getTooltipText(final CrawledLink value) {

                switch (value.getLinkState()) {
                case OFFLINE:
                    return _GUI._.ConditionDialog_layoutDialogContent_offline_();
                case ONLINE:
                    return _GUI._.ConditionDialog_layoutDialogContent_online_();
                case TEMP_UNKNOWN:
                    return _GUI._.ConditionDialog_layoutDialogContent_uncheckable_();

                }
                return null;
            }

            public int getDefaultWidth() {

                return 100;
            }

            public boolean isPaintWidthLockIcon() {
                return false;
            }

            protected boolean isDefaultResizable() {

                return false;
            }

            @Override
            protected Icon getIcon(CrawledLink value) {

                switch (value.getLinkState()) {
                case OFFLINE:
                    return NewTheme.getInstance().getIcon("false", 16);
                case ONLINE:
                    return NewTheme.getInstance().getIcon("true", 16);
                case TEMP_UNKNOWN:
                    return NewTheme.getInstance().getIcon("checkbox_undefined", 16);

                }
                return null;

            }

            @Override
            public String getStringValue(CrawledLink value) {
                return null;
            }
        });

        addColumn(new ExtFileSizeColumn<CrawledLink>(_GUI._.ResultTableModel_initColumns_size_()) {
            /**
			 * 
			 */
            private static final long serialVersionUID = -3198585422004322732L;

            public int getDefaultWidth() {
                return 80;
            }

            public boolean isPaintWidthLockIcon() {
                return false;
            }

            protected boolean isDefaultResizable() {

                return false;
            }

            @Override
            protected long getBytes(CrawledLink o2) {
                return o2.getSize();
            }
        });
        addColumn(new ExtTextColumn<CrawledLink>(_GUI._.ResultTableModel_initColumns_filetype_()) {
            /**
			 * 
			 */
            private static final long serialVersionUID = -2845784989103727716L;

            public int getDefaultWidth() {
                return 65;
            }

            public boolean isPaintWidthLockIcon() {
                return false;
            }

            protected Icon getIcon(final CrawledLink value) {

                return value.getIcon();
            }

            protected boolean isDefaultResizable() {

                return false;
            }

            @Override
            public String getStringValue(CrawledLink value) {

                return Files.getExtension(value.getName());
            }
        });
        addColumn(new ExtTextColumn<CrawledLink>(_GUI._.ResultTableModel_initColumns_hoster()) {
            /**
			 * 
			 */
            private static final long serialVersionUID = 3673059407306941787L;

            public int getDefaultWidth() {
                return 100;
            }

            public boolean isPaintWidthLockIcon() {
                return false;
            }

            protected boolean isDefaultResizable() {

                return false;
            }

            protected Icon getIcon(final CrawledLink value) {
                return value.getDomainInfo().getFavIcon();
            }

            @Override
            public String getStringValue(CrawledLink value) {

                return value.getDomainInfo().getTld();
            }
        });

        addColumn(new ExtTextColumn<CrawledLink>(_GUI._.ResultTableModel_initColumns_source()) {
            /**
			 * 
			 */
            private static final long serialVersionUID = -6866522293823735808L;

            {
                editorField.setEditable(false);
            }

            public int getDefaultWidth() {
                return 250;
            }

            public boolean isDefaultVisible() {
                return false;
            }

            @Override
            public boolean isEditable(final CrawledLink obj) {
                return true;
            }

            @Override
            public String getStringValue(CrawledLink value) {
                StringBuilder sb = new StringBuilder();

                CrawledLink p = value;
                String last = p.getURL();
                while ((p = p.getSourceLink()) != null) {
                    if (last != null && last.equals(p.getURL())) continue;
                    sb.append("∟");
                    sb.append(p.getURL());
                    sb.append("\r\n");
                }
                return sb.toString().trim();
            }
        });

        addColumn(new ExtTextColumn<CrawledLink>(_GUI._.PackagizerSingleTestTableModel_initColumns_downloadfolder_()) {

            /**
			 * 
			 */
            private static final long serialVersionUID = -4648517798862470360L;

            @Override
            public boolean isEnabled(CrawledLink obj) {
                return rule.getRule().getDownloadDestination() != null;
            }

            public boolean isVisible(boolean savedValue) {
                return rule.getRule().getDownloadDestination() != null;
            }

            @Override
            public String getStringValue(CrawledLink value) {

                return value.getDesiredPackageInfo().getDestinationFolder();
            }
        });

        addColumn(new ExtTextColumn<CrawledLink>(_GUI._.PackagizerSingleTestTableModel_initColumns_packagename_()) {
            /**
			 * 
			 */
            private static final long serialVersionUID = -399219090023179926L;

            public boolean isVisible(boolean savedValue) {
                return rule.getRule().getPackageName() != null;
            }

            @Override
            public String getStringValue(CrawledLink value) {
                return value.getDesiredPackageInfo().getName();
            }
        });
        addColumn(new ExtTextColumn<CrawledLink>(_GUI._.PackagizerSingleTestTableModel_initColumns_filename_()) {
            /**
			 * 
			 */
            private static final long serialVersionUID = 5325984890669861167L;

            public boolean isVisible(boolean savedValue) {
                return rule.getRule().getFilename() != null;
            }

            @Override
            public String getStringValue(CrawledLink value) {
                return PackagizerController.getInstance().replaceVariables(rule.getRule().getFilename(), value, new PackagizerRuleWrapper(rule.getRule())) + "(" + rule.getRule().getFilename() + ")";
                // return rule.getFilenameFilter().getRegex()
            }
        });

    }
}
