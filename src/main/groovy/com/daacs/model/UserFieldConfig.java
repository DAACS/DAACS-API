package com.daacs.model;

/**
 * Created by chostetter on 6/30/16.
 */
public class UserFieldConfig {
    private String roleAttribute;
    private String uniqueIdAttribute;
    private String firstNameAttribute;
    private String lastNameAttribute;
    private String secondaryIdAttribute;
    private String canvasSisIdAttribute;

    private String adminRole;
    private String advisorRole;
    private String instructorRole;
    private String studentRole;

    private Boolean useUniqueIdAttributeToLogin;

    public UserFieldConfig(){
        this.roleAttribute = "role";
        this.uniqueIdAttribute = "uuid";
        this.firstNameAttribute = "firstName";
        this.lastNameAttribute = "lastName";
        this.adminRole = "admin";
        this.advisorRole = "advisor";
        this.instructorRole = "instructor";
        this.studentRole = "student";
        this.useUniqueIdAttributeToLogin = false;
    }

    public UserFieldConfig(String roleAttribute, String uniqueIdAttribute, String firstNameAttribute, String lastNameAttribute, String adminRole, String advisorRole, String instructorRole, String studentRole, Boolean useUniqueIdAttributeToLogin, String secondaryIdAttribute, String canvasSisIdAttribute) {
        this.roleAttribute = roleAttribute;
        this.uniqueIdAttribute = uniqueIdAttribute;
        this.firstNameAttribute = firstNameAttribute;
        this.lastNameAttribute = lastNameAttribute;
        this.adminRole = adminRole;
        this.advisorRole = advisorRole;
        this.instructorRole = instructorRole;
        this.studentRole = studentRole;
        this.useUniqueIdAttributeToLogin = useUniqueIdAttributeToLogin;
        this.secondaryIdAttribute = secondaryIdAttribute;
        this.canvasSisIdAttribute = canvasSisIdAttribute;
    }

    public String getRoleAttribute() {
        return roleAttribute;
    }

    public String getUniqueIdAttribute() {
        return uniqueIdAttribute;
    }

    public String getFirstNameAttribute() {
        return firstNameAttribute;
    }

    public String getLastNameAttribute() {
        return lastNameAttribute;
    }

    public String getAdminRole() {
        return adminRole;
    }

    public String getAdvisorRole() {
        return advisorRole;
    }

    public String getInstructorRole() {
        return instructorRole;
    }

    public String getStudentRole() {
        return studentRole;
    }

    public Boolean getUseUniqueIdAttributeToLogin() {
        return useUniqueIdAttributeToLogin;
    }

    public String getSecondaryIdAttribute() {
        return secondaryIdAttribute;
    }

    public String getCanvasSisIdAttribute() {
        return canvasSisIdAttribute;
    }
}
