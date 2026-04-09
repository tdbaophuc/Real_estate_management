package com.javaweb.utils;

public class CheckNumberUtil {
    public static boolean isNumber(String data)
    {
      try {
          Integer.parseInt(data);
          return true;
      } catch ( NumberFormatException e ){
          return false;
      }
    };
}
