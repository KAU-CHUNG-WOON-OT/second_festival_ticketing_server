package ticketing.archiver;

import org.springframework.boot.SpringApplication;

public class TestArchiverApplication {

	public static void main(String[] args) {
		SpringApplication.from(ArchiverApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
