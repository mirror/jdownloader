package jd;

public class Tester {

    public static void main(String s[]) throws Exception {
        new Tester();
    }

    private Tester() {
        String host = "share-online.biz";

        String dummy = cleanString(host);
        if (dummy.length() < 2) dummy = cleanString(getClass().getSimpleName());
        if (dummy.length() < 2) dummy = host.toUpperCase();
        if (dummy.length() > 2) dummy = dummy.substring(0, 2);

        System.out.println(dummy);
    }

    private final String cleanString(String host) {
        return host.replaceAll("[a-z0-9\\-\\.]", "");
    }
}
