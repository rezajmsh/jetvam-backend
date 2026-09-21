package ir.jetvam.infra.i18n;

import java.util.Map;

public interface MessageRepository {

    Map<String, String> findActiveMessages(String localeTag);
}
