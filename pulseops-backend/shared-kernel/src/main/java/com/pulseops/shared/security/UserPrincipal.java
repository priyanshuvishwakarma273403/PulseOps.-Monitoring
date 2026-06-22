package com.pulseops.shared.security;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import java.io.Serializable;
import java.util.Collection;

@Getter
@AllArgsConstructor
public class UserPrincipal implements Serializable {
    private final Long id;
    private final String email;
    private final Collection<? extends GrantedAuthority> authorities;
}
