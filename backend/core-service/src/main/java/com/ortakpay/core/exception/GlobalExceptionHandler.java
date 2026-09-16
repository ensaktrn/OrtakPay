package com.ortakpay.core.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // Without this, a malformed path variable (e.g. a non-UUID string where
    // a UUID is expected) throws before this advice's other handlers ever
    // see it, and Spring Security's exception translation turns the
    // unhandled exception into a misleading 401 instead of a 400 - the
    // request never actually had an auth problem.
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ProblemDetail handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "Invalid value for parameter '" + ex.getName() + "': " + ex.getValue());
        problem.setTitle("Invalid Request Parameter");
        return problem;
    }

    @ExceptionHandler(DuplicateEmailException.class)
    public ProblemDetail handleDuplicateEmail(DuplicateEmailException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setTitle("Email Already Registered");
        return problem;
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ProblemDetail handleBadCredentials(BadCredentialsException ex) {
        // Deliberately generic: never reveal whether the email or the password
        // was the wrong part, regardless of what the underlying exception says.
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        problem.setTitle("Authentication Failed");
        return problem;
    }

    @ExceptionHandler(InvalidSplitException.class)
    public ProblemDetail handleInvalidSplit(InvalidSplitException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setTitle("Invalid Split");
        return problem;
    }

    @ExceptionHandler(GroupMembershipException.class)
    public ProblemDetail handleGroupMembership(GroupMembershipException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setTitle("Not a Group Member");
        return problem;
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ProblemDetail handleOptimisticLock(ObjectOptimisticLockingFailureException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT, "Balance was updated concurrently, please retry");
        problem.setTitle("Concurrent Update");
        return problem;
    }

    @ExceptionHandler(GroupNotFoundException.class)
    public ProblemDetail handleGroupNotFound(GroupNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setTitle("Group Not Found");
        return problem;
    }

    @ExceptionHandler(GroupAccessDeniedException.class)
    public ProblemDetail handleGroupAccessDenied(GroupAccessDeniedException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
        problem.setTitle("Not a Group Member");
        return problem;
    }

    @ExceptionHandler(InvalidSettlementException.class)
    public ProblemDetail handleInvalidSettlement(InvalidSettlementException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setTitle("Invalid Settlement");
        return problem;
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ProblemDetail handleUserNotFound(UserNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setTitle("User Not Found");
        return problem;
    }

    @ExceptionHandler(DuplicateGroupMemberException.class)
    public ProblemDetail handleDuplicateGroupMember(DuplicateGroupMemberException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setTitle("Already a Group Member");
        return problem;
    }
}
