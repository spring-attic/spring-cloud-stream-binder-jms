package org.springframework.cloud.stream.binder.jms.util;

import javax.jms.Message;

public interface MessageRecoverer {
    void recover(Message message, Throwable cause);
}
