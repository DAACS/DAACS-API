package com.daacs.model.dto

import javax.validation.constraints.NotNull

/**
 * Created by mgoldman
 */

class SendClassInviteRequest {

    @NotNull
    String classId;

    @NotNull
    List<String> userEmails;

    @NotNull
    Boolean forceAccept;
}

