package fileuploader.controller;

import fileuploader.exceptions.StorageException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

/**
 * Created by luisoliveira on 11/11/17.
 */
@ControllerAdvice
public class ControllerExceptionHandler {

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity handleMissingParametersException(MissingServletRequestParameterException ex) {
        String errorMsg = ex.getParameterName() + " parameter is missing";
        return ResponseEntity.unprocessableEntity().body(errorMsg);
    }

    @ExceptionHandler(StorageException.class)
    public ResponseEntity handleStorageException(StorageException ex) {
        return ResponseEntity.unprocessableEntity().body(ex.getMessage());
    }

}
