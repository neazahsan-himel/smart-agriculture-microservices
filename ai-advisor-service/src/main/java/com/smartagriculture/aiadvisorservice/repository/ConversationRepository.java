package com.smartagriculture.aiadvisorservice.repository;

import com.smartagriculture.aiadvisorservice.entity.Conversation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ConversationRepository extends JpaRepository<Conversation, String> {

    Optional<Conversation> findByIdAndDeletedFalse(String id);

    Page<Conversation> findByFarmerIdAndDeletedFalse(String farmerId, Pageable pageable);
}
