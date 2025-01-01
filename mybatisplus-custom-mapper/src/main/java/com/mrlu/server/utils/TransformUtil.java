package com.mrlu.server.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author 简单de快乐
 */
public class TransformUtil {

    private static final String UNDERLINE = "_";

    /**
     * 下划线转驼峰
     * @param str
     * @return
     */
    public static String underLineToHump(String str){
        if (!str.contains(UNDERLINE)) {
            return str;
        }
        String[] strings = str.split(UNDERLINE);
        StringBuilder stringBuffer = new StringBuilder();
        for(int i = 0;i < strings.length;i++){
            if (i == 0) {
                stringBuffer.append(strings[i].toLowerCase());
            } else {
                stringBuffer.append(strings[i].substring(0,1).toUpperCase()).append(strings[i].substring(1).toLowerCase());
            }
        }
        return stringBuffer.toString();
    }

    /**
     *  驼峰转下划线
     */
    public static String humpToUnderLine(String str){
        Pattern compile = Pattern.compile("[A-Z]");
        Matcher matcher = compile.matcher(str);
        StringBuffer sb = new StringBuffer();
        while(matcher.find()) {
            //将匹配到的大写字符转换成小写，并且在前面添加下划线然后添加到缓冲区。group(0)在没有分配组的时候匹配所有符合的
            matcher.appendReplacement(sb,  "_" + matcher.group(0).toLowerCase());
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

}
