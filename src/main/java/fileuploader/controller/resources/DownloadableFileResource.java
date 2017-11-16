package fileuploader.controller.resources;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

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
public class DownloadableFileResource {

    @ApiModelProperty("database filename")
    private String filename;

    @ApiModelProperty("uploaded file content bytes")
    private InputStream inputStream;

}
