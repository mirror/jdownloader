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

package jd.gui.skins.simple;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.Vector;

import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.table.TableColumn;

import jd.gui.skins.simple.components.JLinkButton;
import jd.http.Browser;
import jd.utils.JDLocale;
import jd.utils.JDTheme;

import org.jdesktop.swingx.JXTitledSeparator;

public class JDAboutDialog extends JDialog {

    private static final long serialVersionUID = -2008578821095704294L;

    public static void showDialog() {
        new JDAboutDialog().setVisible(true);
    }

    public JDAboutDialog() {
        int n = 10;

        /*
         * See how I added myself above. Every string starts with a single white
         * space. This improves the way the entries look in the table (without
         * fiddleing with the tablecellrenderer)
         */

        Vector<String[]> devs = new Vector<String[]>();
        devs.add(new String[] { " Botzi", " botzi@jdownloader.org", " Hoster / Decrypter / Addons, Bugfixing, Database backend, No Support" });
        devs.add(new String[] { " coalado", " support@jdownloader.org", " JDownloader core, ocr, gui, reconnect, container, website, project administration" });
        devs.add(new String[] { " djuzi", " djuzi@jdownloader.org", " Hoster/Decrypter plugins, Bug fixes, Localizing, PL Translation" });
        devs.add(new String[] { " DwD", " dwd@jdownloader.org", " hoster, ocr, decrypter, extractor, reconnect" });
        devs.add(new String[] { " eXecuTe", " jd.execute@gmail.com", " command line support, language editor, newsfeed addon, tango theme, some plugins" });
        devs.add(new String[] { " gocsp", " gocsp@jdownloader.org", " Mac Developer" });
        devs.add(new String[] { " gluewurm", " ---", " Developing innovative ideas, Bugfixing, Technical-Feasibility-Advisor" });
        devs.add(new String[] { " Greeny", " greeny@jdownloader.org", " LangFileEditor, Support, Decrypter-Plugins, Bugfixing and making the GUI more user-friendly" });
        devs.add(new String[] { " jago", " jago@jdownloader.org", " Senior software architect in real life. Responsible for the Swing GUI design of JD." });
        devs.add(new String[] { " jiaz", " jiaz@jdownloader.org", " Webinterface,Hoster/Decrypter-Plugins,Support,Bugfixing,JDownloader core" });
        devs.add(new String[] { " scr4ve", " scr4ve@jdownloader.org", " Security Stuff, Addons, Decrypter-Plugins, Support and Bugfixing" });
        devs.add(new String[] { " Sheadox", " sheadox@jdownloader.org", " Hoster plugins, Decrypt plugins, Support" });
        devs.add(new String[] { " Viperb0y", " support@jdownloader.org", " Hoster / Decrypter, Support and Bugfixing" });

        Collections.sort(devs, new Comparator<String[]>() {

            public int compare(String[] o1, String[] o2) {
                return o1[0].compareToIgnoreCase(o2[0]);
            }

        });

        JTable table = new JTable(devs.toArray(new String[devs.size()][]), new String[] { JDLocale.L("gui.dialog.about.member", "Member"), JDLocale.L("gui.dialog.about.email", "Email"), JDLocale.L("gui.dialog.about.section", "Section") });
        table.setEnabled(false);
        setWidth(table.getColumnModel().getColumn(0), 80);
        setWidth(table.getColumnModel().getColumn(1), 160);

        JPanel links = new JPanel(new FlowLayout(FlowLayout.CENTER, n, 0));
        links.add(new JLinkButton(JDLocale.L("gui.dialog.about.homepage", "Homepage"), JDLocale.L("gui.dialog.about.homeurl", "http://www.jdownloader.org/home?lng=en")));
        links.add(new JLinkButton(JDLocale.L("gui.dialog.about.forum", "Support board"), JDLocale.L("gui.dialog.about.forumurl", "http://board.jdownloader.org")));
        links.add(new JLinkButton(JDLocale.L("gui.dialog.about.chat", "Chat"), JDLocale.L("gui.dialog.about.chaturl", "http://www.jdownloader.org/support?lng=en")));

        JXTitledSeparator titledSeparator = new JXTitledSeparator(JDLocale.L("gui.dialog.about.jddevteam", "JDownloader Developer Team"));
        titledSeparator.setForeground(Color.BLUE);

        JPanel s = new JPanel(new BorderLayout(n, n));
        s.add(titledSeparator, BorderLayout.NORTH);
        s.add(new JScrollPane(table), BorderLayout.CENTER);
        s.add(links, BorderLayout.SOUTH);
        s.setPreferredSize(new Dimension(800, 300));

        final JTextPane textPane = new JTextPane();
        textPane.setContentType("text/html");
        textPane.setEditable(false);
        textPane.setPreferredSize(new Dimension(800, 400));

        JPanel p = new JPanel(new BorderLayout(2 * n, 2 * n));
        p.setBorder(new EmptyBorder(n, n, n, n));
        p.add(new JScrollPane(textPane), BorderLayout.CENTER);
        p.add(s, BorderLayout.SOUTH);
        p.setPreferredSize(new Dimension(800, 600));

        setResizable(false);
        setIconImage(JDTheme.I("gui.images.jd_logo"));
        setModal(true);
        setTitle(JDLocale.L("gui.dialog.about.title", "About JDownloader"));
        setContentPane(p);
        pack();
        setLocationRelativeTo(null);

        getPage(textPane);
    }

    private void getPage(final JTextPane textPane) {
        new Thread() {

            @Override
            public void run() {
                try {
                    Browser br = new Browser();
                    final String txt = br.getPage(JDLocale.L("gui.dialog.about.sourceurl", "http://service.jdownloader.org/html/about_en.html"));
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            textPane.setText(txt);
                        }
                    });
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }.start();
    }

    private void setWidth(TableColumn column, int width) {
        column.setMinWidth(width);
        column.setPreferredWidth(width);
        column.setMaxWidth(width);
    }

}
