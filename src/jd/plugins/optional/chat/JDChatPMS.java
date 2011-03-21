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

package jd.plugins.optional.chat;

import javax.swing.JScrollPane;
import javax.swing.JTextPane;

public class JDChatPMS {
    private String Username;
    private StringBuilder sb;
    private JTextPane TextArea;
    private JScrollPane scrollPane;

    public JDChatPMS(String username) {
        super();
        Username = username;
        TextArea = new JTextPane();
        TextArea.setEditable(false);
        TextArea.setContentType("text/html");
        scrollPane = new JScrollPane(TextArea);
        sb = new StringBuilder();
    }

    public String getUsername() {
        return Username;
    }

    public void setUsername(String username) {
        Username = username;
    }

    public StringBuilder getSb() {
        return sb;
    }

    public JTextPane getTextArea() {
        return TextArea;
    }

    public JScrollPane getScrollPane() {
        return scrollPane;
    }

}
