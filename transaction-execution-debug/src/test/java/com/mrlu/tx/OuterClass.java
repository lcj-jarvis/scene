package com.mrlu.tx;

public class OuterClass {
    private String outerField = "Outer class field";

    public class InnerClass {

        String innerField = "Outer class field";

        public void accessOuterField() {
            // 内部类可以访问外部类的私有字段
            System.out.println(outerField);
        }
    }

    public void accessInnerField() {
        // 内部类可以访问外部类的私有字段
        System.out.println(new InnerClass().innerField);
    }



}
