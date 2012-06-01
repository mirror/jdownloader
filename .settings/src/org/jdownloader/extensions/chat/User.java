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

package org.jdownloader.extensions.chat;

import java.awt.Color;

public class User implements Comparable<User> {
    public static final int RANK_DEFAULT = -1;
    public static final int RANK_OP = 0;
    public static final int RANK_VOICE = 1;

    public String getColor() {
        String col = Integer.toHexString(new Color(name.hashCode()).getRGB());
        while (col.length() < 6) {
            col = "0" + col;
        }
        return col.substring(col.length() - 6);
    }

    public String name;
    public int rank = RANK_DEFAULT;

    public User(String name) {
        if (name.startsWith("@")) {
            rank = RANK_OP;
            name = name.substring(1);
        }
        if (name.startsWith("+")) {
            rank = RANK_VOICE;
            name = name.substring(1);
        }
        this.name = name;

    }

    public int compareTo(User o) {
        return getRangName().compareTo(o.getRangName());
    }

    public String getNickLink(String id) {
        return "<a href='intern:" + id + "|" + name + "'>" + name + "</a>";
    }

    private String getRangName() {
        switch (rank) {
        case RANK_OP:
            return "!!!" + name.toLowerCase();
        case RANK_VOICE:
            return "!!" + name.toLowerCase();
        }
        return name.toLowerCase();
    }

    public String getRank() {
        switch (rank) {
        case RANK_OP:
            return "@";
        case RANK_VOICE:
            return "+";
        }
        return "";
    }

    public String getStyle() {

        return "color:#" + getColor();
    }

    public boolean isUser(String name) {
        if (name.startsWith("@")) {

            name = name.substring(1);
        }
        if (name.startsWith("+")) {

            name = name.substring(1);
        }
        return name.equals(this.name);

    }

    // @Override
    @Override
    public String toString() {
        switch (rank) {
        case RANK_OP:
            return "@" + name;
        case RANK_VOICE:
            return "+" + name;
        }
        return name;

    }

}