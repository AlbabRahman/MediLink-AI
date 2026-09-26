package com.medilinkai.repository;

import com.medilinkai.model.Reminder;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ReminderRepository extends JpaRepository<Reminder, Long> {
    List<Reminder> findByUserIdOrderByCreatedAtDesc(Long userId);
}
