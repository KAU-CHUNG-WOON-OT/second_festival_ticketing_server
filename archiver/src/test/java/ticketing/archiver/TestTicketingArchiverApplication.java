package ticketing.archiver;

import org.springframework.boot.SpringApplication;

public class TestTicketingArchiverApplication {

	public static void main(String[] args) {
		SpringApplication.from(TicketingArchiverApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
