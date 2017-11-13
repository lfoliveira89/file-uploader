package fileuploader.exceptions;

/**
 * Created by luisoliveira on 11/11/17.
 */
public class UnprocessableEntityException extends RuntimeException {

    public UnprocessableEntityException(String error) {
        super(error);
    }

}
