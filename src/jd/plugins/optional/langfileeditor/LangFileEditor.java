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
import jd.config.MenuAction;
import jd.gui.UserIO;
import jd.gui.swing.SwingGui;
import jd.gui.swing.jdgui.SingletonPanel;
import jd.gui.swing.jdgui.actions.ToolBarAction;
import jd.nutils.nativeintegration.LocalBrowser;
import jd.nutils.svn.Subversion;
import jd.plugins.OptionalPlugin;
import jd.plugins.PluginOptional;
import jd.utils.JDTheme;
import jd.utils.locale.JDL;

@OptionalPlugin(rev = "$Revision$", id = "langfileditor", interfaceversion = 5)
/**
 * Editor for jDownloader language files. Gets JDLocale.L() and JDLocale.LF()
 * entries from source and compares them to the keypairs in the language file.
 * 
 * @author eXecuTe
 * @author Greeny
 * @author coalado
 */
public class LangFileEditor extends PluginOptional {

    private final SingletonPanel lfe;
    protected MenuAction activateAction;
    private LFEView lfeView;
    private String user;
    private String pass;

    public LangFileEditor(PluginWrapper wrapper) {
        super(wrapper);
        lfe = new SingletonPanel(LFEGui.class, this.getPluginConfig(), this);
        initConfigEntries();
    }

    private void initConfigEntries() {
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_BUTTON, new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                try {
                    LocalBrowser.openURL(null, new URL("http://jdownloader.org/knowledge/wiki/development/translation/translate-jdownloader"));

                } catch (Exception e1) {
                    // TODO Auto-generated catch block
                    e1.printStackTrace();

                    UserIO.getInstance().requestMessageDialog(JDL.L("jd.plugins.optional.langfileeditor.LangFileEditor.btn.readmore", "more..."), "http://jdownloader.org/knowledge/wiki/development/translation/translate-jdownloader");
                }

            }
        }, JDL.L("jd.plugins.optional.langfileeditor.LangFileEditor.btn.readmore", "more..."), JDL.L("jd.plugins.optional.langfileeditor.LangFileEditor.initConfigEntries.message", "To use this addon, you need a JD-SVN Account"), null));

        user = getPluginConfig().getStringProperty(LFEGui.PROPERTY_SVN_ACCESS_USER);
        pass = getPluginConfig().getStringProperty(LFEGui.PROPERTY_SVN_ACCESS_PASS);
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, getPluginConfig(), LFEGui.PROPERTY_SVN_ACCESS_USER, "Upload (SVN) Username") {
            private static final long serialVersionUID = 1L;

            public void valueChanged(Object newValue) {
                super.valueChanged(newValue);
                user = newValue.toString();
            }
        });

        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_PASSWORDFIELD, getPluginConfig(), LFEGui.PROPERTY_SVN_ACCESS_PASS, "Upload (SVN) Password") {
            private static final long serialVersionUID = 1L;

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
        if (e.getSource() instanceof MenuAction && ((MenuAction) e.getSource()).getActionID() == 0) {
            if (lfeView == null) lfeView = new LFEView(lfe.getPanel(), this);

            if (((MenuAction) e.getSource()).isSelected()) {

                SwingGui.getInstance().setContent(lfeView);
            } else {
                lfeView.close();
            }

        }
    }

    @Override
    public boolean initAddon() {
        activateAction = new MenuAction(ToolBarAction.Types.TOGGLE, getHost(), 0).setActionListener(this);
        activateAction.setIcon(this.getIconKey());       
activateAction.setSelected(false);
        return true;
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
    public String getCoder() {
        return "Greeny";
    }

    @Override
    public String getIconKey() {
        return "gui.splash.languages";
    }

}