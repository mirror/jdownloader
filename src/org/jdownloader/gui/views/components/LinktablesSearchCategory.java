package org.jdownloader.gui.views.components;

import javax.swing.Icon;

import org.appwork.storage.config.annotations.LabelInterface;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;

public enum LinktablesSearchCategory implements SearchCatInterface,LabelInterface {

    FILENAME(_GUI.T.searchcategory_filename(), IconKey.ICON_TEXT, _GUI.T.searchcategory_filename_help()),

    HOSTER(_GUI.T.searchcategory_hoster(), IconKey.ICON_BROWSE, _GUI.T.searchcategory_hoster_help()),

    PACKAGE(_GUI.T.searchcategory_package(), IconKey.ICON_PACKAGE_OPEN, _GUI.T.searchcategory_package_help()),

    COMMENT(_GUI.T.searchcategory_comment(), IconKey.ICON_LIST, _GUI.T.searchcategory_comment_help()),
    COMMENT_PACKAGE(_GUI.T.searchcategory_comment_package(), IconKey.ICON_LIST, _GUI.T.searchcategory_comment_help()),

    STATUS(_GUI.T.searchcategory_status(), IconKey.ICON_INFO, _GUI.T.searchcategory_status_help()),;

    private String label;
    private String iconKey;
    private String helptext;

    private LinktablesSearchCategory(String searchcategory_filename, String iconKey, String helptext) {
        label = searchcategory_filename;
        this.iconKey = iconKey;
        this.helptext = helptext;
    }

    public String getLabel() {
        return label;
    }

    public Icon getIcon() {
        return NewTheme.I().getIcon(iconKey, 18);
    }

    public String getHelpText() {
        return helptext;
    }

}
