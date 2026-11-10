package com.games.shipmaster.common.api;

import java.util.UUID;

import org.springframework.core.MethodParameter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import com.games.shipmaster.common.domain.UnauthorizedException;

/** Resolves {@link CurrentPlayer} from the JWT subject so controllers never parse tokens. */
public class CurrentPlayerArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentPlayer.class) && parameter.getParameterType() == UUID.class;
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
            NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (!(auth instanceof JwtAuthenticationToken jwt)) {
            throw new UnauthorizedException("Missing or invalid access token");
        }
        try {
            return UUID.fromString(jwt.getName());
        } catch (IllegalArgumentException e) {
            throw new UnauthorizedException("Missing or invalid access token");
        }
    }
}
