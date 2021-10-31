package utils;

public class StringUtils {

    /**
     * 将字符串某个字母大写
     *
     * @param str   字符串
     * @param index 字母序号
     * @return  转换后的字符串
     */
    public static String capitalize(String str, int index) {
        char[] cs = str.toCharArray();
        cs[index] -= 32;
        return String.valueOf(cs);
    }
}
