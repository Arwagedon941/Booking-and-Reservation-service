package com.example.platform.fileservice.service;

import com.example.platform.fileservice.dto.ResourceDTO;
import com.example.platform.fileservice.model.Resource;
import com.example.platform.fileservice.model.ResourceType;
import com.example.platform.fileservice.repository.ResourceRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class ResourceService {
    
    private final ResourceRepository resourceRepository;
    
    public ResourceService(ResourceRepository resourceRepository) {
        this.resourceRepository = resourceRepository;
    }
    
    @CacheEvict(value = "resources", allEntries = true)
    public ResourceDTO createResource(ResourceDTO dto) {
        Resource resource = new Resource();
        resource.setName(dto.getName());
        resource.setDescription(dto.getDescription());
        resource.setType(dto.getType());
        resource.setPricePerHour(dto.getPricePerHour());
        resource.setCapacity(dto.getCapacity());
        resource.setAvailable(dto.getAvailable() != null ? dto.getAvailable() : true);
        
        Resource saved = resourceRepository.save(resource);
        return toDTO(saved);
    }
    
    @Cacheable(value = "resources", key = "#a0")
    @Transactional(readOnly = true)
    public Optional<ResourceDTO> getResourceById(Long id) {
        return resourceRepository.findById(id)
                .map(this::toDTO);
    }
    
    @Transactional(readOnly = true)
    public List<ResourceDTO> getAllResources() {
        return resourceRepository.findAll().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public List<ResourceDTO> getAvailableResources() {
        return resourceRepository.findByAvailableTrue().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public List<ResourceDTO> getResourcesByType(ResourceType type) {
        return resourceRepository.findByType(type).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public List<ResourceDTO> getAvailableResourcesByType(ResourceType type) {
        return resourceRepository.findByTypeAndAvailableTrue(type).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }
    
    @CacheEvict(value = "resources", allEntries = true)
    public Optional<ResourceDTO> updateResource(Long id, ResourceDTO dto) {
        return resourceRepository.findById(id)
                .map(resource -> {
                    resource.setName(dto.getName());
                    resource.setDescription(dto.getDescription());
                    resource.setType(dto.getType());
                    resource.setPricePerHour(dto.getPricePerHour());
                    resource.setCapacity(dto.getCapacity());
                    if (dto.getAvailable() != null) {
                        resource.setAvailable(dto.getAvailable());
                    }
                    Resource updated = resourceRepository.save(resource);
                    return toDTO(updated);
                });
    }
    
    @CacheEvict(value = "resources", allEntries = true)
    public boolean deleteResource(Long id) {
        if (resourceRepository.existsById(id)) {
            resourceRepository.deleteById(id);
            return true;
        }
        return false;
    }
    
    @Transactional(readOnly = true)
    public boolean isResourceAvailable(Long id) {
        return resourceRepository.findByIdAndAvailableTrue(id).isPresent();
    }
    
    private ResourceDTO toDTO(Resource resource) {
        return new ResourceDTO(
                resource.getId(),
                resource.getName(),
                resource.getDescription(),
                resource.getType(),
                resource.getPricePerHour(),
                resource.getCapacity(),
                resource.getAvailable(),
                resource.getCreatedAt(),
                resource.getUpdatedAt()
        );
    }
    
}

