package jd.gui.simpleSWT;

/**
 * Der HauptSWTGUI
 * @author DwD
 *
 */
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.swt.SWT;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;

import jd.utils.JDSWTUtilities;

import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;


public class MainGui extends org.eclipse.swt.widgets.Composite {

    private Composite compStatusBar;
    private static CTabFolder folder;
    public static GUIConfig guiConfig = new GUIConfig();
    public static File guiConfigFile = new File("guiconf.ser");
    private static boolean isDownloadTree = true;
    // private boolean[] queuedColClicked; TODO von unten

    private static ToolBar toolBar;
    private static ToolItem btPreferences;
    private static ToolItem btInfo;
    private static ToolItem btGoLastDown;
    private static ToolItem btGoDown;
    private static ToolItem btGoUp;
    private static ToolItem btGoLastUp;
    private static ToolItem btDelete;
    private static ToolItem btCopy;
    private static ToolItem btPaste;
    private static ToolItem btStartStop;
    private static ToolItem btOpen;

    /**
     * Es werden die Bilder fuer den GUI in den Speicher geladen
     * 
     */

    private void loadImages(Display display) {
        ClassLoader cl = getClass().getClassLoader();
        JDSWTUtilities.addImageSwt("download", cl.getResourceAsStream("img/swt/download24.png"), display);
        JDSWTUtilities.addImageSwt("log", cl.getResourceAsStream("img/swt/log24.png"), display);
        JDSWTUtilities.addImageSwt("plugins", cl.getResourceAsStream("img/swt/plugins24.png"), display);
        JDSWTUtilities.addImageSwt("statistic", cl.getResourceAsStream("img/swt/statistic24.png"), display);
        JDSWTUtilities.addImageSwt("completed", cl.getResourceAsStream("img/swt/completed24.png"), display);
        JDSWTUtilities.addImageSwt("open", cl.getResourceAsStream("img/swt/open.png"), display);
        JDSWTUtilities.addImageSwt("copy", cl.getResourceAsStream("img/swt/copy.png"), display);
        JDSWTUtilities.addImageSwt("delete", cl.getResourceAsStream("img/swt/delete.png"), display);
        JDSWTUtilities.addImageSwt("go-down", cl.getResourceAsStream("img/swt/go-down.png"), display);
        JDSWTUtilities.addImageSwt("go-lastdown", cl.getResourceAsStream("img/swt/go-lastdown.png"), display);
        JDSWTUtilities.addImageSwt("go-up", cl.getResourceAsStream("img/swt/go-up.png"), display);
        JDSWTUtilities.addImageSwt("go-lastup", cl.getResourceAsStream("img/swt/go-lastup.png"), display);
        JDSWTUtilities.addImageSwt("info", cl.getResourceAsStream("img/swt/info.png"), display);
        JDSWTUtilities.addImageSwt("paste", cl.getResourceAsStream("img/swt/paste.png"), display);
        JDSWTUtilities.addImageSwt("stop", cl.getResourceAsStream("img/swt/stop.png"), display);
        JDSWTUtilities.addImageSwt("preferences", cl.getResourceAsStream("img/swt/preferences.png"), display);
        JDSWTUtilities.addImageSwt("start", cl.getResourceAsStream("img/swt/start.png"), display);
        JDSWTUtilities.addImageSwt("container", cl.getResourceAsStream("img/swt/mime/container.png"), display);
        JDSWTUtilities.addImageSwt("default", cl.getResourceAsStream("img/swt/mime/default.png"), display);
        JDSWTUtilities.addImageSwt("folder", cl.getResourceAsStream("img/swt/mime/folder.png"), display);
    }


    public static void main(String[] args) {
        showGUI();
    }


    public static void showGUI() {
        if ((guiConfigFile).isFile()) {
            ObjectInputStream objIn;
            try {
                objIn = new ObjectInputStream(new BufferedInputStream(new FileInputStream("guiconf.ser")));
                guiConfig = (GUIConfig) objIn.readObject();
                objIn.close();
            } catch (FileNotFoundException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

        }
        Display display = Display.getDefault();
        final Shell shell = new Shell(display);
        MainGui inst = new MainGui(shell, SWT.NULL);
        Point size = inst.getSize();
        shell.setText(JDSWTUtilities.getSWTResourceString("MainGui.name"));
        shell.setLayout(new FillLayout());
        Rectangle shellBounds = shell.computeTrim(0, 0, size.x, size.y);
        shell.setSize(shellBounds.width, shellBounds.height);
        shell.addListener(SWT.Close, GuiListeners.initMainGuiCloseListener(shell));
        Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
        shell.setLocation((d.width - guiConfig.GUIsize[0]) / 2, (d.height - guiConfig.GUIsize[1]) / 2);
        shell.open();
     

        if(guiConfig.isMaximized)
        {
        shell.setLocation(shell.getLocation());
        shell.setMaximized(true);
        if(guiConfig.DownloadColumnWidhtMaximized!=null)
        for (int i = 0; i < guiConfig.DownloadColumnWidhtMaximized.length; i++) {
            DownloadTab.trDownload.getColumn(i).setWidth(guiConfig.DownloadColumnWidhtMaximized[i]);
        }
        }
        while (!shell.isDisposed()) {
            if (!display.readAndDispatch())
                display.sleep();
        }
    }

    public MainGui(org.eclipse.swt.widgets.Composite parent, int style) {
        super(parent, style);
        initGUI();
    }

    private void initGUI() {
        final Shell shell = this.getShell();
        final Display display = this.getDisplay();

        GuiListeners.setGuiConfig(guiConfig);
        GuiListeners.setMainGuiShell(this.getShell());
        this.setSize(guiConfig.GUIsize[0], guiConfig.GUIsize[1]);
        
        loadImages(display);
        this.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        GridLayout thisLayout = new GridLayout();
        thisLayout.marginHeight = 0;
        thisLayout.horizontalSpacing = 0;
        thisLayout.marginWidth = 3;
        thisLayout.verticalSpacing = 0;
        this.setLayout(thisLayout);
        {
            toolBar = new ToolBar(this, (SWT.getPlatform().equals("gtk") ? SWT.NONE : SWT.FLAT));
            toolBar.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
            {
                btOpen = JDSWTUtilities.toolBarItem(display, toolBar, "open", SWT.DROP_DOWN);
                btOpen.setToolTipText(JDSWTUtilities.getSWTResourceString("MainGui.btOpen.desc"));
                btOpen.addListener(SWT.Selection, GuiListeners.initBtOpenListener());
                MenuItem btOpenOpen = GuiListeners.addListenerMenu("btOpen", JDSWTUtilities.getSWTResourceString("MainGui.btOpenOpen.name"));
                btOpenOpen.addListener(SWT.Selection, GuiListeners.getListener("openFile"));
                MenuItem btOpenSave = GuiListeners.addListenerMenu("btOpen", JDSWTUtilities.getSWTResourceString("MainGui.btOpenSave.name"));
                btOpenSave.addListener(SWT.Selection, GuiListeners.initSaveListener());
                MenuItem btOpenSaveAs = GuiListeners.addListenerMenu("btOpen", JDSWTUtilities.getSWTResourceString("MainGui.btOpenSaveAs.name"));
                btOpenSaveAs.addListener(SWT.Selection, GuiListeners.initSaveAsListener());
                new ToolItem(toolBar, SWT.SEPARATOR);
                btStartStop = JDSWTUtilities.toolBarItem(display, toolBar, "start");
                btStartStop.addListener(SWT.Selection, GuiListeners.initBtStartStopListener(btStartStop));
                btStartStop.setToolTipText(JDSWTUtilities.getSWTResourceString("MainGui.btStart.desc"));
                new ToolItem(toolBar, SWT.SEPARATOR);
                btPaste = JDSWTUtilities.toolBarItem(display, toolBar, "paste");
                btPaste.setToolTipText(JDSWTUtilities.getSWTResourceString("MainGui.btPaste.desc"));
                btCopy = JDSWTUtilities.toolBarItem(display, toolBar, "copy");
                btCopy.setToolTipText(JDSWTUtilities.getSWTResourceString("MainGui.btCopy.desc"));
                btDelete = JDSWTUtilities.toolBarItem(display, toolBar, "delete");
                btDelete.setToolTipText(JDSWTUtilities.getSWTResourceString("MainGui.btDelete.desc"));
                new ToolItem(toolBar, SWT.SEPARATOR);
                btGoLastUp = JDSWTUtilities.toolBarItem(display, toolBar, "go-lastup");
                btGoLastUp.setToolTipText(JDSWTUtilities.getSWTResourceString("MainGui.btGoLastUp.desc"));
                btGoUp = JDSWTUtilities.toolBarItem(display, toolBar, "go-up");
                btGoUp.setToolTipText(JDSWTUtilities.getSWTResourceString("MainGui.btGoUp.desc"));
                btGoDown = JDSWTUtilities.toolBarItem(display, toolBar, "go-down");
                btGoDown.setToolTipText(JDSWTUtilities.getSWTResourceString("MainGui.btGoDown.desc"));
                btGoLastDown = JDSWTUtilities.toolBarItem(display, toolBar, "go-lastdown");
                btGoLastDown.setToolTipText(JDSWTUtilities.getSWTResourceString("MainGui.btGoLastDown.desc"));
                new ToolItem(toolBar, SWT.SEPARATOR);
                btPreferences = JDSWTUtilities.toolBarItem(display, toolBar, "preferences");
                btPreferences.addListener(SWT.Selection, GuiListeners.initBtPreferencesListener());
                btPreferences.setToolTipText(JDSWTUtilities.getSWTResourceString("MainGui.btPreferences.desc"));
                btInfo = JDSWTUtilities.toolBarItem(display, toolBar, "info", SWT.DROP_DOWN);
                btInfo.addListener(SWT.Selection, GuiListeners.initBtInfoListener());
                MenuItem btInfoHelp = GuiListeners.addListenerMenu("btInfo", JDSWTUtilities.getSWTResourceString("MainGui.btInfoHelp.name"));
                MenuItem btInfoUpdate = GuiListeners.addListenerMenu("btInfo", JDSWTUtilities.getSWTResourceString("MainGui.btInfoUpdate.name"));
                MenuItem btInfoAbout = GuiListeners.addListenerMenu("btInfo", JDSWTUtilities.getSWTResourceString("MainGui.btInfoAbout.name"));
                btInfoAbout.addListener(SWT.Selection, GuiListeners.initBtInfoAboutListener());
            }

        }
        {
            folder = new CTabFolder(this, SWT.BORDER);
            folder.addKeyListener(GuiListeners.initMainGuiKeyListener());
            folder.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
            folder.setSimple(false);
            folder.setUnselectedImageVisible(true);
            folder.setUnselectedCloseVisible(true);
            folder.setBorderVisible(true);
            folder.setSelectionForeground(display.getSystemColor(SWT.COLOR_DARK_BLUE));
            folder.setSelectionBackground(new Color[]{display.getSystemColor(SWT.COLOR_GRAY), display.getSystemColor(SWT.COLOR_WHITE), display.getSystemColor(SWT.COLOR_GRAY)}, new int[]{40, 60}, true);
            folder.setFocus();
            folder.setSelection(DownloadTab.initDownload());

            folder.addListener(SWT.Selection, GuiListeners.getListener("toolBarBtSetEnabled"));
            btDelete.addListener(SWT.Selection, GuiListeners.getListener("DownloadTab.delete"));
            btCopy.addListener(SWT.Selection, GuiListeners.getListener("DownloadTab.copy"));
            btGoUp.addListener(SWT.Selection, GuiListeners.getListener("DownloadTab.goUp"));
            btGoDown.addListener(SWT.Selection, GuiListeners.getListener("DownloadTab.goDown"));
            btGoLastUp.addListener(SWT.Selection, GuiListeners.getListener("DownloadTab.goLastUp"));
            btGoLastDown.addListener(SWT.Selection, GuiListeners.getListener("DownloadTab.goLastDown"));

            CompletedTab.initCompleted();

            LoggerTab.initLogger();

            StatisticsTab.initStatistics();
            
            PluginActivitiesTab.initPluginActivities();

        }
        {
            compStatusBar = new Composite(this, SWT.NONE);
            GridLayout compStatusBarLayout = new GridLayout();
            compStatusBar.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
            compStatusBarLayout.makeColumnsEqualWidth = true;
            compStatusBar.setLayout(compStatusBarLayout);
        }
        this.addListener(SWT.Resize, new Listener(){

            public void handleEvent(Event e) {
                if (!shell.getMaximized()) {

                    Point size = shell.getSize();
                    guiConfig.GUIsize = new int[]{size.x, size.y};

                        for (int i = 0; i < MainGui.guiConfig.DownloadColumnWidht.length; i++) {
                            DownloadTab.trDownload.getColumn(i).setWidth(MainGui.guiConfig.DownloadColumnWidht[i]);
                        }
                    

                }
                else
                {

                        if(guiConfig.DownloadColumnWidhtMaximized!=null)
                        for (int i = 0; i < MainGui.guiConfig.DownloadColumnWidhtMaximized.length; i++) {
                            DownloadTab.trDownload.getColumn(i).setWidth(MainGui.guiConfig.DownloadColumnWidhtMaximized[i]);
                        }
                    

                }
                
            }});
        this.layout();
        GuiListeners.getListener("toolBarBtSetEnabled").handleEvent(new Event());
    }

    public static void toolBarBtSetEnabled() {

        if ((folder.getSelectionIndex() == 0) && DownloadTab.trDownload.getSelectionCount() > 0) {
            TreeItem[] trDownloadSelection = DownloadTab.getSelection(DownloadTab.trDownload);
            if (!isDownloadTree) {
                isDownloadTree = true;
                btDelete.removeListener(SWT.Selection, GuiListeners.getListener("CompletedTab.delete"));
                btDelete.addListener(SWT.Selection, GuiListeners.getListener("DownloadTab.delete"));
                btCopy.removeListener(SWT.Selection, GuiListeners.getListener("CompletedTab.copy"));
                btCopy.addListener(SWT.Selection, GuiListeners.getListener("DownloadTab.copy"));
            }
            btDelete.setEnabled(true);
            btCopy.setEnabled(true);
            if(trDownloadSelection.length<1)
            {
                btGoLastUp.setEnabled(false);
                btGoUp.setEnabled(false);
                btGoDown.setEnabled(false);
                btGoLastDown.setEnabled(false);
            }
            else
            {
            if (DownloadTab.trDownload.getItem(0) == (trDownloadSelection[0])) {
                btGoLastUp.setEnabled(false);
                btGoUp.setEnabled(false);
            } else {
                btGoLastUp.setEnabled(true);
                btGoUp.setEnabled(true);
            }
            if (DownloadTab.trDownload.getItem(DownloadTab.trDownload.getItemCount() - 1) == (trDownloadSelection[trDownloadSelection.length - 1])) {
                btGoDown.setEnabled(false);
                btGoLastDown.setEnabled(false);
            } else {
                btGoDown.setEnabled(true);
                btGoLastDown.setEnabled(true);
            }
            }
        } else if ((folder.getSelectionIndex() == 1) && CompletedTab.trCompleted.getSelectionCount() > 0) {
            if (isDownloadTree) {
                isDownloadTree = false;
                btDelete.removeListener(SWT.Selection, GuiListeners.getListener("DownloadTab.delete"));
                btDelete.addListener(SWT.Selection, GuiListeners.getListener("CompletedTab.delete"));
                btCopy.removeListener(SWT.Selection, GuiListeners.getListener("DownloadTab.copy"));
                btCopy.addListener(SWT.Selection, GuiListeners.getListener("CompletedTab.copy"));
            }
            btDelete.setEnabled(true);
            btCopy.setEnabled(true);
            btGoLastUp.setEnabled(false);
            btGoUp.setEnabled(false);
            btGoDown.setEnabled(false);
            btGoLastDown.setEnabled(false);
        } else {
            btDelete.setEnabled(false);
            btCopy.setEnabled(false);
            btGoLastUp.setEnabled(false);
            btGoUp.setEnabled(false);
            btGoDown.setEnabled(false);
            btGoLastDown.setEnabled(false);
        }
    }


    public static CTabFolder getFolder() {
        return folder;
    }

}
