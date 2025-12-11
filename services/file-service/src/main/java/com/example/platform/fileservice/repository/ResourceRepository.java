package com.example.platform.fileservice.repository;

import com.example.platform.fileservice.model.Resource;
import com.example.platform.fileservice.model.ResourceType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ResourceRepository extends JpaRepository<Resource, Long> {
    
    List<Resource> findByType(ResourceType type);
    
    List<Resource> findByAvailableTrue();
    
    List<Resource> findByTypeAndAvailableTrue(ResourceType type);
    
    @Query("SELECT r FROM Resource r WHERE r.available = true AND r.capacity >= :minCapacity")
    List<Resource> findAvailableByMinCapacity(@Param("minCapacity") Integer minCapacity);
    
    Optional<Resource> findByIdAndAvailableTrue(Long id);
}




