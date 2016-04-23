package ru.qatools.selenograph.ext;

import ru.yandex.qatools.camelot.common.BasicMessagesSerializer;

/**
 * @author Ilya Sadykov
 */
public class SelenographMessagesSerializer extends BasicMessagesSerializer {

    @Override
    public Object deserialize(Object body, ClassLoader classLoader) {
        return body;
    }

    @Override
    public Object serialize(Object body, ClassLoader classLoader) {
        return body;
    }

    @Override
    public String identifyBodyClassName(Object body) {
        return (body != null) ? body.getClass().getName() : null;
    }
}
