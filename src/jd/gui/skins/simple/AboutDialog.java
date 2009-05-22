package jd.gui.skins.simple;

//jDownloader - Downloadmanager
//Copyright (C) 2009  JD-Team support@jdownloader.org
//
//This program is free software: you can redistribute it and/or modify
//it under the terms of the GNU General Public License as published by
//the Free Software Foundation, either version 3 of the License, or
//(at your option) any later version.
//
//This program is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//GNU General Public License for more details.
//
//You should have received a copy of the GNU General Public License
//along with this program.  If not, see <http://www.gnu.org/licenses/>.

import java.awt.Color;
import java.io.IOException;
import java.util.Vector;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import jd.gui.UserIO;
import jd.gui.skins.simple.components.JLinkButton;
import jd.gui.userio.dialog.AbstractDialog;
import jd.http.Browser;
import jd.nutils.JDImage;
import jd.utils.JDLocale;
import net.miginfocom.swing.MigLayout;

public class AboutDialog extends AbstractDialog {

    private JLabel label;
    private JCheckBox checkbox;
    private JTextPane textField;
    private JList list;

    public AboutDialog() {
        super(UserIO.NO_COUNTDOWN | UserIO.NO_OK_OPTION | UserIO.NO_CANCEL_OPTION, JDLocale.L("gui.about.title", "About JDownloader"), null, null, null);

        init();
    }

    /**
* 
*/
    private static final long serialVersionUID = -7647771640756844691L;

    public void contentInit(JPanel cp) {

        cp.setLayout(new MigLayout("ins 0,wrap 1", "[fill,grow]", "[][grow,fill][]"));
        JPanel p = new JPanel(new MigLayout("ins 0,wrap 2", "", "[shrink]"));
        p.setBackground(Color.WHITE);
        cp.setBackground(Color.WHITE);
        p.add(new JLabel(JDLocale.L("gui.about.message", "JDownloader - Hall of Fame")), "alignx left, aligny bottom");
        p.add(new JLabel(JDImage.getScaledImageIcon(JDImage.getImage("logo/jd_logo_54_54"), 54, 54)), "alignx right");
        p.add(new JSeparator(), "spanx,growx,pushx");
        cp.add(p, "");

        textField = new JTextPane();
        textField.setContentType("text/html");
        textField.setBackground(Color.WHITE);
        textField.setBorder(null);

        // textField.setOpaque(false);
        textField.setText("");
        textField.setEditable(false);
        new Thread() {

            // @Override
            public void run() {
                try {
                    Browser br = new Browser();
                    final String txt = br.getPage(JDLocale.L("gui.dialog.about.sourceurl", "http://service.jdownloader.org/html/about_en.html"));
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            textField.setText(txt);
                        }
                    });
                } catch (IOException e) {
                    jd.controlling.JDLogger.getLogger().log(java.util.logging.Level.SEVERE, "Exception occured", e);
                }
            }

        }.start();
        textField.addHyperlinkListener(new HyperlinkListener() {

            public void hyperlinkUpdate(HyperlinkEvent e) {
                // (e);

                if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {

                    try {
                        JLinkButton.openURL(e.getURL());
                    } catch (Exception e1) {

                    }
                    //
                }
            }

        });
        DevEntry[] devs = getDevs();
        cp.add(new JScrollPane(list = new JList(devs)), "split 2,width 120!,growy,pushy");
        list.addListSelectionListener(new ListSelectionListener() {

            public void valueChanged(ListSelectionEvent e) {
                textField.setText(((DevEntry) list.getSelectedValue()).getHTML());

            }

        });
        cp.add(new JScrollPane(textField), "");
        cp.add(new JSeparator(), "spanx,growx,pushx");
        JLabel lbl;
        cp.add(lbl = new JLabel("JDownloader © AppWork UG (haftungsbeschränkt) 2007-2009"), "split 2,aligny center");
        lbl.setEnabled(false);
        this.getContentPane().setBackground(Color.WHITE);
        JPanel links = new JPanel();
        links.setBackground(Color.WHITE);
        links.add(new JLinkButton(JDLocale.L("gui.dialog.about.homepage", "Homepage"), JDLocale.L("gui.dialog.about.homeurl", "http://www.jdownloader.org/home?lng=en")));
        links.add(new JLinkButton(JDLocale.L("gui.dialog.about.forum", "Support board"), JDLocale.L("gui.dialog.about.forumurl", "http://board.jdownloader.org")));
        links.add(new JLinkButton(JDLocale.L("gui.dialog.about.chat", "Chat"), JDLocale.L("gui.dialog.about.chaturl", "http://www.jdownloader.org/support?lng=en")));

        cp.add(links, "alignx right,aligny bottom");

    }

    protected void afterPacked() {
        this.remove(countDownLabel);
        this.setSize(800, 450);
        this.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

    }

    private DevEntry[] getDevs() {
        Vector<DevEntry> devs = new Vector<DevEntry>();
        devs.add(new DevEntry("coalado", " support@jdownloader.org", " JDownloader core, Framework, OCR, Swing GUI, Reconnect, Container, Website, Project Administration"));

        devs.add(new DevEntry("Botzi", " botzi@jdownloader.org", " Hoster / Decrypter / Addons, Bugfixing, Database backend, No Support"));
        devs.add(new DevEntry("DwD", " dwd@jdownloader.org", " hoster, ocr, decrypter, extractor, reconnect"));
        devs.add(new DevEntry("jiaz", " jiaz@jdownloader.org", " JDownloader core, Framework, Addons/Plugins, Support, Server Administration"));
        devs.add(new DevEntry("Greeny", " greeny@jdownloader.org", " LangFileEditor, Support, Decrypter-Plugins, Bugfixing and making the GUI more user-friendly"));
        devs.add(new DevEntry("scr4ve", " scr4ve@jdownloader.org", " Security Stuff, Addons, Decrypter-Plugins, Support and Bugfixing"));

        devs.add(new DevEntry("gocsp", " gocsp@jdownloader.org", " Mac Developer"));
        devs.add(new DevEntry("gluewurm", " ---", " Developing innovative ideas, Bugfixing, Technical-Feasibility-Advisor"));
        devs.add(new DevEntry("jago", " jago@jdownloader.org", " Senior software architect in real life. Responsible for the Swing GUI design of JD."));
        devs.add(new DevEntry("djuzi", " djuzi@jdownloader.org", " Hoster/Decrypter plugins, Bug fixes, Localizing, PL Translation"));
        devs.add(new DevEntry("eXecuTe", " jd.execute@gmail.com", " command line support, language editor, newsfeed addon, tango theme, some plugins"));
        devs.add(new DevEntry(" ManiacMansion", " ManiacMansion@jdownloader.org", " OCR/AntiCaptcha, Hoster/Decrypter plugins, Bugfixing"));

        devs.add(new DevEntry(" Sheadox", " sheadox@jdownloader.org", " Hoster plugins, Decrypt plugins, Support"));
        devs.add(new DevEntry(" Viperb0y", " support@jdownloader.org", " Hoster / Decrypter, Support and Bugfixing"));
        devs.add(new DevEntry(" Andrei", " andrei030@hotmail.com", " Logo Design (v2)"));
        devs.add(new DevEntry(" Trazo", " ancoar@gmail.com", " Logo Design (v3)"));
        // Collections.sort(devs);
        return devs.toArray(new DevEntry[] {});
    }

    public Integer getReturnID() {
        // TODO Auto-generated method stub
        return (Integer) super.getReturnValue();
    }

    public static void main(String[] args) {
        new GuiRunnable() {

            @Override
            public Object runSave() {
                new AboutDialog();
                return null;
            }
        }.start();

    }

    class DevEntry implements Comparable<DevEntry> {

        private String name;
        private String mail;
        private String desc;
        private String[] descs;

        public DevEntry(String string, String string2, String string3) {
            name = string.trim();
            mail = string2.trim();
            desc = string3;
            descs = string3.split(",");
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getMail() {
            return mail;
        }

        public void setMail(String mail) {
            this.mail = mail;
        }

        public String getDesc() {
            return desc;
        }

        public void setDesc(String desc) {
            this.desc = desc;
        }

        public int compareTo(DevEntry o) {
            // TODO Auto-generated method stub
            return name.compareToIgnoreCase(o.getName());
        }

        public String toString() {
            return name;
        }

        public String getHTML() {
            String ret = "<h2>" + name + "</h2>";
            ret += "<h3>Email</h3>" + mail;
            ret += "<h3>Section</h3>";
            ret += "<ul>";
            for (String d : descs) {
                ret += "<li>" + d.trim() + "</li>";
            }
            ret += "</ul>";

            return ret;
        }

    }

}
