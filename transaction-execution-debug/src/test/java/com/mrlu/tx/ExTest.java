package com.mrlu.tx;

/**
 * @author 简单de快乐
 * @create 2024-07-30 11:05
 */
public class ExTest {

    public static void main(String[] args) {
        try {
            String a = null;
            a.substring(1);
        } catch (NullPointerException e) {
            System.out.println(1);
            throw e;
        } catch (RuntimeException e) {
            System.out.println(2);
            throw e;
        } catch (Exception e) {
            System.out.println(3);
            throw e;
        }
    }




}
