package com.rubyride.tripmanager.service;

import com.rubyride.tripmanager.utility.StreamUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring5.SpringTemplateEngine;

import javax.mail.MessagingException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;

@Service
public class EmailService {
  private static final Logger log = LoggerFactory.getLogger(EmailService.class);

  private final JavaMailSender mailSender;
  private final SpringTemplateEngine thymeleafTemplateEngine;

  @Autowired
  public EmailService(final JavaMailSender mailSender, final SpringTemplateEngine thymeleafTemplateEngine) {
    this.mailSender = mailSender;
    this.thymeleafTemplateEngine = thymeleafTemplateEngine;
  }

  public void sendEmail(final String emailAddress, final String subject, final String body, final Map<String, Resource> attachments, final boolean isHtml) throws MessagingException {
    final var message = mailSender.createMimeMessage();

    final var messageHelper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
    messageHelper.setTo(emailAddress);
    messageHelper.setSubject(subject);
    messageHelper.setText(body, isHtml);

    Optional.ofNullable(attachments)
        .ifPresent(atts -> atts
            .forEach(StreamUtils.uncheckConsumer(messageHelper::addInline)));

    mailSender.send(message);
  }

  public void sendTemplatedEmail(final String emailAddress, final String subject, final String templateName, final Map<String, Object> variableMappings, final Map<String, Resource> attachments) throws MessagingException {
    final var thymeleafContext = new Context();
    thymeleafContext.setVariables(variableMappings);
    final var htmlBody = thymeleafTemplateEngine.process(templateName, thymeleafContext);

    sendEmail(emailAddress, subject, htmlBody, attachments, true);
  }
}
