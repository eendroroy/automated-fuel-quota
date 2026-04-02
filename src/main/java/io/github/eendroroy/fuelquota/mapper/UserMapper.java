package io.github.eendroroy.fuelquota.mapper;

import io.github.eendroroy.fuelquota.dto.response.UserInfoResponse;
import io.github.eendroroy.fuelquota.entity.User;
import org.springframework.stereotype.Component;

/**
 * Maps {@link User} entities to {@link UserInfoResponse} DTOs.
 *
 * <p>Intentionally excludes the password hash and all PersistenceContext
 * associations to prevent accidental exposure of sensitive data.
 */
@Component
public class UserMapper {

    /**
     * Converts a {@link User} entity to a compact {@link UserInfoResponse}.
     *
     * @param user the source entity (must not be {@code null})
     * @return a populated {@link UserInfoResponse}
     */
    public UserInfoResponse toResponse(User user) {
        return UserInfoResponse.builder()
                .id(user.getId().toString())
                .email(user.getEmail())
                .name(user.getName())
                .role(user.getRole().name())
                .build();
    }
}

