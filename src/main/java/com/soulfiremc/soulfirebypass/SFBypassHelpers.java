package com.soulfiremc.soulfirebypass;

public class SFBypassHelpers {
  public static final String KEY_PREFIX = "SF_";

  public static int countOccurrences(String haystack, String needle) {
    int count = 0;
    int lastIndex = 0;
    while (lastIndex != -1) {
      lastIndex = haystack.indexOf(needle, lastIndex);
      if (lastIndex != -1) {
        count++;
        lastIndex += needle.length();
      }
    }

    return count;
  }
}
