package com.daacs.model.dto

import javax.validation.constraints.NotNull

/**
 * Created by chostetter on 7/25/16.
 */
public class SendHelpEmailRequest {

    @NotNull
    String text;

    @NotNull
    String userAgent;

    @NotNull
    String assessmentId;

}
