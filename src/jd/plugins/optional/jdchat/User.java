package jd.plugins.optional.jdchat;

public class User implements Comparable<Object> {
    public static final int RANK_DEFAULT = -1;
    public static final int RANK_OP = 0;
    public static final int RANK_VOICE = 1;
    public String color;
    public String name;
    public int rank = RANK_DEFAULT;

    public User(String name) {
        this(name, null);
    }

    public User(String name, String color) {
        if (name.startsWith("@")) {
            rank = RANK_OP;
            name = name.substring(1);
        }
        if (name.startsWith("+")) {
            rank = RANK_VOICE;
            name = name.substring(1);
        }
        this.name = name;
        if (color == null) {
            color = Utils.getRandomColor();
        }
        this.color = color;

    }

    public int compareTo(Object o) {

        return getRangName().compareTo(((User) o).getRangName());
    }

    public String getNickLink(String id) {
        return "<a href='intern:" + id + "|" + name + "'>" + name + "</a>";
    }

    private String getRangName() {
        switch (rank) {
        case RANK_OP:
            return "!!!" + name;
        case RANK_VOICE:
            return "!!" + name;
        }
        return name;

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

        return "color:#" + color;
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