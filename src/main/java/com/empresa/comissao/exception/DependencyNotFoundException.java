package com.empresa.comissao.exception;

import lombok.Getter;

@Getter
public class DependencyNotFoundException extends RuntimeException {
    private final String code;
    private final String dependency;

    public DependencyNotFoundException(String message, String dependency) {
        super(message);
        this.code = "DEPENDENCY_NOT_FOUND";
        this.dependency = dependency;
    }
}
