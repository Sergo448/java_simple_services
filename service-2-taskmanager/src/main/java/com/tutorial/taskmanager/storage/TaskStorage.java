package com.tutorial.taskmanager.storage;

import java.util.ArrayList;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Comparator;
import java.util.stream.Stream;

import com.tutorial.taskmanager.model.Task;
import lombok.Data;


/*@Data*/
public class TaskStorage {

    private final Map<Long, Task> tasks = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);
    

    public Task create(String title, String description) {
        long id = idGenerator.getAndIncrement();
        Task task = new Task(id, title, description);
        tasks.put(id, task);
        return task;
    }

    public List<Task> findAll() {
        return new ArrayList<>(tasks.values());
    }

    public List<Task> findAll(String titleFilter, Boolean completedFilter) {
        return tasks.values().stream()
            .filter(task -> {
                // Фильтр по title (содержит подстроку, регистронезависимый)
                if (titleFilter != null && !titleFilter.isEmpty()) {
                    if (task.getTitle() == null ||
                        !task.getTitle().toLowerCase().contains(titleFilter.toLowerCase())) {
                        return false;
                    }
                }

                // Фильтр по completed
                if (completedFilter != null) {
                    if (task.isCompleted() != completedFilter) {
                        return false;
                    }
                }

                return true;
            })
            .toList();
    }

    public Map<String, Object> findAll(
            String titleFilter,
            Boolean completedFilter,
            String sortBy,
            String order,
            int page,
            int size) {

        // Фильтрация
        Stream<Task> stream = tasks.values().stream()
            .filter(task -> {
                if (titleFilter != null && !titleFilter.isEmpty()) {
                    if (task.getTitle() == null ||
                        !task.getTitle().toLowerCase().contains(titleFilter.toLowerCase())) {
                        return false;
                    }
                }

                if (completedFilter != null) {
                    if (task.isCompleted() != completedFilter) {
                        return false;
                    }
                }

                return true;
            });

        // Сортировка
        Comparator<Task> comparator = getComparator(sortBy);
        if ("desc".equalsIgnoreCase(order)) {
            comparator = comparator.reversed();
        }
        stream = stream.sorted(comparator);

        // Подсчет общего количества до пагинации
        List<Task> allFiltered = stream.toList();
        int total = allFiltered.size();

        // Пагинация
        int skip = page * size;
        List<Task> paginatedTasks = allFiltered.stream()
            .skip(skip)
            .limit(size)
            .toList();

        // Формирование ответа
        Map<String, Object> response = new HashMap<>();
        response.put("tasks", paginatedTasks);
        response.put("total", total);
        response.put("page", page);
        response.put("size", size);
        response.put("totalPages", (int) Math.ceil((double) total / size));

        return response;
    }

    private Comparator<Task> getComparator(String sortBy) {
        return switch (sortBy.toLowerCase()) {
            case "title" -> Comparator.comparing(
                task -> task.getTitle() != null ? task.getTitle().toLowerCase() : "",
                Comparator.nullsLast(String::compareTo)
            );
            case "completed" -> Comparator.comparing(Task::isCompleted);
            case "createdat" -> Comparator.comparing(
                task -> task.getCreatedAt() != null ? task.getCreatedAt() : "",
                Comparator.nullsLast(String::compareTo)
            );
            default -> Comparator.comparing(Task::getId);
        };
    }

    public Optional<Task> findById (long id) {
        return Optional.ofNullable(tasks.get(id));
    }

    public Optional<Task> update(long id, String title, String description, Boolean completed) {
        com.tutorial.taskmanager.model.Task task = tasks.get(id);
        if (task == null) {return Optional.empty();}
        else {
            if (title != null) {task.setTitle(title);}
            if (description != null) {task.setDescription(description);}
            if (completed != null) {task.setCompleted(completed);}
            return Optional.of(task);
        }
    }

    public boolean delete(long id) {
        return tasks.remove(id) != null;
    }
}
