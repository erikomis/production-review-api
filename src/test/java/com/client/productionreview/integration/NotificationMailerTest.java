package com.client.productionreview.integration;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class NotificationMailerTest {

    @Test
    void send_delegatesAndSwallowsFailures() {
        MailIntegration mail = mock(MailIntegration.class);
        NotificationMailer mailer = new NotificationMailer(mail);

        mailer.send("a@b.com", "assunto", "corpo");
        verify(mail).send("a@b.com", "corpo", "assunto");

        doThrow(new IllegalStateException("smtp fora do ar")).when(mail).send(anyString(), anyString(), anyString());
        assertDoesNotThrow(() -> mailer.send("a@b.com", "assunto", "corpo"));
    }
}
