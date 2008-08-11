//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team jdownloader@freenet.de
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

package jd.plugins.optional;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.io.File;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Vector;
import java.util.regex.Pattern;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextArea;

import jd.JDFileFilter;
import jd.config.MenuItem;
import jd.controlling.DistributeData;
import jd.gui.skins.simple.ConvertDialog;
import jd.gui.skins.simple.LocationListener;
import jd.gui.skins.simple.SimpleGUI;
import jd.gui.skins.simple.Link.JLinkButton;
import jd.gui.skins.simple.components.JDFileChooser;
import jd.http.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.PluginOptional;
import jd.utils.JDLocale;
import jd.utils.JDTheme;
import jd.utils.JDUtilities;

public class StreamingShareTool extends PluginOptional {
    public static int getAddonInterfaceVersion() {
        return 0;
    }

    private JFrame frame;
    
    private JMenuItem menHelpHelp;
    
    private JMenuItem menOpenList;

    private JMenuItem menSaveDLC;

    private JMenuItem menSaveList;

    private JMenuItem menFileValidate;

    private JMenuItem menExit;
  
    //TODO: Add your Service here:

    private Pattern DecryptOnlyThese = Pattern.compile("^youtube.com$");
    
    private JTextArea textArea;
    
    private String extension = "sst";

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == menOpenList) {
            JDFileChooser fc = new JDFileChooser();
            fc.setApproveButtonText(JDLocale.L("plugins.optional.streamsharingtool.gui.openfile", "Open List"));
            fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
            fc.setFileFilter(new JDFileFilter(null, "." + extension, true));
            fc.showSaveDialog(frame);
            File ret = fc.getSelectedFile();
            if (ret == null || !ret.exists()) { return; }
            try {
                    String open = (String) JDUtilities.loadObject(frame, ret, true);
                    textArea.setText(open);
                    
            } catch (Exception e2) {
                textArea.setText(JDUtilities.getLocalFile(ret));
            }

            return;
        }
        if (e.getSource() == menSaveList) {
            JDFileChooser fc = new JDFileChooser();
            fc.setApproveButtonText(JDLocale.L("plugins.optional.streamsharingtool.gui.savefile", "Save List"));
            fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
            fc.setFileFilter(new JDFileFilter(null, "." + extension, true));
            fc.showSaveDialog(frame);
            File ret = fc.getSelectedFile();
            if (ret == null) { return; }
            if (JDUtilities.getFileExtension(ret) == null || !JDUtilities.getFileExtension(ret).equalsIgnoreCase(extension)) {

                ret = new File(ret.getAbsolutePath() + "." + extension);
            }

            JDUtilities.saveObject(frame, textArea.getText(), ret, null, null, true);
            return;
        }
        if (e.getSource() == menFileValidate) {
        	GetValid(textArea.getText(),true);
            return;
        }        
        if (e.getSource() == menSaveDLC) {
        	saveDLC(textArea.getText());
            return;
        }        
        
        if (e.getSource() == menExit) {
            frame.dispose();
            return;
        }
        if (e.getSource() == menHelpHelp) {
        	try {
				JLinkButton.openURL(JDLocale.L("plugins.optional.streamsharingtool.wikiurl", "http://wiki.jdownloader.org/index.php?title=StreamingShare"));
			} catch (MalformedURLException e1) {
				e1.printStackTrace();
			}
            return;
        }       
        
        
        if (frame == null || !frame.isVisible()) {
            initGUI();
        } else {
            frame.dispose();
        }
    }

    public ArrayList<MenuItem> createMenuitems() {
        ArrayList<MenuItem> menu = new ArrayList<MenuItem>();
        if (frame == null || !frame.isVisible()) {
            menu.add(new MenuItem(JDLocale.L("plugins.optional.streamsharingtool.action.start", "Start"), 0).setActionListener(this));
        } else {
            menu.add(new MenuItem(JDLocale.L("plugins.optional.streamsharingtool.action.end", "Exit"), 0).setActionListener(this));

        }
        return menu;
    }

    public String getCoder() {
        return "JD-Team";
    }

    public String getPluginName() {
        return JDLocale.L("plugins.optional.streamsharingtool.name", "StreamingShare Link-Generator");
    }

    public String getRequirements() {
        return "JRE 1.5+";
    }

    public String getVersion() {
        String ret = new Regex("$Revision: 2173 $", "\\$Revision: ([\\d]*?) \\$").getFirstMatch();
        return ret == null ? "0.0" : ret;
    }

    public boolean initAddon() {

        return true;
    }
    
    private void saveDLC(String text)
    {
        JDFileChooser fc= new JDFileChooser("_LOADSAVEDLC");
        fc.setFileFilter(new JDFileFilter(null, ".dlc", true));
        fc.showSaveDialog(SimpleGUI.CURRENTGUI.getFrame());
        File ret = fc.getSelectedFile();
        if (ret == null) { return; }
    	String comment = JDUtilities.getGUI().showTextAreaDialog(JDLocale.L("plugins.optional.streamsharingtool.gui.comment.title", "Enter DLC-Comment:"),JDLocale.L("plugins.optional.streamsharingtool.gui.comment.question", "Please enter your desired DLC-Comment:"), JDLocale.L("plugins.optional.streamsharingtool.gui.comment.defaultcomment", "StreamingShare-DLC"));
    	
    	//TODO: Gespeicherter DLC-Uploadername in Defaultcomment einbringen
    	
    	if (comment == null) { comment = JDLocale.L("plugins.optional.streamsharingtool.gui.comment.defaultcomment", "StreamingShare-DLC"); }
        Vector<DownloadLink> links = GetValid(text,false);
        
        Vector<DownloadLink> toSave = new Vector<DownloadLink>();
        
        for(int i = 0; i < links.size();i++)
        {
        	
        //TODO: Add your Service here:
        if(links.get(i).getPlugin().getHost().matches("^youtube\\.com$"))
        {
        //toSave.add(links.get(i)); geht nicht, da in DLCs nur Links und keine Objects gespeichert werden
        	String URL = 
        		"< streamingshare=\"youtube.com\" " + 
                "name=\""+ links.get(i).getName() + 
                "\" dlurl=\"" + links.get(i).getDownloadURL() + 
                "\" brurl=\"" + links.get(i).getBrowserUrl() + 
                "\" convertto=\"" + links.get(i).getProperty("convertto").toString() + 
                "\" comment=\"" + comment + 
                "\" >";
        	
        toSave.add(new DownloadLink(null, null, getHost(), Encoding.htmlDecode(URL), true));
        

        //logger.info(links.get(i).getDownloadURL());
        //logger.info(links.get(i).getName());
        //logger.info(links.get(i).getBrowserUrl());
        //logger.info(links.get(i).getSourcePluginComment());
        //logger.info(links.get(i).getProperty("convertto").toString());

        }
        }
        
        /*for(int i = 0; i < toSave.size();i++)
        {
        	logger.info(toSave.get(i).getDownloadURL());
        	//TO-DO: Debuginfo
        }*/

        if (JDUtilities.getFileExtension(ret) == null || !JDUtilities.getFileExtension(ret).equalsIgnoreCase("dlc")) {

            ret = new File(ret.getAbsolutePath() + ".dlc");
        }

        JDUtilities.getController().saveDLC(ret, toSave);
        JDUtilities.getGUI().showCountdownConfirmDialog(JDLocale.L("plugins.optional.streamsharingtool.action.save.dlc.finished", "StreamingShare DLC ready 2 use!"), 45);
    	
    }

    private void initGUI() {
        frame = new JFrame();
        frame.setTitle(JDLocale.L("plugins.optional.streamsharingtool.gui.title", "StreamShare Link Creator"));
        frame.setIconImage(JDTheme.I("gui.images.config.decrypt"));
        frame.setPreferredSize(new Dimension(600, 600));
        frame.setName("STREAMSHARINGLINKGENERATOR");
        LocationListener list = new LocationListener();
        frame.addComponentListener(list);
        frame.addWindowListener(list);
        frame.setLayout(new BorderLayout());
        initMenu();

        textArea = new JTextArea();
        JScrollPane scrollPane = new JScrollPane(textArea);

        textArea.setText(JDLocale.L("plugins.optional.streamsharingtool.textarea.inittext", "Insert your Links here"));
        frame.setResizable(true);
        textArea.setEditable(true);
        textArea.requestFocusInWindow();

        frame.add(scrollPane, BorderLayout.CENTER);

        frame.pack();
        SimpleGUI.restoreWindow(null, null, frame);
        frame.setVisible(true);
    }

    private void initMenu() {
        JMenuBar menuBar = new JMenuBar();
        frame.setJMenuBar(menuBar);

        JMenu menFile = new JMenu(JDLocale.L("plugins.optional.streamsharingtool.gui.menu.file", "File"));
        JMenu menHelp = new JMenu(JDLocale.L("plugins.optional.streamsharingtool.gui.menu.help", "Help"));
        
        
        menuBar.add(menFile);
        menuBar.add(menHelp);
        
        
        
        // Filemenu
        menOpenList = new JMenuItem(JDLocale.L("plugins.optional.streamsharingtool.gui.menu.open.list", "Open List"));
        menSaveDLC = new JMenuItem(JDLocale.L("plugins.optional.streamsharingtool.gui.menu.save.dlc", "Save DLC"));
        menSaveList = new JMenuItem(JDLocale.L("plugins.optional.streamsharingtool.gui.menu.save.list", "Save List"));
        menExit = new JMenuItem(JDLocale.L("plugins.optional.streamsharingtool.gui.menu.exit", "Exit"));
        menFileValidate = new JMenuItem(JDLocale.L("plugins.optional.streamsharingtool.gui.menu.validate", "Validate"));
        menHelpHelp = new JMenuItem(JDLocale.L("plugins.optional.streamsharingtool.gui.menu.help.help", "Help"));

        menSaveDLC.addActionListener(this);
        menSaveList.addActionListener(this);
        menExit.addActionListener(this);
        menFileValidate.addActionListener(this);
        menOpenList.addActionListener(this);
        menHelpHelp.addActionListener(this);
        
        menSaveDLC.setIcon(JDTheme.II("gui.images.jd_logo",16,16));
        menSaveList.setIcon(JDTheme.II("gui.images.save",16,16));
        menOpenList.setIcon(JDTheme.II("gui.images.load",16,16));
        menExit.setIcon(JDTheme.II("gui.images.exit",16,16));
        menHelpHelp.setIcon(JDTheme.II("gui.images.help",16,16));
        
        menFile.add(menFileValidate);
        menFile.add(menOpenList);
        menFile.add(menSaveList);
        menFile.add(menSaveDLC);
        menFile.add(new JSeparator());
        menFile.add(menExit);
        menHelp.add(menHelpHelp);
    }

    public void onExit() {

    }

    private Vector<DownloadLink> GetValid(String text, boolean showcount) {
        String script = Encoding.htmlDecode(text);
        ConvertDialog.setKeepformat(false);
        ConvertDialog.setKeepineverycase(true);
        Vector<DownloadLink> toReturn = new Vector<DownloadLink>();
        Vector<DownloadLink> links = new DistributeData(script).findLinks(false);
        
        if(links.size()==0)
        {
            if(showcount)
            {
            JDUtilities.getGUI().showCountdownConfirmDialog("0 " + JDLocale.L("plugins.optional.streamsharingtool.action.validate.linksfound", "Link(s) gefunden!"), 60);
            }
            return null;
        }
        logger.info(String.valueOf(links.size())+ " Links found");
        for(int i = 0; i < links.size();i++)
        {
        	if(DecryptOnlyThese.matcher(links.get(i).getPlugin().getHost()).matches())
            {
        	toReturn.add(links.get(i));
        	//TO-DO: Debuginfo
            //logger.info(links.get(i).getDownloadURL()+ " added to valid Links");
            /*logger.info(links.get(i).getName());
            logger.info(links.get(i).getBrowserUrl());
            logger.info(links.get(i).getSourcePluginComment());
            logger.info(links.get(i).getProperty("convertto").toString());*/
            
            }
        	else
        	{
        		//TO-DO: Debuginfo
        		//logger.info(links.get(i).getDownloadURL()+ " isn't a valid Link");
        	}

        }
        
        ConvertDialog.setKeepformat(false);
        ConvertDialog.setKeepineverycase(false);
        if(showcount)
        {
        JDUtilities.getGUI().showCountdownConfirmDialog(String.valueOf(links.size()) + " " + JDLocale.L("plugins.optional.streamsharingtool.action.validate.linksfound", "Link(s) gefunden!"), 60);
        }
        return toReturn;

    }

}


