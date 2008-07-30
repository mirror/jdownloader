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

package jd.gui.skins.simple.Link;

import java.awt.GridLayout;
import java.net.MalformedURLException;
import java.net.URL;

import javax.swing.JButton;
import javax.swing.JFrame;

public class test extends JButton {

    /**
     * 
     */
    private static final long serialVersionUID = -3572661017084847518L;

    public static void main(String[] a) {
        JFrame f = new JFrame();
        f.getContentPane().setLayout(new GridLayout(0, 2));
        f.getContentPane().add(new JLinkButton("http://rapidshare.com/de/faq.html"));
        try {
            f.getContentPane().add(new JLinkButton("AGB", new URL("http://rapidshare.com/de/faq.html")));
        } catch (MalformedURLException e) {

            e.printStackTrace();
        }
        f.setSize(600, 200);
        f.setVisible(true);
    }
}