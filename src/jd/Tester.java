package jd;

public class Tester {

    public static void main(String[] args) throws Throwable {
        convert("ff0c03", null);
        convert("adadad", 100);
        convert("ff9936", 120);
    }

    private static void convert(String hex, Integer alpha) {
        StringBuilder sb = new StringBuilder();
        sb.append("new Color(");
        sb.append(Integer.parseInt(hex.substring(0, 2), 16));
        sb.append(", ");
        sb.append(Integer.parseInt(hex.substring(2, 4), 16));
        sb.append(", ");
        sb.append(Integer.parseInt(hex.substring(4, 6), 16));
        if (alpha != null) {
            sb.append(", ");
            sb.append(alpha);
        }
        sb.append(")");
        System.out.println(sb.toString());
    }
}