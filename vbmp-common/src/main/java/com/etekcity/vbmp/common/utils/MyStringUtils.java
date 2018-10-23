package com.etekcity.vbmp.common.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import javax.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.*;

public class MyStringUtils {

    private static final Logger logger = LoggerFactory.getLogger(MyStringUtils.class);

    /**
     * @param str
     * @param regex
     * @return
     */
    public static List<String> covertStrToList(String str, String regex) {
        if (!StringUtils.hasLength(regex)) {
            regex = ",";
        }
        List<String> list = new ArrayList<>();
        if (StringUtils.hasLength(str)) {
            list.addAll(Arrays.asList(str.split(regex)));
        }
        return list;
    }

    public static String checkAndToList(String str, String regex, String addElement) {
        if (!StringUtils.hasLength(regex)) {
            regex = ",";
        }
        List<String> list = covertStrToList(str, regex);
        if (StringUtils.hasText(addElement) && !list.contains(addElement)) {
            list.add(addElement);
        } else {
            return str;
        }

        return covertListToStr(list, regex);
    }

    /**
     * @param list
     * @param regex
     * @return
     */
    public static String covertListToStr(List<String> list, String regex) {
        String strList = "";
        if (!StringUtils.hasLength(regex)) {
            regex = ",";
        }
        if (list == null) {
            return strList;
        }
        // 去重，转换为String
        Set<String> set = new HashSet<>(list);
        Iterator<String> iterator = set.iterator();
        StringBuilder stringBuilder = new StringBuilder();
        while (iterator.hasNext()) {
            stringBuilder.append(iterator.next()).append(regex);
        }
        int index = stringBuilder.lastIndexOf(regex);
        if (index >= 0) {
            stringBuilder = stringBuilder.deleteCharAt(index);
        }
        return stringBuilder.toString();
    }

    /**
     * @param list
     * @param regex
     * @param element
     * @return
     */
    public static String findElementAndDelete(String list, String regex, String element) {
        List<String> elementList = covertStrToList(list, regex);
        Iterator<String> iterator = elementList.iterator();
        while (iterator.hasNext()) {
            String item = iterator.next();
            if (item.equals(element)) {
                iterator.remove();
            }
        }
        return covertListToStr(elementList, regex);
    }

    public static boolean isNullData(String... strings) {
        for (String str : strings) {
            if (str == null || "".equals(str)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 解析body中的参数
     *
     * @param httpServletRequest
     * @return
     */
    public static String readRequestBody(HttpServletRequest httpServletRequest) {
        StringBuilder stringBuilder = new StringBuilder();
        try {
            BufferedReader bufferedReader = httpServletRequest.getReader();
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                stringBuilder.append(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return stringBuilder.toString();
    }
}
