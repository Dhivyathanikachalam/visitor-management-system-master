package com.java.vms.model;


import org.springframework.security.core.GrantedAuthority;

public enum Role implements GrantedAuthority {

    ADMIN,
    GATEKEEPER,
    RESIDENT;

    @Override
    public String getAuthority() {
        return name();
    }
}
