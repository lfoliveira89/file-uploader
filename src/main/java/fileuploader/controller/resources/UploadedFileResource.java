package fileuploader.controller.resources;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
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

    @ApiModelProperty("database id")
    private Long id;

    @ApiModelProperty("database userId")
    private String userId;

    @ApiModelProperty("database filename")
    private String filename;

    @ApiModelProperty("database status (Completed, Failed, Pending)")
    private String status;

    @ApiModelProperty("uploaded file response time (ms)")
    private Long uploadedTimeInMilliseconds;

    @ApiModelProperty("database chunks")
    private Integer chunks;

    @ApiModelProperty("uploaded file content bytes")
    private InputStream inputStream;

    @ApiModelProperty("download link")
    private Link links;

}
