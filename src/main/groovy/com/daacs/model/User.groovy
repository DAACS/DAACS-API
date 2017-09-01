package com.daacs.model

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty
import org.opensaml.saml2.core.Attribute
import org.opensaml.xml.schema.XSString
import org.opensaml.xml.schema.impl.XSAnyImpl
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Version
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.security.authentication.encoding.ShaPasswordEncoder
import org.springframework.util.StringUtils

import javax.validation.constraints.NotNull
import java.time.Instant
/**
 * Created by chostetter on 7/1/16.
 */

@JsonIgnoreProperties(["metaClass"])
@ApiModel
@Document(collection = "users")
public class User {

    @Id
    String id;

    @NotNull
    @Indexed(unique = true)
    String username;

    @Indexed(unique = true, sparse = true)
    String secondaryId;

    String canvasSisId;

    @JsonIgnore
    String password;

    String firstName;
    String lastName;

    Boolean hasDataUsageConsent;

    Boolean reportedCompletionToCanvas = false;

    @NotNull
    List<String> roles = [];
    Boolean rolesAreWritable = true;

    @NotNull
    @ApiModelProperty(dataType = "java.lang.String")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    Instant createdDate;

    @NotNull
    @Version
    Long version;

    String resetPasswordCode;

    public User() {
        this.createdDate = Instant.now();
        this.version = 0L;
    }

    public User(SessionedUser sessionedUser){
        this.username = sessionedUser.getUsername();
        this.id = sessionedUser.getId();
        this.firstName = sessionedUser.getFirstName();
        this.lastName = sessionedUser.getLastName();
        this.secondaryId = sessionedUser.getSecondaryId();
        this.canvasSisId = sessionedUser.getCanvasSisId();
        this.roles = sessionedUser.getAuthorities().collect{ it ->
            it.getAuthority();
        }
    }

    public User(String username, String password, String firstName, String lastName, Boolean hasDataUsageConsent, List<String> roles, String secondaryId, String canvasSisId) {
        this.username = username;
        this.password = encodePassword(password);
        this.firstName = firstName;
        this.lastName = lastName;
        this.version = 0L;
        createdDate = Instant.now();
        this.roles = roles;
        this.hasDataUsageConsent = hasDataUsageConsent;
        this.secondaryId = secondaryId;
        this.canvasSisId = canvasSisId;
    }

    public User(String username, String firstName, String lastName) {
        this.username = username;
        this.firstName = firstName;
        this.lastName = lastName;
        this.version = 0L;
        createdDate = Instant.now();
        this.roles = [];
        this.hasDataUsageConsent = null;
        this.secondaryId = null;
        this.canvasSisId = null;
    }

    public User(String username, List<Attribute> attributes, UserFieldConfig userFieldConfig){
        this.username = username;
        this.password = null;

        List<String> uniqueIdValues = getAttributeValue(attributes, userFieldConfig.getUniqueIdAttribute());
        id = uniqueIdValues.size() > 0 ? uniqueIdValues.get(0) : username;

        List<String> firstNameValues = getAttributeValue(attributes, userFieldConfig.getFirstNameAttribute());
        firstName = firstNameValues.size() > 0 ? firstNameValues.get(0) : "";

        List<String> lastNameValues = getAttributeValue(attributes, userFieldConfig.getLastNameAttribute());
        lastName = lastNameValues.size() > 0 ? lastNameValues.get(0) : "";

        List<String> secondaryIdValues = getAttributeValue(attributes, userFieldConfig.getSecondaryIdAttribute());
        secondaryId = secondaryIdValues.size() > 0 ? secondaryIdValues.get(0) : null;

        List<String> canvasSisIdIdValues = getAttributeValue(attributes, userFieldConfig.getCanvasSisIdAttribute());
        canvasSisId = canvasSisIdIdValues.size() > 0 ? canvasSisIdIdValues.get(0) : null;

        roles = determineRoles(attributes, userFieldConfig);
    }

    private static String encodePassword(String password){
        return new ShaPasswordEncoder().encodePassword(password, null);
    }

    private static List<String> determineRoles(List<Attribute> attributes, UserFieldConfig userFieldConfig){
        List<String> roles = [];

        if(userFieldConfig.getRoleAttribute() != null) {
            attributes.findAll{ attribute ->
                attribute.getName().equals(userFieldConfig.getRoleAttribute())
            }
            .each{ attribute ->
                attribute.getAttributeValues().each{ xmlObject ->
                    String roleValue;
                    if(xmlObject instanceof XSAnyImpl){
                        roleValue = ((XSAnyImpl) xmlObject).getTextContent();
                    }
                    else{
                        roleValue = ((XSString) xmlObject).getValue();
                    }

                    if(roleValue.equals(userFieldConfig.getStudentRole())){
                        roles.add("ROLE_STUDENT");
                    }

                    if(roleValue.equals(userFieldConfig.getAdminRole())){
                        roles.add("ROLE_ADMIN");
                    }

                    if(roleValue.equals(userFieldConfig.getAdvisorRole())){
                        roles.add("ROLE_ADVISOR");
                    }
                }
            }
        }

        return roles;
    }

    private List<String> getAttributeValue(List<Attribute> attributes, String attributeName){
        if(StringUtils.isEmpty(attributeName)){
            return [];
        }

        List<String> values = [];

        attributes.findAll{ attribute ->
            attribute.getName().equals(attributeName)
        }
        .each{ attribute ->
            attribute.getAttributeValues().each{ xmlObject ->
                String value;
                if(xmlObject instanceof XSAnyImpl){
                    value = ((XSAnyImpl) xmlObject).getTextContent();
                }
                else{
                    value = ((XSString) xmlObject).getValue();
                }

                values.add(value);
            }
        }

        return values;
    }

    void setRoles(List<String> roles) {
        if(rolesAreWritable){
            this.roles = roles
        }
    }

    void setRolesAreWritable(Boolean rolesAreWritable) {
        if(this.rolesAreWritable){
            this.rolesAreWritable = rolesAreWritable
        }
    }

    void setAndEncodePassword(String password) {
        this.password = encodePassword(password);
    }
}
