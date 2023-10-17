package org.gridsuite.ds.server.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class Utils {
    private Utils() {
        // never called
    }

    public static List<String> convertStringToList(String stringArray) {
        List<String> converted = new ArrayList<>();
        converted.addAll(Arrays.asList(stringArray.split(",")));
        return converted;
    }
}
