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

package jd.plugins.optional.neembuu;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.event.ControlEvent;
import jd.gui.swing.SwingGui;
import jd.gui.swing.jdgui.interfaces.SwitchPanelEvent;
import jd.gui.swing.jdgui.interfaces.SwitchPanelListener;
import jd.gui.swing.jdgui.menu.MenuAction;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.OptionalPlugin;
import jd.plugins.PluginOptional;
import jd.utils.locale.JDL;
import jpfm.FileAttributesProvider;
import jpfm.JPfmMount;

/**
 * order :
 * # isJPfmUsable
 * # prepare --> if mount locaiton not set or problematic --> ignore --> end
 * # User selected to mount a file -> if file alreadty present -> ignore else watch() invoked
 * # volumes automatically unmounted and all kernel resources freed on exit.
 * @author Shashank Tulsyan
 */
@OptionalPlugin(rev = "$Revision: 11760 $", id = "neembuu", hasGui = true, interfaceversion = 5, minJVM=1.7, linux=false, windows=true, mac=false )
public class Neembuu extends PluginOptional {

    private static final int CONTEXT_MENU_ID_WATCH_LINK = 1000;

    private static final int CONTEXT_MENU_ID_OPEN_LINK = 1001;

    private static final int CONTEXT_MENU_ID_WATCH_PACKAGE = 1002;

    private static final int CONTEXT_MENU_ID_OPEN_PACKAGE = 1003;

    private MenuAction activateAction;

    private NeembuuTab tab;

    private VirtualFolderManager vfm = new VirtualFolderManager(this);

    public Neembuu(PluginWrapper wrapper) {
        super(wrapper);
        initConfigEntries();

    }

    @Override
    public void actionPerformed(ActionEvent e) {
        // Enable/disable GUI Tab
        if (e.getSource() == activateAction) {
            setGuiEnable(activateAction.isSelected());
        } else if (e.getSource() instanceof MenuAction) {
            actionPerformedOnMenuItem((MenuAction) e.getSource());
        }
    }

    private void actionPerformedOnMenuItem(MenuAction source) {
        DownloadLink link;
        FilePackage fp;

        switch (source.getActionID()) {
        case CONTEXT_MENU_ID_WATCH_LINK:
            link = (DownloadLink) source.getProperty("LINK");
            fp = link.getFilePackage();
            watch(link);
            break;

        case CONTEXT_MENU_ID_OPEN_LINK:
            link = (DownloadLink) source.getProperty("LINK");
            fp = link.getFilePackage();
            break;
        case CONTEXT_MENU_ID_WATCH_PACKAGE:
            fp = (FilePackage) source.getProperty("PACKAGE");
            break;
        case CONTEXT_MENU_ID_OPEN_PACKAGE:
            fp = (FilePackage) source.getProperty("PACKAGE");
            break;
        }
    }

    private void watch(DownloadLink link){

        //for now let 's use the vlc installed in out system
        //to play the file.

        if(!isJPfmUsable()){
            //no error messages shown for now, we simply log this
            logger.log(Level.WARNING,"Not ready and user wants to watch a file");
            return;
        }
        prepare();

        FileAttributesProvider file;

        // it is here that we can decide in which subfolder (inside virtual folder) 
        // we want the file for now keeping the file in virtual folder root
        if( (file=vfm.getRootDirectory().get(link.getFinalFileName()))!=null){
            if(file instanceof JDFile){

                //file already added to virtual folder
                // ignore

                // todo :
                // start jvlc and open using the file using the path :
                // jpfmMountinstance.mountLocation() + java.io.File.separatorChar + link.getFinalFileName()
                // or simply open this file in vlc that is installed in the system
                return;
            }else {
                // todo :
                // a file of some other kind
                // example : BasicRealFile,
                // is present with the same name, so add this with a different
                // name which does not exists.
                // For now ignoring

                // jpfm.volume.BasicRealFile  could be used to add
                // real files in the volume, like subtitles
                // so that it easier for user and/or vlc to find it.
            }
        }else {
            // we need to add this file in the volume
            JDFile jDFile = new JDFile(link, vfm.getRootDirectory());
            vfm.getRootDirectory().add(jDFile);

            // todo :
            // start jvlc and open using the file using the path :
            // jpfmMountinstance.mountLocation() + java.io.File.separatorChar + link.getFinalFileName()
            // or simply open this file in vlc that is installed in the system
        }
        
    }

    @Override
    public void onControlEvent(ControlEvent event) {
        // receiver all control events. reconnects,
        // downloadstarts/end,pluginstart/pluginend

        DownloadLink link;
        switch (event.getID()) {

        case ControlEvent.CONTROL_LINKLIST_CONTEXT_MENU:
            ArrayList<MenuAction> items = (ArrayList<MenuAction>) event.getParameter();
            MenuAction m;
            MenuAction container = new MenuAction("jd.plugins.optional.neembuu.Neembuu.menu.container", 0);
            container.setIcon(this.getIconKey());
            items.add(container);
            if (event.getSource() instanceof DownloadLink) {
                link = (DownloadLink) event.getSource();

                container.addMenuItem(m = new MenuAction("jd.plugins.optional.neembuu.Neembuu.menu.watch.link", CONTEXT_MENU_ID_WATCH_LINK));
                m.setProperty("LINK", link);
                m.setActionListener(this);
                container.addMenuItem(m = new MenuAction("jd.plugins.optional.neembuu.Neembuu.menu.open.link", CONTEXT_MENU_ID_OPEN_LINK));
                m.setActionListener(this);
                m.setProperty("LINK", link);

            } else {
                FilePackage fp = (FilePackage) event.getSource();

                container.addMenuItem(m = new MenuAction("jd.plugins.optional.neembuu.Neembuu.menu.watch.package", CONTEXT_MENU_ID_WATCH_PACKAGE));
                m.setProperty("PACKAGE", fp);
                m.setActionListener(this);
                container.addMenuItem(m = new MenuAction("jd.plugins.optional.neembuu.Neembuu.menu.open.package", CONTEXT_MENU_ID_OPEN_PACKAGE));
                m.setActionListener(this);
                m.setProperty("PACKAGE", fp);
            }
            break;
        }
    }

    @Override
    public ArrayList<MenuAction> createMenuitems() {
        // add main menu items.. this item is used to show/hide GUi
        ArrayList<MenuAction> menu = new ArrayList<MenuAction>();
        MenuAction m;

        menu.add(m = activateAction);
        if (tab == null || !tab.isVisible()) {
            m.setSelected(false);
        } else {
            m.setSelected(true);
        }
        return menu;
    }

    @Override
    public boolean initAddon() {
        // this method is called ones after the addon has been loaded

        activateAction = new MenuAction("Neembuu", 0);
        activateAction.setActionListener(this);
        activateAction.setIcon(this.getIconKey());
        activateAction.setSelected(false);

        return true;
    }

    private void initConfigEntries() {

        // note : mount location cannot be changed when the virtual folder is mounted
        // create a browsefile setting entry
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_BROWSEFILE, getPluginConfig(), "MOUNTLOCATION", "Mountlocation"));
        // combobox entry.

        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_COMBOBOX_INDEX, getPluginConfig(), "MODE", new String[] { "No restrictions", "limit download to required speed", "..." }, JDL.L("plugins.jdchat.userlistposition", "Download AI Mode:")).setDefaultValue(0));

        //config.getEntries().get(0).
    }

    @SuppressWarnings("unchecked")
    private void initGUI() {

        tab = new NeembuuTab();
        tab.getBroadcaster().addListener(new SwitchPanelListener() {

            @Override
            public void onPanelEvent(SwitchPanelEvent event) {
                if (event.getID() == SwitchPanelEvent.ON_REMOVE) {
                    setGuiEnable(false);
                }
            }

        });

    }

    @Override
    public void onExit() {
        // addon disabled/tabe closed
    }

    @Override
    public void setGuiEnable(boolean b) {
        if (b) {

            if (tab == null) {
                initGUI();

            }
            SwingGui.getInstance().setContent(tab);

        } else {

            if (tab != null) {
                SwingGui.getInstance().disposeView(tab);
                this.stopAddon();
                tab = null;
            }

        }
        if (activateAction != null && activateAction.isSelected() != b) activateAction.setSelected(b);
    }

    @Override
    public String getIconKey() {
        // should use an own icon later
        return "gui.images.chat";
    }


    private boolean isJPfmUsable(){
        try{
            // invoking static function in JPfmMount
            // that initialize native side structures
            JPfmMount.class.getName();
        }catch(Exception any){
            // this would
            logger.log(Level.SEVERE, "JPfmMount static functions failed", any);
            return false;
        }
        // if jpfm.dll is not in path
        // we might do a System.load here
        // System.load("<jpfm lib path here>" );

        return true;
    }

    /*package private*/ Logger getLogger(){
        return logger;
    }

    private void prepare(){
        if(!isJPfmUsable()){
            //("JPfm not usuable") ;
            throw new IllegalStateException("JPfm not usable");
        }
        if(vfm==null){
            //
        }
        
    }
}
