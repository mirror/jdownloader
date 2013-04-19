package org.jdownloader.gui.views.downloads.contextmenumanager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import javax.swing.JPopupMenu;

import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

import org.appwork.storage.config.JsonConfig;
import org.appwork.utils.Application;
import org.jdownloader.gui.views.SelectionInfo;
import org.jdownloader.gui.views.components.packagetable.context.CheckStatusAction;
import org.jdownloader.gui.views.components.packagetable.context.EnabledAction;
import org.jdownloader.gui.views.components.packagetable.context.PriorityDefaultAction;
import org.jdownloader.gui.views.components.packagetable.context.PriorityHighAction;
import org.jdownloader.gui.views.components.packagetable.context.PriorityHigherAction;
import org.jdownloader.gui.views.components.packagetable.context.PriorityHighestAction;
import org.jdownloader.gui.views.components.packagetable.context.PriorityLowerAction;
import org.jdownloader.gui.views.components.packagetable.context.SetCommentAction;
import org.jdownloader.gui.views.components.packagetable.context.SetDownloadPassword;
import org.jdownloader.gui.views.components.packagetable.context.URLEditorAction;
import org.jdownloader.gui.views.downloads.context.CreateDLCAction;
import org.jdownloader.gui.views.downloads.context.DeleteDisabledSelectedLinks;
import org.jdownloader.gui.views.downloads.context.DeleteQuickAction;
import org.jdownloader.gui.views.downloads.context.DeleteSelectedAndFailedLinksAction;
import org.jdownloader.gui.views.downloads.context.DeleteSelectedFinishedLinksAction;
import org.jdownloader.gui.views.downloads.context.DeleteSelectedOfflineLinksAction;
import org.jdownloader.gui.views.downloads.context.ForceDownloadAction;
import org.jdownloader.gui.views.downloads.context.NewPackageAction;
import org.jdownloader.gui.views.downloads.context.OpenDirectoryAction;
import org.jdownloader.gui.views.downloads.context.OpenFileAction;
import org.jdownloader.gui.views.downloads.context.OpenInBrowserAction;
import org.jdownloader.gui.views.downloads.context.PackageNameAction;
import org.jdownloader.gui.views.downloads.context.ResetAction;
import org.jdownloader.gui.views.downloads.context.ResumeAction;
import org.jdownloader.gui.views.downloads.context.SetDownloadFolderInDownloadTableAction;
import org.jdownloader.gui.views.downloads.context.StopsignAction;
import org.jdownloader.gui.views.linkgrabber.contextmenu.SortAction;

public class DownloadListContextMenuManager {

    private ContextMenuConfigInterface config;

    public DownloadListContextMenuManager() {

        config = JsonConfig.create(Application.getResource("cfg/menus/DownloadListContextMenu"), ContextMenuConfigInterface.class);

        init();

    }

    private static final int VERSION = 0;

    private MenuContainerRoot setupDefaultStructure() {
        MenuContainerRoot mr = new MenuContainerRoot();
        mr.setSource(VERSION);
        // mr.add()
        SettingsMenuContainer settings;
        mr.add(settings = new SettingsMenuContainer());

        settings.add(new MenuItemData(get(CheckStatusAction.class)));
        settings.add(new MenuItemData(get(OpenInBrowserAction.class), MenuItemProperty.HIDE_IF_DISABLED));
        settings.add(new MenuItemData(get(URLEditorAction.class)));
        settings.add(new SeparatorData());
        settings.add(new MenuItemData(get(PackageNameAction.class)));
        settings.add(new MenuItemData(get(SetDownloadFolderInDownloadTableAction.class)));
        settings.add(new MenuItemData(get(SetDownloadPassword.class)));

        PriorityMenuContainer priority;
        settings.add(priority = new PriorityMenuContainer());
        priority.add(new MenuItemData(get(PriorityLowerAction.class)));
        priority.add(new MenuItemData(get(PriorityDefaultAction.class)));
        priority.add(new MenuItemData(get(PriorityHighAction.class)));
        priority.add(new MenuItemData(get(PriorityHigherAction.class)));
        priority.add(new MenuItemData(get(PriorityHighestAction.class)));

        mr.add(new SeparatorData());
        mr.add(new MenuItemData(get(OpenFileAction.class), MenuItemProperty.HIDE_IF_OUTPUT_NOT_EXISTING, MenuItemProperty.HIDE_IF_OPENFILE_IS_UNSUPPORTED));
        mr.add(new MenuItemData(get(OpenDirectoryAction.class), MenuItemProperty.HIDE_IF_OPENFILE_IS_UNSUPPORTED));
        mr.add(new MenuItemData(get(SortAction.class)));
        mr.add(new MenuItemData(get(EnabledAction.class)));
        mr.add(new SeparatorData());

        mr.add(new MenuItemData(get(ForceDownloadAction.class)));
        mr.add(new MenuItemData(get(StopsignAction.class)));
        mr.add(new SeparatorData());

        mr.add(new AddonSubMenuLink());

        // /* addons */

        MoreMenuContainer more;
        mr.add(more = new MoreMenuContainer());
        more.add(new MenuItemData(get(ResumeAction.class)));
        more.add(new MenuItemData(get(ResetAction.class)));
        more.add(new SeparatorData());
        more.add(new MenuItemData(get(NewPackageAction.class)));
        more.add(new MenuItemData(get(CreateDLCAction.class)));
        mr.add(new SeparatorData());
        mr.add(new MenuItemData(get(DeleteQuickAction.class)));
        DeleteMenuContainer delete;
        mr.add(delete = new DeleteMenuContainer());
        delete.add(new MenuItemData(get(DeleteDisabledSelectedLinks.class)));
        delete.add(new MenuItemData(get(DeleteSelectedAndFailedLinksAction.class)));
        delete.add(new MenuItemData(get(DeleteSelectedFinishedLinksAction.class)));
        delete.add(new MenuItemData(get(DeleteSelectedOfflineLinksAction.class)));
        return mr;
    }

    private ActionData get(Class<?> class1) {
        return map.get(class1);
    }

    private void init() {

        add(new ActionData(CheckStatusAction.class));
        add(new ActionData(OpenInBrowserAction.class));
        add(new ActionData(URLEditorAction.class));
        add(new ActionData(PackageNameAction.class, MenuItemProperty.PACKAGE_CONTEXT));
        add(new ActionData(SetDownloadFolderInDownloadTableAction.class));
        add(new ActionData(SetDownloadPassword.class));
        add(new ActionData(SetCommentAction.class));

        // Priority
        add(new ActionData(PriorityLowerAction.class));
        add(new ActionData(PriorityDefaultAction.class));
        add(new ActionData(PriorityHighAction.class));
        add(new ActionData(PriorityHigherAction.class));
        add(new ActionData(PriorityHighestAction.class));

        add(new ActionData(OpenFileAction.class, MenuItemProperty.LINK_CONTEXT));
        add(new ActionData(OpenDirectoryAction.class));

        add(new ActionData(SortAction.class));
        add(new ActionData(EnabledAction.class));
        add(new ActionData(ForceDownloadAction.class));
        add(new ActionData(StopsignAction.class));
        add(new ActionData(ResumeAction.class));
        add(new ActionData(ResetAction.class));
        add(new ActionData(NewPackageAction.class));
        add(new ActionData(CreateDLCAction.class));
        add(new ActionData(DeleteQuickAction.class));
        add(new ActionData(DeleteDisabledSelectedLinks.class));
        add(new ActionData(DeleteSelectedAndFailedLinksAction.class));
        add(new ActionData(DeleteSelectedFinishedLinksAction.class));
        add(new ActionData(DeleteSelectedOfflineLinksAction.class));

    }

    private HashMap<Class<?>, ActionData> map = new HashMap<Class<?>, ActionData>();

    private void add(ActionData actionData) {

        map.put(actionData._getClazz(), actionData);
    }

    public void show() {
    }

    public JPopupMenu build(SelectionInfo<FilePackage, DownloadLink> si) {
        long t = System.currentTimeMillis();
        JPopupMenu root = new JPopupMenu();
        MenuContainerRoot md = getMenuData();
        new MenuBuilder(root, si, md).run();
        // createLayer(root, md);

        return root;
    }

    public MenuContainerRoot getMenuData() {
        long t = System.currentTimeMillis();
        MenuContainerRoot ret = config.getMenuStructure();
        if (ret == null) {
            ret = setupDefaultStructure();
            config.setMenuStructure(ret);
        }
        System.out.println(System.currentTimeMillis() - t);
        return ret;
    }

    public List<ActionData> list() {
        ArrayList<ActionData> ret = new ArrayList<ActionData>();
        for (Entry<Class<?>, ActionData> set : map.entrySet()) {
            ret.add(set.getValue());
        }
        return ret;
    }

}
