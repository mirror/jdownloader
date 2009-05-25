import java.util.HashMap;

import jd.utils.JDUtilities;

public class Tester {

    public Tester() {
        HashMap<Integer, String> a = new HashMap<Integer, String>();
        a.put(9, "adfasdfasdf");
        a.put(5, "isodafhaisodfh");
        a.put(1, "adsifuhdsafoiuh");
        
        System.out.println(a);
        
        System.out.println(JDUtilities.revSortByKey(a));
    }

    public static void main(String[] args) {
        new Tester();
    }
}
