package jd.controlling.container;

import java.util.List;

import org.appwork.storage.config.ConfigInterface;
import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.CustomStorageName;
import org.appwork.storage.config.annotations.DefaultBooleanValue;
import org.appwork.storage.config.annotations.DefaultEnumValue;
import org.appwork.storage.config.annotations.DefaultJsonObject;
import org.appwork.storage.config.annotations.DefaultOnNull;
import org.appwork.storage.config.annotations.DescriptionForConfigEntry;
import org.appwork.storage.config.annotations.LabelInterface;
import org.jdownloader.translate._JDT;

@CustomStorageName("containerconfig")
public interface ContainerConfig extends ConfigInterface {
    @AboutConfig
    @DefaultBooleanValue(true)
    @DefaultOnNull
    @DescriptionForConfigEntry("If Enabled, JDownloader will save the linkgrabber list when you exit jd, and restore it on next startup")
    boolean isSaveLinkgrabberListEnabled();

    void setSaveLinkgrabberListEnabled(boolean b);

    public static enum ContainerDeleteOption implements LabelInterface {
        ASK_FOR_DELETE {
            @Override
            public String getLabel() {
                return _JDT.T.DeleteOption_ask();
            }
        },
        DONT_DELETE {
            @Override
            public String getLabel() {
                return _JDT.T.DeleteOption_no_delete();
            }
        },
        RECYCLE {
            @Override
            public String getLabel() {
                return _JDT.T.DeleteOption_recycle();
            }
        },
        DELETE {
            @Override
            public String getLabel() {
                return _JDT.T.DeleteOption_final_delete();
            }
        };
    }

    @AboutConfig
    @DefaultEnumValue("DONT_DELETE")
    @DefaultOnNull
    @DescriptionForConfigEntry("What Action should be performed after adding a container (DLC RSDF,METALINK,CCF,...)")
    ContainerDeleteOption getDeleteContainerFilesAfterAddingThemAction();

    void setDeleteContainerFilesAfterAddingThemAction(ContainerDeleteOption action);

    @DefaultJsonObject("[]")
    @DefaultOnNull
    @AboutConfig
    @DescriptionForConfigEntry("A list of passwords for automatic handling of .SFDL FTP containers.")
    List<String> getSFDLContainerPasswordList();

    void setSFDLContainerPasswordList(List<String> list);
}
