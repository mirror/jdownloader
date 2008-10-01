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

package jd.gui.skins.simple;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.net.MalformedURLException;

import javax.swing.AbstractAction;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTable;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.table.TableColumn;

import jd.gui.skins.simple.components.JLinkButton;
import jd.http.Browser;
import jd.utils.JDLocale;

import org.jdesktop.swingx.JXHyperlink;
import org.jdesktop.swingx.JXTitledSeparator;

public class JDAboutDialog {

    private final static class LinkAction extends AbstractAction {

        private static final long serialVersionUID = 1L;
        private String url;

        private LinkAction(String label, String url) {
            super(label);
            this.url = url;
        }

        public void actionPerformed(ActionEvent e) {
            try {
                JLinkButton.openURL(url);
            } catch (MalformedURLException e1) {
                e1.printStackTrace();
            }
        }
    }

    static JFrame getDialog() {
        JFrame dialog = new JFrame();
        dialog.setResizable(false);
        dialog.setAlwaysOnTop(true);
        dialog.setTitle(JDLocale.L("gui.dialog.about.title", "About JDownloader"));
        int n = 10;
        JPanel p = new JPanel(new BorderLayout(30, 30));
        p.setBorder(new EmptyBorder(n, n, n, n));
        dialog.setContentPane(p);
        JXTitledSeparator titledSeparator = new JXTitledSeparator("JDownloader Developer Team");
        titledSeparator.setForeground(Color.BLUE);
        JXTitledSeparator titledSeparator2 = new JXTitledSeparator("What is JDownloader?");
        titledSeparator2.setForeground(Color.BLUE);

        // See how I added myself above. Every string starts with a single white
        // space. This improves the way the entries look in the table
        // (without fiddleing with the tablecellrenderer)
        String[][] devs = new String[][] { 
                { " coalado", " support@jdownloader.org", " JDownloader core, ocr, container, website, project administration" },
                { " jago", " jago@jdownloader.org", " Senior software architect in real life. Responsible for the Swing GUI design of JD." },
                { " jiaz", " jiaz@jdownloader.org", " Webinterface,Hoster/Decrypter-Plugins and Support,Bugfixing" },
                { " Greeny", " greeny@jdownloader.org", " Support, Decrypter-Plugins, Bugfixing and making the GUI more user-friendly" },
                { " Viperb0y", " support@jdownloader.org", " Hoster / Decrypter, Support and Bugfixing" },
                { " ToKaM", "tokam@frogged.de", "Hoster / Decrypter" }
                
            // {" uncomment and add your nick"," xxx@yyy.com"," describe
            // yourself..."},
            // {" uncomment and add your nick"," xxx@yyy.com"," describe
            // yourself..."},
            // {" uncomment and add your nick"," xxx@yyy.com"," describe
            // yourself..."},
            // {" uncomment and add your nick"," xxx@yyy.com"," describe
            // yourself..."},
            // {" uncomment and add your nick"," xxx@yyy.com"," describe
            // yourself..."},
            // {" uncomment and add your nick"," xxx@yyy.com"," describe
            // yourself..."},
            // {" uncomment and add your nick"," xxx@yyy.com"," describe
            // yourself..."},
            // {" uncomment and add your nick"," xxx@yyy.com"," describe
            // yourself..."},
            // {" uncomment and add your nick"," xxx@yyy.com"," describe
            // yourself..."},
        };

        JTable table = new JTable(devs, new String[] { "Entwickler", "Email", "Ressort" });
        JDAboutDialog.setWidth(table.getColumnModel().getColumn(0), 80);
        JDAboutDialog.setWidth(table.getColumnModel().getColumn(1), 160);

        JPanel links = new JPanel();
        links.add(new JXHyperlink(new LinkAction("Homepage", "http://jdownloader.net/index_en.php")));
        links.add(new JSeparator());
        links.add(new JXHyperlink(new LinkAction("Supportboard", "http://jdownloader.net/support_en.php")));
        links.add(new JSeparator());
        links.add(new JXHyperlink(new LinkAction("Chat", "http://jdownloader.net/chat_en.php")));

        JPanel s = new JPanel(new BorderLayout(n, n));
        s.add(new JScrollPane(table), BorderLayout.CENTER);
        s.add(links, BorderLayout.SOUTH);
        p.add(s, BorderLayout.SOUTH);
        s.add(titledSeparator, BorderLayout.NORTH);
        s.setPreferredSize(new Dimension(800, 200));

        final JTextPane textPane = new JTextPane();
        textPane.setContentType("text/html");
        textPane.setEditable(false);
        textPane.setPreferredSize(new Dimension(800, 400));
        // p.add(titledSeparator2, BorderLayout.NORTH);
        p.add(new JScrollPane(textPane), BorderLayout.CENTER);
        p.setPreferredSize(new Dimension(800, 600));

        dialog.pack();
        dialog.setLocationRelativeTo(null);

        Thread t = new Thread() {

            @Override
            public void run() {
                try {
                    Browser br = new Browser();
                    final String txt = br.getPage(JDLocale.L("gui.dialog.about.sourceurl", "http://service.jdownloader.org/html/about_en.html"));
                    // JDUtilities.getGUI().showHTMLDialog(JDLocale.L(
                    // "gui.dialog.about.title","About JDownloader"), txt);
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            textPane.setText(txt);
                        }
                    });
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };
        t.start();

        return dialog;
    }

    private static void setWidth(TableColumn column, int width) {
        column.setMinWidth(width);
        column.setPreferredWidth(width);
        column.setMaxWidth(width);
    }

}
