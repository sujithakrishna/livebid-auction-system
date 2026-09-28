package com.sujitha.livebid.exception;

import java.sql.SQLException;

public class OptimisticLockException extends SQLException {

    public OptimisticLockException(String message) {
        super(message);
    }
}