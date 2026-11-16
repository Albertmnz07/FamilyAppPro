package albertmnz07.demo.family.web;

import albertmnz07.demo.family.error.CannotRemoveAdminException;
import albertmnz07.demo.family.error.DuplicateDniException;
import albertmnz07.demo.family.error.DuplicateUsernameException;
import albertmnz07.demo.family.error.ForbiddenException;
import albertmnz07.demo.family.error.InviteAlreadyRedeemedException;
import albertmnz07.demo.family.error.InviteExpiredException;
import albertmnz07.demo.family.error.NotFoundException;
import albertmnz07.demo.family.error.UnauthorizedException;
import albertmnz07.demo.family.error.UnsupportedPhotoTypeException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import java.util.List;
import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(UnauthorizedException.class)
    public ProblemDetail unauthorized(UnauthorizedException ex) {
        return problem(HttpStatus.UNAUTHORIZED, "Unauthorized", ex.getMessage(), "unauthorized");
    }

    @ExceptionHandler(ForbiddenException.class)
    public ProblemDetail forbidden(ForbiddenException ex) {
        return problem(HttpStatus.FORBIDDEN, "Forbidden", ex.getMessage(), "forbidden");
    }

    @ExceptionHandler(NotFoundException.class)
    public ProblemDetail notFound(NotFoundException ex) {
        return problem(HttpStatus.NOT_FOUND, "Not found", ex.getMessage(), "not-found");
    }

    @ExceptionHandler(DuplicateDniException.class)
    public ProblemDetail duplicateDni(DuplicateDniException ex) {
        return problem(HttpStatus.CONFLICT, "Conflict", ex.getMessage(), "duplicate-dni");
    }

    @ExceptionHandler(DuplicateUsernameException.class)
    public ProblemDetail duplicateUsername(DuplicateUsernameException ex) {
        return problem(HttpStatus.CONFLICT, "Conflict", ex.getMessage(), "duplicate-username");
    }

    @ExceptionHandler(InviteExpiredException.class)
    public ProblemDetail inviteExpired(InviteExpiredException ex) {
        return problem(HttpStatus.CONFLICT, "Conflict", ex.getMessage(), "invite-expired");
    }

    @ExceptionHandler(InviteAlreadyRedeemedException.class)
    public ProblemDetail inviteAlreadyRedeemed(InviteAlreadyRedeemedException ex) {
        return problem(HttpStatus.CONFLICT, "Conflict", ex.getMessage(), "invite-already-redeemed");
    }

    @ExceptionHandler(CannotRemoveAdminException.class)
    public ProblemDetail cannotRemoveAdmin(CannotRemoveAdminException ex) {
        return problem(HttpStatus.CONFLICT, "Conflict", ex.getMessage(), "cannot-remove-admin");
    }

    @ExceptionHandler(UnsupportedPhotoTypeException.class)
    public ProblemDetail unsupportedPhotoType(UnsupportedPhotoTypeException ex) {
        return problem(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Unsupported media type", ex.getMessage(),
                "unsupported-photo-type");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail invalidBody(MethodArgumentNotValidException ex) {
        List<Map<String, String>> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> Map.of(
                        "field", error.getField(),
                        "message", error.getDefaultMessage() == null ? "invalid" : error.getDefaultMessage()))
                .toList();
        ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, "Validation failed",
                "One or more fields are invalid", "validation-failed");
        problem.setProperty("errors", errors);
        return problem;
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ProblemDetail invalidParameter(ConstraintViolationException ex) {
        List<Map<String, String>> errors = ex.getConstraintViolations().stream()
                .map(violation -> Map.of(
                        "field", violation.getPropertyPath().toString(),
                        "message", violation.getMessage()))
                .toList();
        ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, "Validation failed",
                "One or more parameters are invalid", "validation-failed");
        problem.setProperty("errors", errors);
        return problem;
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ProblemDetail invalidParameter(HandlerMethodValidationException ex) {
        List<Map<String, String>> errors = ex.getAllErrors().stream()
                .map(error -> Map.of(
                        "field", fieldOf(error),
                        "message", error.getDefaultMessage() == null ? "invalid" : error.getDefaultMessage()))
                .toList();
        ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, "Validation failed",
                "One or more parameters are invalid", "validation-failed");
        problem.setProperty("errors", errors);
        return problem;
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ProblemDetail uploadTooLarge(MaxUploadSizeExceededException ex) {
        return problem(HttpStatus.PAYLOAD_TOO_LARGE, "Payload too large",
                "Each DNI photo must be at most 1 MB", "photo-too-large");
    }

    @ExceptionHandler({MissingServletRequestPartException.class, HttpMessageNotReadableException.class,
            IllegalArgumentException.class})
    public ProblemDetail badRequest(Exception ex) {
        return problem(HttpStatus.BAD_REQUEST, "Bad request", ex.getMessage(), "bad-request");
    }

    private String fieldOf(MessageSourceResolvable error) {
        if (error instanceof org.springframework.validation.FieldError fieldError) {
            return fieldError.getField();
        }
        return "request";
    }

    private ProblemDetail problem(HttpStatus status, String title, String detail, String code) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        problem.setProperty("code", code);
        return problem;
    }
}
