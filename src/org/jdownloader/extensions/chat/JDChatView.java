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

package org.jdownloader.extensions.chat;

import javax.swing.Icon;

import org.jdownloader.extensions.StopException;

import jd.plugins.AddonPanel;
import jd.utils.JDTheme;
import jd.utils.locale.JDL;


public class JDChatView extends AddonPanel {

    private static final long   serialVersionUID = -7876057076125402969L;
    private static final String JDL_PREFIX       = "jd.plugins.optional.jdchat.JDChatView.";
    private ChatExtension       chat;

    public JDChatView(ChatExtension chatExtension) {
        super(chatExtension);
        this.chat = chatExtension;
        init();
    }

    @Override
    public Icon getIcon() {
        return JDTheme.II("gui.images.chat", 16, 16);
    }

    @Override
    public String getTitle() {
        return JDL.L(JDL_PREFIX + "title", "JD Support Chat");
    }

    @Override
    public String getTooltip() {
        return JDL.L(JDL_PREFIX + "tooltip", "JD Support Chat");

    }

    @Override
    protected void onHide() {

    }

    @Override
    protected void onShow() {

    }

    @Override
    public String getID() {
        return "jdchatview";
    }

    @Override
    protected void onDeactivated() {

        try {
            chat.stop();
        } catch (StopException e) {
            e.printStackTrace();
        }

    }

    @Override
    protected void onActivated() {

        new Thread() {
            @Override
            public void run() {
                chat.initIRC();
            }
        }.start();

    }

}
