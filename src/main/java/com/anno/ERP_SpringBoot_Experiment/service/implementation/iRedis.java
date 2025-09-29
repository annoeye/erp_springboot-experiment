package com.anno.ERP_SpringBoot_Experiment.service.implementation;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public interface iRedis {
    boolean hasKey(String key);

    void delete(String... keys);

    Long getExpire(String key, TimeUnit timeUnit);

    void setValue(String key, Object value);

    void setValueWithExpiry(String key, Object value, long time, TimeUnit timeUnit);

    Object getValue(String key);

    void hSet(String key, String field, Object value);

    Object hGet(String key, String field);

    Map<Object, Object> hGetAll(String key);

    void hDelete(String key, Object... fields);

    void lPush(String key, Object value);

    Object lPop(String key);

    List<Object> lRange(String key, long start, long end);

    void sAdd(String key, Object... values);

    Set<Object> sMembers(String key);

    void sRemove(String key, Object... values);
}
