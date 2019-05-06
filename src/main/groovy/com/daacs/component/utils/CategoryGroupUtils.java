package com.daacs.component.utils;

import java.util.List;

/**
 * Created by mgoldman on 3/8/19.
 */
public interface CategoryGroupUtils {

    List<String> getDefaultIds();

    boolean validateIdFormat(String id);
}

