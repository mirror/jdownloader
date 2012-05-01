package org.jdownloader.controlling.packagizer;

import java.util.ArrayList;

import org.appwork.storage.config.ConfigInterface;
import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DefaultBooleanValue;
import org.appwork.storage.config.annotations.DefaultFactory;
import org.appwork.storage.config.defaults.AbstractDefaultFactory;
import org.jdownloader.controlling.filter.BooleanFilter;
import org.jdownloader.translate._JDT;

public interface PackagizerSettings extends ConfigInterface {

    class DefaultList extends AbstractDefaultFactory<ArrayList<PackagizerRule>> {

        @Override
        public ArrayList<PackagizerRule> getDefaultValue() {

            PackagizerRule folderByPackage = new PackagizerRule();
            folderByPackage.setMatchAlwaysFilter(new BooleanFilter(true));
            folderByPackage.setDownloadDestination("<jd:packagename>");
            folderByPackage.setIconKey("folder");
            folderByPackage.setName(_JDT._.PackagizerSettings_folderbypackage_rule_name());
            folderByPackage.setEnabled(false);
            ArrayList<PackagizerRule> ret = new ArrayList<PackagizerRule>();
            ret.add(folderByPackage);
            return ret;
        }
    }

    @DefaultFactory(DefaultList.class)
    @AboutConfig
    ArrayList<PackagizerRule> getRuleList();

    void setRuleList(ArrayList<PackagizerRule> list);

    @AboutConfig
    @DefaultBooleanValue(true)
    boolean isPackagizerEnabled();

    void setPackagizerEnabled(boolean b);

}
