package com.lezztto.LmfBank.auth.exception;

/** A regular user reached for data or an action outside their own scope (ADR 0010). */
public class ForbiddenActionException extends RuntimeException {

    public ForbiddenActionException() {
        super("You do not have access to this resource");
    }
}
