package jd;

import java.io.IOException;

public class Tester {

    public static void main(String[] args) throws Exception {
        try {
            throw new Tester().new TestException();

        } catch (IOException e) {
        }
        ;
    }

    class TestException extends IOException {

    }
}
