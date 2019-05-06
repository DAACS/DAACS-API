package com.daacs.component.utils;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Created by mgoldman on 3/8/19.
 */
@Component
public class CategoryGroupUtilsImpl implements CategoryGroupUtils {

    @Override
    public List<String> getDefaultIds(){
        List<String> labels = new ArrayList<>();
        labels.add(DefaultCatgoryGroup.MATHEMATICS_ID);
        labels.add(DefaultCatgoryGroup.COLLEGE_SKILLS_ID);
        labels.add(DefaultCatgoryGroup.WRITING_ID);
        labels.add(DefaultCatgoryGroup.READING_ID);
        return  labels;
    }

    @Override
    public boolean validateIdFormat(String id) {

        //check for only lowercase alphanumerical characters and '-'. And doesn't begin or end with '-'
        Pattern p = Pattern.compile("^[a-z\\d]([a-z\\d-]*[a-z\\d])?$");
        return (p.matcher(id).find());
    }
}
