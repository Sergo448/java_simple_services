package com.tutorial.taskmanager.storage;

import java.util.ArrayList;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.List;
import java.util.Map;

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
