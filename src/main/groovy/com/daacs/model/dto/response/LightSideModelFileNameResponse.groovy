package com.daacs.model.dto.response

import com.daacs.model.dto.UnwrappableResponse

class LightSideModelFileNameResponse extends UnwrappableResponse {

    public String fileName;

    public LightSideModelFileNameResponse(String s) {
        this.fileName = s;
    }

}