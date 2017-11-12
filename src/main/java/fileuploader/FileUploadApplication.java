package fileuploader;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Created by luisoliveira on 11/15/17.
 */
@SpringBootApplication
@ComponentScan(basePackages = { "fileuploader" })
@EnableJpaRepositories(value = "fileuploader.repositories")
@EntityScan(value = "fileuploader.domain")
public class FileUploadApplication {

	public static void main(String[] args) {
		SpringApplication.run(FileUploadApplication.class, args);
	}

}
