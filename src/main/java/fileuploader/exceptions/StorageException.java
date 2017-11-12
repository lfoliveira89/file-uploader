package fileuploader.exceptions;

/**
 * Created by luisoliveira on 11/11/17.
 */
public class StorageException extends RuntimeException {

    public StorageException(String error) {
        super(error);
    }

}
