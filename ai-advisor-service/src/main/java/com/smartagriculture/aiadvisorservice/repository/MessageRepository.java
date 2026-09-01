package com.smartagriculture.aiadvisorservice.repository;

import com.smartagriculture.aiadvisorservice.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MessageRepository extends JpaRepository<Message, String> {

    List<Message> findByConversationIdOrderByCreatedAtAsc(String conversationId);
}
