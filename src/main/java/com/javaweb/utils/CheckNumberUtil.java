package com.javaweb.utils;

import static java.lang.Integer.parseInt;

public class CheckNumberUtil {
    public boolean isNumber(String data)
    {
      try {
          Integer.parseInt(data);
          return true;
      } catch ( NumberFormatException e ){
          return false;
      }
    };
}
