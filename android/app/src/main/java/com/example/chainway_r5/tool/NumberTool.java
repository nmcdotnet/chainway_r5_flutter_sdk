package com.example.chainway_r5.tool;

import java.math.BigDecimal;

public class NumberTool {
    public static double getPointDouble(int point, double val) {
        BigDecimal bd = new BigDecimal(val);
        return bd.setScale(point, BigDecimal.ROUND_HALF_UP).doubleValue();
    }
}
