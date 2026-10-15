package com.mentivo.security;

import com.mentivo.entity.User;
import com.mentivo.entity.enums.CurrentMode;
import com.mentivo.entity.enums.PrimaryRole;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.stream.Collectors;

public class UserPrincipal implements UserDetails {

    private final Long id;
    private final String email;
    private final String password;
    private final String fullName;
    private final PrimaryRole primaryRole;
    private final CurrentMode currentMode;
    private final boolean isActive;
    private final Collection<? extends GrantedAuthority> authorities;

    public UserPrincipal(Long id, String email, String password, String fullName,
                         PrimaryRole primaryRole, CurrentMode currentMode, boolean isActive,
                         Collection<? extends GrantedAuthority> authorities) {
        this.id = id;
        this.email = email;
        this.password = password;
        this.fullName = fullName;
        this.primaryRole = primaryRole;
        this.currentMode = currentMode;
        this.isActive = isActive;
        this.authorities = authorities;
    }

    public static UserPrincipal create(User user) {
        Collection<GrantedAuthority> authorities = user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority(role.name()))
                .collect(Collectors.toList());

        return new UserPrincipal(
                user.getId(),
                user.getEmail(),
                user.getPassword(),
                user.getFullName(),
                user.getPrimaryRole(),
                user.getCurrentMode(),
                user.isActive(),
                authorities
        );
    }

    public Long getId() { return id; }
    public String getFullName() { return fullName; }
    public PrimaryRole getPrimaryRole() { return primaryRole; }
    public CurrentMode getCurrentMode() { return currentMode; }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() { return authorities; }

    @Override
    public String getPassword() { return password; }

    @Override
    public String getUsername() { return email; }

    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() { return true; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() { return isActive; }
}
