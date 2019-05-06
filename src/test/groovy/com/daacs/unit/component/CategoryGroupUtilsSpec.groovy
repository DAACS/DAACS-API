package com.daacs.unit.component

import com.daacs.component.utils.CategoryGroupUtils
import com.daacs.component.utils.CategoryGroupUtilsImpl
import spock.lang.Specification

/**
 * Created by mgoldman on 3/18/19.
 */
class CategoryGroupUtilsSpec extends Specification {

    CategoryGroupUtils categoryGroupUtils

    def setup() {
        categoryGroupUtils = new CategoryGroupUtilsImpl()
    }

    def "validateIdFormat succeeds "() {
        when:
        boolean isValid = categoryGroupUtils.validateIdFormat(id)

        then:
        isValid

        where:
        id << [
                "a",
                "mathematics",
                "mathematics0123",
                "mathematics-advanced",
                "mathematics0123-advanced"
        ]
    }

    def "validateIdFormat fails "() {
        when:
        boolean isValid = categoryGroupUtils.validateIdFormat(id)

        then:
        !isValid

        where:
        id << [
                "mathematics-",
                "-mathematics",
                "màthemàtics" ,
                "mathem+atics",
                "Mathematics" ,
                "matheMatics"
        ]

    }


}
