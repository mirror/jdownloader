//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
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

package org.jdownloader.extensions.langfileeditor;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.ConfigGroup;
import jd.controlling.JDController;
import jd.event.ControlEvent;
import jd.event.ControlListener;
import jd.gui.UserIO;
import jd.gui.swing.jdgui.JDGui;
import jd.gui.swing.jdgui.SingletonPanel;
import jd.gui.swing.jdgui.interfaces.SwitchPanelEvent;
import jd.gui.swing.jdgui.interfaces.SwitchPanelListener;
import jd.gui.swing.jdgui.menu.MenuAction;
import jd.nutils.svn.Subversion;
import jd.plugins.AddonPanel;
import jd.utils.JDTheme;

import org.appwork.utils.os.CrossSystem;
import org.jdownloader.extensions.AbstractExtension;
import org.jdownloader.extensions.ExtensionConfigPanel;
import org.jdownloader.extensions.StartException;
import org.jdownloader.extensions.StopException;
import org.jdownloader.extensions.langfileeditor.translate.T;

/**
 * Editor for jDownloader language files. Gets JDL.L() and JDL.LF() entries from
 * source and compares them to the keypairs in the language file.
 * 
 * @author Greeny
 * @author coalado
 */

public class LangFileEditorExtension extends AbstractExtension<LangFileEditorConfig> implements ActionListener, ControlListener {

    private final SingletonPanel lfe;
    private MenuAction           activateAction;
    private LFEView              lfeView;
    private String               user;
    private String               pass;
    private ExtensionConfigPanel configPanel;

    public ExtensionConfigPanel<LangFileEditorExtension> getConfigPanel() {
        return configPanel;
    }

    public boolean hasConfigPanel() {
        return true;
    }

    public LangFileEditorExtension() throws StartException {
        super(T._.jd_plugins_optional_langfileeditor_langfileeditor());
        lfe = new SingletonPanel(LFEGui.class, this);

    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == activateAction) {
            setGuiEnable(activateAction.isSelected());
        }
    }

    public void setGuiEnable(boolean b) {
        if (b) {
            if (lfeView == null) {
                lfeView = new LFEView(lfe.getPanel());
                lfeView.getBroadcaster().addListener(new SwitchPanelListener() {

                    @Override
                    public void onPanelEvent(SwitchPanelEvent event) {
                        if (event.getEventID() == SwitchPanelEvent.ON_REMOVE) {
                            activateAction.setSelected(false);
                        }
                    }

                });
            }
            JDGui.getInstance().setContent(lfeView, true);
        } else {
            if (lfeView != null) {
                lfeView.getLFEGui().saveChanges(false);
                lfeView.close();
            }
        }
        if (activateAction != null && activateAction.isSelected() != b) activateAction.setSelected(b);
    }

    public void controlEvent(ControlEvent event) {
        if (event.getEventID() == ControlEvent.CONTROL_SYSTEM_EXIT) {
            if (lfeView != null && JDGui.getInstance().getMainTabbedPane().contains(lfeView)) {
                lfeView.getLFEGui().saveChanges(false);
            }
        }
    }

    @Override
    public String getIconKey() {
        return "gui.splash.languages";
    }

    @Override
    protected void stop() throws StopException {
        JDController.getInstance().removeControlListener(this);
    }

    @Override
    protected void start() throws StartException {
        activateAction = new MenuAction("langfileditor", 0);
        activateAction.setActionListener(this);
        activateAction.setIcon(this.getIconKey());
        activateAction.setSelected(false);

        JDController.getInstance().addControlListener(this);
    }

    protected void initSettings(ConfigContainer config) {

        config.setGroup(new ConfigGroup(getName(), getIconKey()));
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_BUTTON, new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                CrossSystem.openURLOrShowMessage("http://jdownloader.org/knowledge/wiki/development/translation/translate-jdownloader");
            }

        }, T._.jd_plugins_optional_langfileeditor_LangFileEditor_btn_readmore(), T._.jd_plugins_optional_langfileeditor_LangFileEditor_initConfigEntries_message(), null));

        user = getPluginConfig().getStringProperty(LFEGui.PROPERTY_SVN_ACCESS_USER);
        pass = getPluginConfig().getStringProperty(LFEGui.PROPERTY_SVN_ACCESS_PASS);
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, getPluginConfig(), LFEGui.PROPERTY_SVN_ACCESS_USER, T._.jd_plugins_optional_langfileeditor_LangFileEditor_initConfigEntries_username()) {
            private static final long serialVersionUID = 1L;

            @Override
            public void valueChanged(Object newValue) {
                super.valueChanged(newValue);
                user = newValue.toString();
            }
        });

        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_PASSWORDFIELD, getPluginConfig(), LFEGui.PROPERTY_SVN_ACCESS_PASS, T._.jd_plugins_optional_langfileeditor_LangFileEditor_initConfigEntries_password()) {
            private static final long serialVersionUID = 1L;

            @Override
            public void valueChanged(Object newValue) {
                super.valueChanged(newValue);
                pass = newValue.toString();
            }
        });

        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_BUTTON, new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                boolean ret = Subversion.checkLogin(LFEGui.LANGUAGE_SVN, user, pass);

                if (ret) {
                    UserIO.getInstance().requestMessageDialog(T._.jd_plugins_optional_langfileeditor_LangFileEditor_initConfigEntries_checklogins_succeeded());
                    getPluginConfig().setProperty(LFEGui.PROPERTY_SVN_ACCESS_USER, user);

                    getPluginConfig().setProperty(LFEGui.PROPERTY_SVN_ACCESS_PASS, pass);
                    getPluginConfig().save();
                } else {
                    UserIO.getInstance().requestMessageDialog(T._.jd_plugins_optional_langfileeditor_LangFileEditor_initConfigEntries_checklogins_failed());
                }

            }
        }, T._.jd_plugins_optional_langfileeditor_LangFileEditor_testlogins(), T._.jd_plugins_optional_langfileeditor_LangFileEditor_testloginsmessage(), JDTheme.II("gui.images.premium", 16, 16)));

    }

    @Override
    public String getConfigID() {
        return "langfileditor";
    }

    @Override
    public String getAuthor() {
        return "Greeny & Coalado";
    }

    @Override
    public String getDescription() {
        return T._.jd_plugins_optional_langfileeditor_langfileeditor_description();
    }

    @Override
    public AddonPanel getGUI() {
        return null;
    }

    @Override
    public ArrayList<MenuAction> getMenuAction() {
        ArrayList<MenuAction> menu = new ArrayList<MenuAction>();

        menu.add(activateAction);

        return menu;
    }

    @Override
    protected void initExtension() throws StartException {

        ConfigContainer cc = new ConfigContainer(getName());
        initSettings(cc);
        configPanel = createPanelFromContainer(cc);
    }

}