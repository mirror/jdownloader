package jd;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Tester {

    public static void main(String args[]) throws Exception {
        String regex = "\\S+(?<!(\\.p?h?p?))";
        Pattern pattern = Pattern.compile(regex);

        String candidate = "I think that JohnSmith.bla ";
        candidate += "is a fictional chara.dsct.php His real name ";
        candidate += "might be JohnJackson, JohnWestling, ";
        candidate += "or JohnHolmes for all we know.";

        Matcher matcher = pattern.matcher(candidate);

        String tmp = null;

        while (matcher.find()) {
          tmp = matcher.group();
          System.out.println("MATCH:" + tmp);
        }
      }
}
