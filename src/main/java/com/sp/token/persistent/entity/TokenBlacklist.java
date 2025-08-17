package com.sp.token.persistent.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class TokenBlacklist {

    @Id
    private String token;

    private LocalDateTime blacklistedAt;
    private LocalDateTime expiresAt;

    public TokenBlacklist(String token, LocalDateTime expiresAt) {
        this.token = token;
        this.blacklistedAt = LocalDateTime.now();
        this.expiresAt = expiresAt;
    }
}