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

package org.jdownloader.extensions.customizer;

import java.util.ArrayList;

import javax.swing.ImageIcon;

import jd.config.ConfigContainer;
import jd.controlling.LinkGrabberController;
import jd.controlling.LinkGrabberPackagingEvent;
import jd.gui.swing.jdgui.menu.MenuAction;
import jd.plugins.AddonPanel;
import jd.plugins.DownloadLink;
import jd.plugins.LinkGrabberFilePackage;
import jd.utils.locale.JDL;

import org.appwork.utils.images.IconIO;
import org.jdownloader.extensions.AbstractConfigPanel;
import org.jdownloader.extensions.AbstractExtension;
import org.jdownloader.extensions.StartException;
import org.jdownloader.extensions.StopException;

public class PackageCustomizerExtension extends AbstractExtension implements LinkGrabberPackagingEvent {

    private static final String   JDL_PREFIX        = "jd.plugins.optional.customizer.JDPackageCustomizer.";

    public static final String    PROPERTY_SETTINGS = "SETTINGS";

    private LinkGrabberController ctrl;

    private final String          customIconText;

    private CustomizerView        view;

    public AbstractConfigPanel getConfigPanel() {
        return null;
    }

    public boolean hasConfigPanel() {
        return false;
    }

    public PackageCustomizerExtension() throws StartException {
        super(JDL.L("jd.plugins.optional.customizer.jdpackagecustomizer", null));

        customIconText = JDL.L(JDL_PREFIX + "customized", "Customized with the Regex %s");
    }

    @Override
    public String getIconKey() {
        return "gui.images.newpackage";
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

            fp.setCustomIcon(getIcon(16), String.format(customIconText, setting.getRegex()));
            link.setCustomIcon(getIcon(16), String.format(customIconText, setting.getRegex()));

            setting.incMatchCount();
        } else {
            ctrl.attachToPackagesSecondStageInternal(link);
        }
    }

    @Override
    protected void stop() throws StopException {
        ctrl.setCustomizedPackager(null);
    }

    @Override
    protected void start() throws StartException {
        CustomizeSetting.setSettings(getPluginConfig().getGenericProperty(PROPERTY_SETTINGS, new ArrayList<CustomizeSetting>()));

        ctrl = LinkGrabberController.getInstance();
        ctrl.setCustomizedPackager(this);

        logger.info("Customizer: OK");
    }

    @Override
    protected void initSettings(ConfigContainer config) {
    }

    @Override
    public String getConfigID() {
        return "packagecustomizer";
    }

    @Override
    public String getAuthor() {
        return "Greeny";
    }

    @Override
    public String getDescription() {
        return JDL.L("jd.plugins.optional.customizer.jdpackagecustomizer.description", null);
    }

    @Override
    public ImageIcon getIcon(int size) {
        return IconIO.getImageIcon(PackageCustomizerExtension.class.getResource("img/icon.png"), size);
    }

    @Override
    public AddonPanel getGUI() {

        return view;
    }

    @Override
    public ArrayList<MenuAction> getMenuAction() {
        return null;

    }

    @Override
    protected void initExtension() throws StartException {

        view = new CustomizerView(this);

        CustomizerGui gui = new CustomizerGui(getPluginConfig());
        view.setContent(gui);
        view.setInfoPanel(gui.getInfoPanel());
    }

}
