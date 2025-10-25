package com.sp.config;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class EnvironmentConfig {
    private String frontendUrl;
    private String cookieDomain;
    private boolean isLocal;
}