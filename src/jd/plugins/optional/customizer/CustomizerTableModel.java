package jd.plugins.optional.customizer;

import java.util.ArrayList;

import jd.gui.swing.components.table.JDTableModel;
import jd.plugins.optional.customizer.columns.DownloadDirColumn;
import jd.plugins.optional.customizer.columns.EnabledColumn;
import jd.plugins.optional.customizer.columns.ExtractColumn;
import jd.plugins.optional.customizer.columns.MatchCountColumn;
import jd.plugins.optional.customizer.columns.NameColumn;
import jd.plugins.optional.customizer.columns.PackageNameColumn;
import jd.plugins.optional.customizer.columns.PasswordColumn;
import jd.plugins.optional.customizer.columns.PriorityColumn;
import jd.plugins.optional.customizer.columns.RegexColumn;
import jd.plugins.optional.customizer.columns.SubDirectoryColumn;
import jd.utils.locale.JDL;

public class CustomizerTableModel extends JDTableModel {

    private static final long serialVersionUID = -8877812970684393642L;
    private static final String JDL_PREFIX = "jd.plugins.optional.customizer.CustomizerTableModel.";

    private ArrayList<CustomizeSetting> settings;

    public CustomizerTableModel(String configname, ArrayList<CustomizeSetting> settings) {
        super(configname);

        this.settings = settings;
    }

    protected void initColumns() {
        this.addColumn(new NameColumn(JDL.L(JDL_PREFIX + "name", "Name"), this));
        this.addColumn(new EnabledColumn(JDL.L(JDL_PREFIX + "enabled", "Enabled"), this));
        this.addColumn(new RegexColumn(JDL.L(JDL_PREFIX + "regex", "Regex"), this));
        this.addColumn(new PackageNameColumn(JDL.L(JDL_PREFIX + "packageName", "FilePackage name"), this));
        this.addColumn(new DownloadDirColumn(JDL.L(JDL_PREFIX + "downloadDir", "Download directory"), this));
        this.addColumn(new SubDirectoryColumn(JDL.L(JDL_PREFIX + "subDirectory", "Use SubDirectory"), this));
        this.addColumn(new ExtractColumn(JDL.L(JDL_PREFIX + "extract", "Extract"), this));
        this.addColumn(new PasswordColumn(JDL.L(JDL_PREFIX + "password", "Password"), this));
        this.addColumn(new PriorityColumn(JDL.L(JDL_PREFIX + "priority", "Priority"), this));
        this.addColumn(new MatchCountColumn(JDL.L(JDL_PREFIX + "matchCount", "Match count from Start"), this));
    }

    @Override
    public void refreshModel() {
        synchronized (list) {
            list.clear();
            list.addAll(settings);
        }
    }

    public ArrayList<CustomizeSetting> getSettings() {
        return settings;
    }

    public void setSettings(ArrayList<CustomizeSetting> settings) {
        this.settings = settings;
    }

}
