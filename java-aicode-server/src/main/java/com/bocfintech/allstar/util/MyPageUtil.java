package com.bocfintech.allstar.util;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.bocfintech.allstar.entity.MyPage;
import com.github.pagehelper.ISelect;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MyPageUtil {
    private static Pattern linePattern = Pattern.compile("_(\\w)");
    public static final char UNDERLINE = '_';
    private static Pattern humpPattern = Pattern.compile("[A-Z]");

    public MyPageUtil() {
    }

    public static <T> MyPage<T> getMypage(IPage<T> iPage) {
        MyPage<T> myPage = new MyPage();
        myPage.setContent(iPage.getRecords());
        myPage.setNumber(iPage.getCurrent());
        myPage.setSize(iPage.getSize());
        myPage.setTotalElements(iPage.getTotal());
        myPage.setTotalPages(iPage.getPages());
        return myPage;
    }

    public static <T> MyPage<T> doPage(Integer pageNum, Integer pageSize, ISelect select) {
        Page<T> page = PageHelper.startPage(pageNum, pageSize).doSelectPage(select);
        PageInfo<T> pageInfo = new PageInfo(page);
        MyPage<T> tPage = new MyPage();
        tPage.setContent(page);
        tPage.setNumber((long)pageInfo.getPageSize());
        tPage.setSize((long)pageInfo.getSize());
        tPage.setTotalElements(pageInfo.getTotal());
        tPage.setTotalPages((long)pageInfo.getPages());
        return tPage;
    }

    public static String getUUID() {
        String s = UUID.randomUUID().toString();
        return s;
    }

    public static String[] getUUID(int number) {
        if (number < 1) {
            return null;
        } else {
            String[] ss = new String[number];

            for(int i = 0; i < number; ++i) {
                ss[i] = getUUID();
            }

            return ss;
        }
    }

    public static String getNowTime() {
        return (new SimpleDateFormat("yyyy-MM-dd hh:mm:ss")).format(Calendar.getInstance().getTime());
    }

    public static String lineToHump(String str) {
        str = str.toLowerCase();
        Matcher matcher = linePattern.matcher(str);
        StringBuffer sb = new StringBuffer();

        while(matcher.find()) {
            matcher.appendReplacement(sb, matcher.group(1).toUpperCase());
        }

        matcher.appendTail(sb);
        return sb.toString();
    }

    public static String humpToLine(String str) {
        return str.replaceAll("[A-Z]", "_$0").toLowerCase();
    }

    public static String camelToUnderline(String str) {
        Matcher matcher = humpPattern.matcher(str);
        StringBuffer sb = new StringBuffer();

        while(matcher.find()) {
            matcher.appendReplacement(sb, "_" + matcher.group(0).toLowerCase());
        }

        matcher.appendTail(sb);
        return sb.toString();
    }

    public static String sortOrderAdapter(String sortOrder) {
        if (sortOrder != null && !"".equals(sortOrder.trim())) {
            sortOrder = sortOrder.trim();
            sortOrder = sortOrder.toUpperCase();
            byte var2 = -1;
            switch(sortOrder.hashCode()) {
                case -2020130678:
                    if (sortOrder.equals("DESCEND")) {
                        var2 = 2;
                    }
                    break;
                case 65105:
                    if (sortOrder.equals("ASC")) {
                        var2 = 1;
                    }
                    break;
                case 2094737:
                    if (sortOrder.equals("DESC")) {
                        var2 = 3;
                    }
                    break;
                case 1939611850:
                    if (sortOrder.equals("ASCEND")) {
                        var2 = 0;
                    }
            }

            switch(var2) {
                case 0:
                    return "ASC";
                case 1:
                    return "ASC";
                case 2:
                    return "DESC";
                case 3:
                    return "DESC";
                default:
                    return "";
            }
        } else {
            return "";
        }
    }

    public static String getOrderBy(String sortField, String sortOrder, Boolean isMultiple) throws Exception {
        if (!isMultiple) {
            return getOrderBy(sortField, sortOrder);
        } else {
            String[] fileds = sortField.split(",");
            String[] orders = sortOrder.split(",");
            StringBuilder sb = new StringBuilder();
            if (fileds.length >= orders.length) {
                for(int i = 0; i < fileds.length; ++i) {
                    String field = camelToUnderline(fileds[i]);
                    if (field != "") {
                        String order;
                        if (i > orders.length - 1) {
                            order = sortOrderAdapter("asc");
                        } else {
                            order = sortOrderAdapter(orders[i]);
                        }

                        sb.append(field + " " + order);
                        sb.append(", ");
                    }
                }

                return sb.substring(0, sb.length() - 2);
            } else {
                throw new Exception("请输入合理的排序字段个数");
            }
        }
    }

    public static String getOrderBy(String sortField, String sortOrder) {
        String field = camelToUnderline(sortField);
        if (field != "") {
            String order = sortOrderAdapter(sortOrder);
            return field + " " + order;
        } else {
            return "";
        }
    }
}
