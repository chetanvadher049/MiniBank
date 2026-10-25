package util;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;

import model.annotation.Id;

public final class Repository<T> {
    private final HashMap<String, T> entities = new HashMap<>();

    public T save(T entity) {
        if (entity == null) {
            throw new IllegalArgumentException("Entity cannot be null.");
        }
        entities.put(readId(entity), entity);
        return entity;
    }

    public T findById(String id) {
        return entities.get(id);
    }

    public ArrayList<T> findAll() {
        return new ArrayList<>(entities.values());
    }

    public boolean delete(String id) {
        return entities.remove(id) != null;
    }

    @SafeVarargs
    public final void saveAll(T... items) {
        if (items == null) {
            throw new IllegalArgumentException("Items cannot be null.");
        }
        for (T item : items) {
            save(item);
        }
    }

    public int size() {
        return entities.size();
    }

    public static <N extends Number> double average(N[] values) {
        if (values == null || values.length == 0) {
            throw new IllegalArgumentException("Values cannot be null or empty.");
        }
        double total = 0;
        for (N value : values) {
            if (value == null) {
                throw new IllegalArgumentException("Values cannot contain null.");
            }
            total += value.doubleValue();
        }
        return total / values.length;
    }

    private String readId(T entity) {
        Class<?> type = entity.getClass();
        while (type != null) {
            for (Field field : type.getDeclaredFields()) {
                if (field.isAnnotationPresent(Id.class)) {
                    field.setAccessible(true);
                    try {
                        Object id = field.get(entity);
                        if (id == null) {
                            throw new IllegalStateException("Entity ID cannot be null.");
                        }
                        return id.toString();
                    } catch (IllegalAccessException exception) {
                        throw new IllegalStateException(
                                "Unable to read @Id field " + field.getName(), exception);
                    }
                }
            }
            type = type.getSuperclass();
        }
        throw new IllegalArgumentException(
                "Entity type " + entity.getClass().getName() + " has no @Id field.");
    }
}
