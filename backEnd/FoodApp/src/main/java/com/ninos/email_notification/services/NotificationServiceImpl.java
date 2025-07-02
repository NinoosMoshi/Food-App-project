package com.ninos.email_notification.services;

import com.ninos.email_notification.dtos.NotificationDTO;
import com.ninos.email_notification.entity.Notification;
import com.ninos.email_notification.repository.NotificationRepository;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private final JavaMailSender javaMailSender;
    private final NotificationRepository notificationRepository;


    @Override
    @Async
    public void sendEmail(NotificationDTO notificationDTO) {
        log.info("inside sendEmail()");

        try {
            MimeMessage mimeMessage = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                    mimeMessage,
                    MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
                    StandardCharsets.UTF_8.name());

            helper.setTo(notificationDTO.getRecipient());
            helper.setSubject(notificationDTO.getSubject());
            helper.setText(notificationDTO.getBody(), notificationDTO.isHtml());

            javaMailSender.send(mimeMessage);

            // SAVE TO DATABASE
            Notification notification = Notification.builder()
                    .recipient(notificationDTO.getRecipient())
                    .subject(notificationDTO.getSubject())
                    .body(notificationDTO.getBody())
                    .type(notificationDTO.getType())
                    .isHtml(notificationDTO.isHtml())
                    .build();

            notificationRepository.save(notification);

            log.info("saved notification successfully to database");

        }catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }
}
