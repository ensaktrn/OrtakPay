package com.ortakpay.core.exception;

/**
 * Not in the Faz 4 spec's explicit exception list, but required by
 * GroupService.addMember's own description ("email'e göre User bul,
 * bulunamazsa UserNotFoundException -> 404") - added alongside the three
 * named exceptions since the behavior they describe can't be built without it.
 */
public class UserNotFoundException extends RuntimeException {

    public UserNotFoundException(String message) {
        super(message);
    }
}
