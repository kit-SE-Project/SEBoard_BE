package com.seproject.member.domain.repository;

import com.seproject.member.domain.model.Frame;
import com.seproject.member.domain.model.FrameType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FrameRepository extends JpaRepository<Frame, Long> {
    Optional<Frame> findByFrameType(FrameType frameType);
    Optional<Frame> findByName(String name);
    boolean existsByName(String name);
}
