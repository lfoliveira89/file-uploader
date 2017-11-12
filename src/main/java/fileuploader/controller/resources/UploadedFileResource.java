package fileuploader.controller.resources;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.annotations.ApiModel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.springframework.hateoas.Link;

import java.io.InputStream;

/**
 * Created by luisoliveira on 11/11/17.
 */
@Getter
@Setter
@AllArgsConstructor
@Builder
@ApiModel
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UploadedFileResource {

    private Long id;
    private String userId;
    private String filename;
    private String status;
    private Long uploadedTimeInMilliseconds;
    private Integer chunks;
    private InputStream inputStream;
    private Link links;

}
