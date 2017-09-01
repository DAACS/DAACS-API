package com.daacs.model.item

import com.daacs.framework.serializer.Views
import com.daacs.model.ListItemMappable
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonView
import io.swagger.annotations.ApiModelProperty

import javax.validation.Valid
import javax.validation.constraints.NotNull
import java.time.Instant
/**
 * Created by chostetter on 7/5/16.
 */

@JsonIgnoreProperties(["metaClass", "chosenItemAnswer"])
public class Item implements ListItemMappable {
    @NotNull
    @JsonView([Views.NotExport])
    String id = UUID.randomUUID().toString() //default to new ID

    ItemContent itemContent;

    @NotNull
    String question;

    @NotNull
    String domainId;

    @NotNull
    @Valid
    List<ItemAnswer> possibleItemAnswers;

    @JsonView([Views.NotExport])
    String chosenItemAnswerId;

    @ApiModelProperty(dataType = "java.lang.String")
    @JsonView([Views.NotExport])
    Instant startDate;

    @ApiModelProperty(dataType = "java.lang.String")
    @JsonView([Views.NotExport])
    Instant completeDate;

    public ItemAnswer getChosenItemAnswer(){
        if(chosenItemAnswerId == null) return null;
        if(possibleItemAnswers == null) return null;
        return possibleItemAnswers.find{ it.id == chosenItemAnswerId }
    }
}
