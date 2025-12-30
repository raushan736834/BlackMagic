package com.blackmagic.BlackMagic.security;

import com.blackmagic.BlackMagic.models.AdminUser;
import jakarta.validation.constraints.NotNull;
import org.jspecify.annotations.NullMarked;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class AdminUserDetails implements UserDetails {

    private final AdminUser user;

    public AdminUserDetails(AdminUser user) {
        this.user = user;
    }

    @Override @NullMarked
    public Collection<? extends GrantedAuthority> getAuthorities() {

        List<GrantedAuthority> authorities = new ArrayList<>();

        // Role authority
        authorities.add(
                new SimpleGrantedAuthority("ROLE_" + user.getRole().name())
        );

        // Permission authorities
        if (user.getPermissions() != null) {
            user.getPermissions().forEach(p ->
                    authorities.add(new SimpleGrantedAuthority(p))
            );
        }

        return authorities;
    }

    @Override
    public String getPassword() {
        return user.getPasswordHash();
    }

    @Override @NullMarked
    public String getUsername() {
        return user.getUsername();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return Boolean.TRUE.equals(user.getActive());
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return Boolean.TRUE.equals(user.getActive());
    }

    /* Optional: Access original user */
    public AdminUser getAdminUser() {
        return user;
    }
}
