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

import java.awt.event.ActionEvent;
import java.util.ArrayList;

import javax.swing.ImageIcon;

import jd.PluginWrapper;
import jd.controlling.LinkGrabberController;
import jd.controlling.LinkGrabberPackagingEvent;
import jd.gui.swing.jdgui.JDGui;
import jd.gui.swing.jdgui.interfaces.SwitchPanelEvent;
import jd.gui.swing.jdgui.interfaces.SwitchPanelListener;
import jd.gui.swing.jdgui.menu.MenuAction;
import jd.plugins.DownloadLink;
import jd.plugins.LinkGrabberFilePackage;
import jd.plugins.OptionalPlugin;
import jd.plugins.PluginOptional;
import jd.utils.JDTheme;
import jd.utils.locale.JDL;

@OptionalPlugin(rev = "$Revision$", id = "packagecustomizer", hasGui = true, interfaceversion = 7)
public class JDPackageCustomizer extends PluginOptional implements LinkGrabberPackagingEvent {

    private static final String   JDL_PREFIX        = "jd.plugins.optional.customizer.JDPackageCustomizer.";

    public static final String    PROPERTY_SETTINGS = "SETTINGS";

    private LinkGrabberController ctrl;
    private final ImageIcon       customIcon;
    private final String          customIconText;

    private CustomizerView        view;

    private MenuAction            showAction;

    public JDPackageCustomizer(PluginWrapper wrapper) {
        super(wrapper);

        customIcon = JDTheme.II("gui.images.newpackage", 16, 16);
        customIconText = JDL.L(JDL_PREFIX + "customized", "Customized with the Regex %s");
    }

    @Override
    public String getIconKey() {
        return "gui.images.newpackage";
    }

    @Override
    public boolean initAddon() {
        CustomizeSetting.setSettings(getPluginConfig().getGenericProperty(PROPERTY_SETTINGS, new ArrayList<CustomizeSetting>()));

        ctrl = LinkGrabberController.getInstance();
        ctrl.setCustomizedPackager(this);

        showAction = new MenuAction("packagecustomizer", getIconKey());
        showAction.setActionListener(this);
        showAction.setSelected(false);

        logger.info("Customizer: OK");
        return true;
    }

    @Override
    public void onExit() {
        ctrl.setCustomizedPackager(null);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == showAction) {
            setGuiEnable(showAction.isSelected());
        }
    }

    private void showGui() {
        if (view == null) {
            view = new CustomizerView();
            view.getBroadcaster().addListener(new SwitchPanelListener() {

                @Override
                public void onPanelEvent(SwitchPanelEvent event) {
                    if (event.getEventID() == SwitchPanelEvent.ON_REMOVE) showAction.setSelected(false);
                }

            });
            CustomizerGui gui = new CustomizerGui(getPluginConfig());
            view.setContent(gui);
            view.setInfoPanel(gui.getInfoPanel());
        }
        showAction.setSelected(true);
        JDGui.getInstance().setContent(view);
    }

    @Override
    public void setGuiEnable(boolean b) {
        if (b) {
            showGui();
        } else {
            if (view != null) view.close();
        }
    }

    @Override
    public ArrayList<MenuAction> createMenuitems() {
        ArrayList<MenuAction> menu = new ArrayList<MenuAction>();

        menu.add(showAction);

        return menu;
    }

    public void attachToPackagesFirstStage(DownloadLink link) {
        ctrl.attachToPackagesFirstStageInternal(link);
    }

    public void attachToPackagesSecondStage(DownloadLink link) {
        CustomizeSetting setting = CustomizeSetting.getFirstMatch(link.getName(), link.getDownloadURL());
        if (setting != null) {
            logger.info("Customizer: Using customization of filepackage for link " + link.getName());
            if (ctrl.isExtensionFiltered(link)) {
                ctrl.getFilterPackage().add(link);
                return;
            }

            String packageName = setting.getPackageName();
            LinkGrabberFilePackage fp;
            if (packageName == null || packageName.equals("")) {
                fp = ctrl.getGeneratedPackage(link);
            } else {
                fp = ctrl.getFPwithName(packageName);
                if (fp == null) fp = new LinkGrabberFilePackage(packageName, ctrl);
            }

            fp.setPostProcessing(setting.isPostProcessing());
            fp.setDownloadDirectory(setting.getDownloadDir());
            fp.setUseSubDir(setting.isUseSubDirectory());
            fp.setPassword(setting.getPassword());
            fp.add(link);
            link.setPriority(setting.getDLPriority());

            fp.setCustomIcon(customIcon, String.format(customIconText, setting.getRegex()));
            link.setCustomIcon(customIcon, String.format(customIconText, setting.getRegex()));

            setting.incMatchCount();
        } else {
            ctrl.attachToPackagesSecondStageInternal(link);
        }
    }

}
