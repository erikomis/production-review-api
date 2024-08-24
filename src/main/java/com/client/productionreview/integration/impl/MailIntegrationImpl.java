package com.client.productionreview.integration.impl;

import com.client.productionreview.integration.MailIntegration;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
public class MailIntegrationImpl  implements MailIntegration {



    private final JavaMailSender javaMailSender;

    public MailIntegrationImpl(JavaMailSender javaMailSender){
        this.javaMailSender = javaMailSender;
    }




    @Override
    public  void send (String mailTo, String message , String subject) {
        SimpleMailMessage simpleMailMessage = new SimpleMailMessage();
        simpleMailMessage.setTo(mailTo);
        simpleMailMessage.setSubject(subject);
        simpleMailMessage.setText(message );

        javaMailSender.send(simpleMailMessage);

        // send email
    }
}
