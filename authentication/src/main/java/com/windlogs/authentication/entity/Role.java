package com.windlogs.authentication.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Set;

@Getter
@RequiredArgsConstructor
public enum Role {
    ADMIN(Set.of(
            Authority.CREATE_STAFF,
            Authority.AUTHENTICATE,
            Authority.ACTIVATE_ACCOUNT
    )),
    PARTNER(Set.of(
            Authority.REGISTER,
            Authority.AUTHENTICATE,
            Authority.ACTIVATE_ACCOUNT,
            Authority.CREATE_STAFF,
            Authority.CREATE_PROJECT,
            Authority.VIEW_STAFF
    )),
    TESTER(Set.of(
            Authority.AUTHENTICATE
    )),
    DEVELOPER(Set.of(
            Authority.AUTHENTICATE
    )),
    MANAGER(Set.of(
            Authority.AUTHENTICATE
    ));

    private final Set<Authority> authorities;
}
