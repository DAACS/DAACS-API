package com.daacs.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.format.annotation.DateTimeFormat

import javax.validation.constraints.NotNull
import java.time.Instant
/**
 * Created by mgoldman
 */

@JsonIgnoreProperties(["metaClass"])
@ApiModel
public class PendingStudent {
    @Id
    String id = UUID.randomUUID().toString();

    @NotNull
    @Indexed(unique = true)
    String username;

    List<PendingInvite> pendingInvites = new ArrayList<>();

    @NotNull
    @ApiModelProperty(dataType = "java.lang.String")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    Instant createdDate;

    public PendingStudent(String username, String classId, forceAccept) {
        this.createdDate = Instant.now();
        this.username = username;

        this.pendingInvites.add(new PendingInvite(classId, forceAccept))
    }

    //Mongo needs these constructors to use its getCommand
    PendingStudent() {
    }

    public PendingStudent(String id, String username, List<PendingInvite> pendingInvites, Instant createdDate) {
        this.id = id
        this.username = username
        this.pendingInvites = pendingInvites
        this.createdDate = createdDate
    }
    //

    public addInvite(String classId, forceAccept) {
        this.pendingInvites.add(new PendingInvite(classId, forceAccept))
    }
}

class PendingInvite {
    @NotNull
    String classId

    Boolean forceAccept = false

    PendingInvite(String classId, Boolean forceAccept) {
        this.classId = classId
        this.forceAccept = forceAccept
    }
}