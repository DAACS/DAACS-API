package com.daacs.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

/**
 * Created by chostetter on 8/8/16.
 */
@JsonIgnoreProperties(["metaClass"])
class UserSearchResult {
    String firstName
    String lastName
    String username
    String id
}
