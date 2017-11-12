package fileuploader.exceptions;

/**
 * Created by luisoliveira on 11/11/17.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String error) {
        super(error);
    }

}
