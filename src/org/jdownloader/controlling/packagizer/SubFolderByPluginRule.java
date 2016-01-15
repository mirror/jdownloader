package org.jdownloader.controlling.packagizer;

import org.jdownloader.controlling.filter.BooleanFilter;
import org.jdownloader.translate._JDT;

import jd.plugins.DownloadLink;

public class SubFolderByPluginRule extends PackagizerRule {

    public static final String ID = "SubFolderByPluginRule";

    public SubFolderByPluginRule() {
        super();

    }

    public void init() {
        setMatchAlwaysFilter(new BooleanFilter(true));
        setDownloadDestination("<jd:" + DownloadLink.RELATIVE_DOWNLOAD_FOLDER_PATH + ">");
        setIconKey("folder");
        setName(_JDT._.PackagizerSettings_folderbyplugin_rule_name2());
        setEnabled(true);
        setId(ID);
        setStaticRule(true);
    }

}
