package com.ticketing.user.api;

import com.ticketing.user.domain.User;

import java.time.Instant;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String fullname,
        String email,
        Instant createdAt
) {
    public static UserResponse from(User user){
        return new UserResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getCreatedAt()
        );
    }
}
