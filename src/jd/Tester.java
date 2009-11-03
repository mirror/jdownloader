package jd;

import jd.parser.Regex;

public class Tester {

    public static void main(String ss[]) throws Exception {
        String a = "public AddContainerAction() {\nsuper(\"action.load\", \"gui.images.load\");\n}";
        String[] matches = new Regex(a, " ?((Threaded|ToolBar|Menu)Action|super)\\s*\\(\"(.*?)\"").getColumn(2);
        for (String m : matches) {
            System.out.println(m);
        }
    }
}