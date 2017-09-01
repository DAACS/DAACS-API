package com.daacs.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.opensaml.saml2.core.Attribute;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.io.Serializable;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Represents a user session model.
 *
 */
@JsonIgnoreProperties({"password"})
public class SessionedUser extends org.springframework.security.core.userdetails.User implements Serializable{

    private static final long serialVersionUID = 81925387489482411L;

    private static final String fakePassword = "<fake123>";

    private String id;
    private String firstName;
    private String lastName;
    private Boolean hasDataUsageConsent;
    private String secondaryId;
    private String canvasSisId;

    @JsonIgnore
    private List<Attribute> attributes;

    public SessionedUser(User user) {
        super(user.getUsername(), user.getPassword() == null ? fakePassword : user.getPassword(), getGrantAuthorities(user));
        this.id = user.getId();
        this.firstName = user.getFirstName();
        this.lastName = user.getLastName();
        this.hasDataUsageConsent = user.getHasDataUsageConsent();
        this.secondaryId = user.getSecondaryId();
        this.canvasSisId = user.getCanvasSisId();
    }

    private static List<SimpleGrantedAuthority> getGrantAuthorities(User user){
        return user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority(role))
                .collect(Collectors.toList());
    }

    @Override
    public boolean equals(Object rhs) {
        return super.equals(rhs);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    public List<Attribute> getAttributes() {
        return attributes;
    }

    public void setAttributes(List<Attribute> attributes) {
        this.attributes = attributes;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public Boolean getHasDataUsageConsent() {
        return hasDataUsageConsent;
    }

    public void setHasDataUsageConsent(Boolean hasDataUsageConsent) {
        this.hasDataUsageConsent = hasDataUsageConsent;
    }

    public String getSecondaryId() {
        return secondaryId;
    }

    public void setSecondaryId(String secondaryId) {
        this.secondaryId = secondaryId;
    }

    public String getCanvasSisId() {
        return canvasSisId;
    }

    public void setCanvasSisId(String canvasSisId) {
        this.canvasSisId = canvasSisId;
    }
}
