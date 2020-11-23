package com.joseph.demo;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {
    @Test
    public void addition_isCorrect() {
        String input="aa 55";
        System.out.println(input);
        boolean isMatch=input.matches("([0-9a-fA-F\\s])*");
        System.out.println(isMatch);
    }
}