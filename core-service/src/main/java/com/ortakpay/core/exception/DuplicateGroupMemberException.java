package com.ortakpay.core.exception;

/**
 * The "DuplicateEmailException benzeri bir exception (409)" the Faz 4 spec asks
 * for addMember's already-a-member case - a distinct type rather than reusing
 * DuplicateEmailException itself, since that one is specifically about
 * registration and would be a misleading name for this unrelated 409.
 */
public class DuplicateGroupMemberException extends RuntimeException {

    public DuplicateGroupMemberException(String message) {
        super(message);
    }
}
