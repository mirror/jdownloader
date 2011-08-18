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

package jd.gui.swing.jdgui.menu.actions;

import java.awt.event.ActionEvent;
import java.util.ArrayList;

import jd.controlling.CNL2;
import jd.controlling.ClipboardHandler;
import jd.controlling.IOEQ;
import jd.controlling.linkcrawler.LinkCrawler;
import jd.gui.UserIO;
import jd.gui.swing.jdgui.actions.ToolBarAction;
import jd.nutils.JDFlags;
import jd.parser.html.HTMLParser;

import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;
import org.jdownloader.translate._JDT;

public class AddUrlAction extends ToolBarAction {

    private static final long serialVersionUID = -7185006215784212976L;

    public AddUrlAction() {
        super(_GUI._.action_addurl(), "action.addurl", "url");
    }

    @Override
    public void onAction(ActionEvent e) {
        IOEQ.add(new Runnable() {

            public void run() {
                StringBuilder def = new StringBuilder();
                try {
                    /*
                     * first we catch the current clipboard and find all links
                     * in it
                     */
                    String newText = ClipboardHandler.getClipboard().getCurrentClipboardLinks();
                    String[] links = HTMLParser.getHttpLinks(newText, null);
                    ArrayList<String> pws = HTMLParser.findPasswords(newText);
                    for (String l : links)
                        def.append(l).append("\r\n");
                    for (String pw : pws) {
                        def.append("password: ").append(pw).append("\r\n");
                    }
                } catch (Throwable e) {
                }
                final String text = UserIO.getInstance().requestInputDialog(UserIO.NO_COUNTDOWN | UserIO.STYLE_LARGE, _GUI._.gui_dialog_addurl_title(), _GUI._.gui_dialog_addurl_message(), def.toString(), NewTheme.I().getIcon("linkgrabber", 32), _GUI._.gui_dialog_addurl_okoption_parse(), null);
                if (text == null || text.length() == 0) return;
                if (CNL2.checkText(text)) return;
                Thread addThread = new Thread(new Runnable() {

                    public void run() {
                        final LinkCrawler lf = new LinkCrawler();
                        /* lets try normal parsing */
                        lf.crawlNormal(text, null);
                        lf.waitForCrawling();
                        if (lf.getCrawledLinks().size() == 0) {
                            /* nothing found, lets try deepCrawling */
                            final String[] deeplinks = HTMLParser.getHttpLinks(text, null);
                            if (deeplinks != null && deeplinks.length > 0) {
                                StringBuilder deeptxt = new StringBuilder();
                                for (final String deeplink : deeplinks) {
                                    if (deeptxt.length() > 0) {
                                        deeptxt.append("\r\n");
                                    }
                                    deeptxt.append(deeplink);
                                }
                                final String title = _JDT._.gui_dialog_deepdecrypt_title();
                                final String message = _JDT._.gui_dialog_deepdecrypt_message(deeptxt.toString());
                                final int res = UserIO.getInstance().requestConfirmDialog(0, title, message, NewTheme.I().getIcon("search", 32), _JDT._.gui_btn_continue(), null);
                                deeptxt = null;
                                if (JDFlags.hasAllFlags(res, UserIO.RETURN_OK)) {
                                    lf.crawlDeep(deeplinks);
                                    lf.waitForCrawling();
                                    System.out.println(lf.getCrawledLinks().size() + " deepLinks found");
                                }
                            }
                        } else {
                            /* yeah, we found some links */
                            System.out.println(lf.getCrawledLinks().size() + " normalLinks found");
                        }
                    }
                });
                addThread.setDaemon(true);
                addThread.setName("AddUrlAction");
                addThread.start();
            }

        }, true);

    }

    @Override
    public void initDefaults() {
    }

    @Override
    protected String createMnemonic() {
        return _GUI._.action_addurl_mnemonic();
    }

    @Override
    protected String createAccelerator() {
        return _GUI._.action_addurl_accelerator();
    }

    @Override
    protected String createTooltip() {
        return _GUI._.action_addurl_tooltip();
    }

}