package jd.gui.skins.simple.startmenu.actions;

import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.event.ActionEvent;

import jd.controlling.DistributeData;
import jd.gui.UserIO;
import jd.parser.html.HTMLParser;
import jd.utils.JDLocale;
import jd.utils.JDTheme;

public class AddUrlAction extends StartAction {

    private static final long serialVersionUID = -7185006215784212976L;

    // private boolean abort;

    @Override
    public void init() {
        this.setIconDim(new Dimension(24, 24));
        this.setIcon("gui.images.url");
        this.setShortDescription("gui.menu.action.addurl.desc");
        this.setName("gui.menu.action.addurl.name");
        this.setMnemonic("gui.menu.addurl.action.mnem", "gui.menu.action.addurl.name");
        this.setAccelerator("gui.menu.action.addurl.accel");
    }

    public void actionPerformed(ActionEvent e) {
        String def = "";
        try {
            String newText = (String) Toolkit.getDefaultToolkit().getSystemClipboard().getData(DataFlavor.stringFlavor);
            String[] links = HTMLParser.getHttpLinks(newText, null);
            for (String l : links)
                def += l + "\r\n";
        } catch (Exception e2) {

        }

        String link = UserIO.getInstance().requestInputDialog(UserIO.NO_COUNTDOWN | UserIO.STYLE_LARGE, JDLocale.L("gui.dialog.addurl.title", "Add URL(s)"), JDLocale.L("gui.dialog.addurl.message", "Add a URL(s). JDownloader will load and parse them for further links."), def, JDTheme.II("gui.images.taskpanes.linkgrabber", 32, 32), JDLocale.L("gui.dialog.addurl.okoption_parse", "Parse URL(s)"), null);
        if (link == null) return;
        DistributeData.loadAndParse(link);
    }

    protected void interrupt() {
        // abort = true;
        Thread.currentThread().interrupt();

    }

}
