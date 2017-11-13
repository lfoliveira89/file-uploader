package fileuploader.utils;

/**
 * Created by luisoliveira on 11/13/17.
 */
public final class RestConstants {

    public static final String FILE_UPLOAD_URL_SERVICE = "/api/files";
    public static final String ID_PATH_VARIABLE = "/{id}";

    public static final String ID_PARAM = "id";
    public static final String USER_ID_PARAM = "userId";
    public static final String FILE_PARAM = "file";

    public static final String CONTENT_RANGE_HEADER = "Content-Range";

    private RestConstants() {
        throw new IllegalStateException("Utility class");
    }

}
