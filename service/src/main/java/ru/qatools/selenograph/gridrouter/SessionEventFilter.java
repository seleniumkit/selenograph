package ru.qatools.selenograph.gridrouter;

import ru.yandex.qatools.camelot.api.CustomFilter;

import static org.springframework.util.StringUtils.isEmpty;

/**
 * @author Ilya Sadykov
 */
public class SessionEventFilter implements CustomFilter {
    @Override
    public boolean filter(Object body) {
        return body != null && body instanceof SessionEvent
                && !isEmpty(((SessionEvent) body).getSessionId())
                && !isEmpty(((SessionEvent) body).getUser())
                && !isEmpty(((SessionEvent) body).getBrowser())
                && !isEmpty(((SessionEvent) body).getVersion());
    }
}
