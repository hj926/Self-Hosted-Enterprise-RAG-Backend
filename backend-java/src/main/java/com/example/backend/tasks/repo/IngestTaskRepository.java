package com.example.backend.tasks.repo;

import java.util.List;

import com.example.backend.tasks.entity.IngestTaskEntity;

import org.springframework.data.jpa.repository.JpaRepository;

public interface IngestTaskRepository extends JpaRepository<IngestTaskEntity, String> {
  List<IngestTaskEntity> findTop10ByStatusOrderByTaskIdAsc(String status);
}