package com.rubyride.tripmanager.service;

import com.rubyride.tripmanager.utility.StreamUtils;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.rest.notify.v1.service.Binding;
import com.twilio.rest.notify.v1.service.Notification;
import com.twilio.type.PhoneNumber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Configuration
@EnableConfigurationProperties
public class TwilioService {
  private static final Logger log = LoggerFactory.getLogger(TwilioService.class);

  @Value("${twilio.accountSid:}")
  private String twilioAccountSid;

  @Value("${twilio.authToken:}")
  private String twilioAuthToken;

  @Value("${twilio.fromNumber:}")
  private String twilioFromNumber;

  @Value("${twilio.serviceSid:}")
  private String twilioServiceSid;

  private PhoneNumber fromNumber;

  @PostConstruct
  public void initializeTwilio() {
    Twilio.init(twilioAccountSid, twilioAuthToken);
    fromNumber = new PhoneNumber(twilioFromNumber);
  }

  public Message sendSMS(final String phoneNumber, final String message) {
    return Message.creator(new PhoneNumber(phoneNumber), fromNumber, message)
        .create();
  }

  public Binding addApnToUserIdentity(final UUID userId, final String apn) {
    return Binding.creator(twilioServiceSid, userId.toString(), Binding.BindingType.APN, apn)
        .create();
  }

  public Binding addFcmToUserIdentity(final UUID userId, final String fcm) {
    return Binding.creator(twilioServiceSid, userId.toString(), Binding.BindingType.FCM, fcm)
        .create();
  }

  public void removeBindingsForUserIdentity(final UUID userId) {
    StreamUtils.streamIterator(
        Binding.reader(twilioServiceSid)
            .setIdentity(List.of(userId.toString()))
            .read()
            .iterator())
        .map(Binding::getSid)
        .forEach(this::removeUserBinding);
  }

  private boolean removeUserBinding(final String bindingSid) {
    return Binding.deleter(twilioServiceSid, bindingSid)
        .delete();
  }

  public Notification notify(final String topic, final UUID userId, final String title, final String message, final Map<String, Object> data) {
    final var notificationCreator = Notification.creator(twilioServiceSid);
    notificationCreator.setIdentity(List.of(userId.toString()));

    final var alertMap = new HashMap<>();

    if (title != null) {
      notificationCreator.setTitle(title);
      alertMap.put("title", title);
    }

    if (message != null) {
      notificationCreator.setBody(message);
      alertMap.put("body", message);
    }

    if (data != null) {
      notificationCreator.setData(data)
          .setFcm(data);

      if (!alertMap.isEmpty()) {
        notificationCreator
            .setApn(Map.of(
                "aps", Map.of(
                    "alert", alertMap,
                    "mutable-content", 1)));
//                "apns-collapse-id", topic));
      }
    }

    return notificationCreator.create();
  }
}
