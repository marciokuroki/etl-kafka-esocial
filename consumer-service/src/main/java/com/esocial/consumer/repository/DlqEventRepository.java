package com.esocial.consumer.repository;

import com.esocial.consumer.model.entity.DlqEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DlqEventRepository extends JpaRepository<DlqEvent, Long> {
    
    List<DlqEvent> findByStatus(String status);
    
    List<DlqEvent> findByEventId(String eventId);
}
