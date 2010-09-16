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

package jd.plugins.optional.langfileeditor;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URL;
import java.util.ArrayList;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.ConfigGroup;
import jd.event.ControlEvent;
import jd.gui.UserIO;
import jd.gui.swing.jdgui.JDGui;
import jd.gui.swing.jdgui.SingletonPanel;
import jd.gui.swing.jdgui.interfaces.SwitchPanelEvent;
import jd.gui.swing.jdgui.interfaces.SwitchPanelListener;
import jd.gui.swing.jdgui.menu.MenuAction;
import jd.nutils.nativeintegration.LocalBrowser;
import jd.nutils.svn.Subversion;
import jd.plugins.OptionalPlugin;
import jd.plugins.PluginOptional;
import jd.utils.JDTheme;
import jd.utils.locale.JDL;

/**
 * Editor for jDownloader language files. Gets JDL.L() and JDL.LF() entries from
 * source and compares them to the keypairs in the language file.
 * 
 * @author Greeny
 * @author coalado
 */
@OptionalPlugin(rev = "$Revision$", id = "langfileditor", hasGui = true, interfaceversion = 7)
public class LangFileEditor extends PluginOptional {

    private final SingletonPanel lfe;
    private MenuAction           activateAction;
    private LFEView              lfeView;
    private String               user;
    private String               pass;

    public LangFileEditor(PluginWrapper wrapper) {
        super(wrapper);
        lfe = new SingletonPanel(LFEGui.class, this);
        initConfigEntries();
    }

    private void initConfigEntries() {
        config.setGroup(new ConfigGroup(getHost(), getIconKey()));
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_BUTTON, new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                try {
                    LocalBrowser.openURL(null, new URL("http://jdownloader.org/knowledge/wiki/development/translation/translate-jdownloader"));
                } catch (Exception e1) {
                    e1.printStackTrace();
                    UserIO.getInstance().requestMessageDialog(JDL.L("jd.plugins.optional.langfileeditor.LangFileEditor.btn.readmore", "more..."), "http://jdownloader.org/knowledge/wiki/development/translation/translate-jdownloader");
                }
            }

        }, JDL.L("jd.plugins.optional.langfileeditor.LangFileEditor.btn.readmore", "more..."), JDL.L("jd.plugins.optional.langfileeditor.LangFileEditor.initConfigEntries.message", "To use this addon, you need a JD-SVN Account"), null));

        user = getPluginConfig().getStringProperty(LFEGui.PROPERTY_SVN_ACCESS_USER);
        pass = getPluginConfig().getStringProperty(LFEGui.PROPERTY_SVN_ACCESS_PASS);
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, getPluginConfig(), LFEGui.PROPERTY_SVN_ACCESS_USER, JDL.L("jd.plugins.optional.langfileeditor.LangFileEditor.initConfigEntries.username", "Upload (SVN) Username")) {
            private static final long serialVersionUID = 1L;

            @Override
            public void valueChanged(Object newValue) {
                super.valueChanged(newValue);
                user = newValue.toString();
            }
        });

        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_PASSWORDFIELD, getPluginConfig(), LFEGui.PROPERTY_SVN_ACCESS_PASS, JDL.L("jd.plugins.optional.langfileeditor.LangFileEditor.initConfigEntries.password", "Upload (SVN) Password")) {
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
                    UserIO.getInstance().requestMessageDialog(JDL.L("jd.plugins.optional.langfileeditor.LangFileEditor.initConfigEntries.checklogins.succeeded", "Successfull!"));
                    getPluginConfig().setProperty(LFEGui.PROPERTY_SVN_ACCESS_USER, user);

                    getPluginConfig().setProperty(LFEGui.PROPERTY_SVN_ACCESS_PASS, pass);
                    getPluginConfig().save();
                } else {
                    UserIO.getInstance().requestMessageDialog(JDL.L("jd.plugins.optional.langfileeditor.LangFileEditor.initConfigEntries.checklogins.failed", "Username or password wrong!"));
                }

            }
        }, JDL.L("jd.plugins.optional.langfileeditor.LangFileEditor.testlogins", "Test & Save Logins"), JDL.L("jd.plugins.optional.langfileeditor.LangFileEditor.testloginsmessage", "Test if the logins are correct"), JDTheme.II("gui.images.premium", 16, 16)));

    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == activateAction) {
            setGuiEnable(activateAction.isSelected());
        }
    }

    @Override
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
            JDGui.getInstance().setContent(lfeView);
        } else {
            if (lfeView != null) {
                lfeView.getLFEGui().saveChanges(false);
                lfeView.close();
            }
        }
        if (activateAction != null && activateAction.isSelected() != b) activateAction.setSelected(b);
    }

    @Override
    public boolean initAddon() {
        activateAction = new MenuAction("langfileditor", 0);
        activateAction.setActionListener(this);
        activateAction.setIcon(this.getIconKey());
        activateAction.setSelected(false);

        return true;
    }

    @Override
    public void onControlEvent(ControlEvent event) {
        if (event.getEventID() == ControlEvent.CONTROL_SYSTEM_EXIT) {
            if (lfeView != null && JDGui.getInstance().getMainTabbedPane().contains(lfeView)) {
                lfeView.getLFEGui().saveChanges(false);
            }
        }
    }

    @Override
    public void onExit() {
    }

    @Override
    public ArrayList<MenuAction> createMenuitems() {
        ArrayList<MenuAction> menu = new ArrayList<MenuAction>();

        menu.add(activateAction);

        return menu;
    }

    @Override
    public String getIconKey() {
        return "gui.splash.languages";
    }

}