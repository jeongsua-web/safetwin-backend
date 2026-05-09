package com.safetwin.common.util;

import com.safetwin.common.exception.ErrorCode;
import com.safetwin.common.exception.SafeTwinException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtils {

    private SecurityUtils() {}

    public static Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new SafeTwinException(ErrorCode.UNAUTHORIZED);
        }
        Object principal = auth.getPrincipal();
        if (principal instanceof Long id) {
            return id;
        }
        throw new SafeTwinException(ErrorCode.UNAUTHORIZED);
    }
}
