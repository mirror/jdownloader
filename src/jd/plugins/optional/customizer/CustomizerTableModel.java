//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.plugins.optional.customizer;

import jd.gui.swing.components.table.JDTableModel;
import jd.plugins.optional.customizer.columns.DLPriorityColumn;
import jd.plugins.optional.customizer.columns.DownloadDirColumn;
import jd.plugins.optional.customizer.columns.EnabledColumn;
import jd.plugins.optional.customizer.columns.MatchCountColumn;
import jd.plugins.optional.customizer.columns.NameColumn;
import jd.plugins.optional.customizer.columns.PackageNameColumn;
import jd.plugins.optional.customizer.columns.PasswordColumn;
import jd.plugins.optional.customizer.columns.PostProcessingColumn;
import jd.plugins.optional.customizer.columns.RegexColumn;
import jd.plugins.optional.customizer.columns.SubDirectoryColumn;
import jd.utils.locale.JDL;

public class CustomizerTableModel extends JDTableModel {

    private static final long serialVersionUID = -8877812970684393642L;
    private static final String JDL_PREFIX = "jd.plugins.optional.customizer.CustomizerTableModel.";

    public CustomizerTableModel(String configname) {
        super(configname);
    }

    protected void initColumns() {
        this.addColumn(new NameColumn(JDL.L(JDL_PREFIX + "name", "Name"), this));
        this.addColumn(new EnabledColumn(JDL.L(JDL_PREFIX + "enabled", "Enabled"), this));
        this.addColumn(new RegexColumn(JDL.L(JDL_PREFIX + "regex", "Regex"), this));
        this.addColumn(new PackageNameColumn(JDL.L(JDL_PREFIX + "packageName", "FilePackage name"), this));
        this.addColumn(new DownloadDirColumn(JDL.L(JDL_PREFIX + "downloadDir", "Download directory"), this));
        this.addColumn(new SubDirectoryColumn(JDL.L(JDL_PREFIX + "subDirectory", "Use SubDirectory"), this));
        this.addColumn(new PostProcessingColumn(JDL.L(JDL_PREFIX + "postProcessing", "Post Processing"), this));
        this.addColumn(new PasswordColumn(JDL.L(JDL_PREFIX + "password", "Password"), this));
        this.addColumn(new DLPriorityColumn(JDL.L(JDL_PREFIX + "dlPriority", "Download Priority"), this));
        this.addColumn(new MatchCountColumn(JDL.L(JDL_PREFIX + "matchCount", "Match count from Start"), this));
    }

    @Override
    public void refreshModel() {
        synchronized (list) {
            list.clear();
            list.addAll(CustomizeSetting.getSettings());
        }
    }

}
