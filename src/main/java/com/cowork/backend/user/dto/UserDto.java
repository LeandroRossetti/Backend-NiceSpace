package com.cowork.backend.user.dto;

import com.cowork.backend.user.AccountType;
import com.cowork.backend.user.User;

public record UserDto(Long id, String email, String nombre, AccountType accountType) {

    public static UserDto from(User user) {
        return new UserDto(user.getId(), user.getEmail(), user.getNombre(), user.getAccountType());
    }
}
