package com.ninos;

import com.ninos.email_notification.dtos.NotificationDTO;
import com.ninos.email_notification.services.NotificationService;
import com.ninos.enums.NotificationType;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;

//@RequiredArgsConstructor
@SpringBootApplication
@EnableAsync
public class FoodAppApplication {

//	private final NotificationService notificationService;

	public static void main(String[] args) {
		SpringApplication.run(FoodAppApplication.class, args);
	}

//	@Bean
//	CommandLineRunner runner() {
//		return args -> {
//			NotificationDTO notificationDTO = NotificationDTO.builder()
//					.recipient("ninos1357@yahoo.com")
//					.subject("Hello Dennis")
//					.body("Hey this is a test email")
//					.type(NotificationType.EMAIL)
//					.build();
//
//			notificationService.sendEmail(notificationDTO);
//		};
//	}

}
