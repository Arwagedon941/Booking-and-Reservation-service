package com.example.platform.fileservice.web;

import com.example.platform.fileservice.dto.ResourceDTO;
import com.example.platform.fileservice.model.ResourceType;
import com.example.platform.fileservice.service.ResourceService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/resources")
public class ResourceController {
    
    private final ResourceService resourceService;
    
    public ResourceController(ResourceService resourceService) {
        this.resourceService = resourceService;
    }
    
    @PostMapping
    @PreAuthorize("hasRole('admin')")
    public ResponseEntity<ResourceDTO> createResource(@Valid @RequestBody ResourceDTO dto) {
        ResourceDTO created = resourceService.createResource(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<ResourceDTO> getResource(@PathVariable("id") Long id) {
        return resourceService.getResourceById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping
    public ResponseEntity<List<ResourceDTO>> getAllResources(
            @RequestParam(name = "type", required = false) ResourceType type,
            @RequestParam(name = "available", required = false) Boolean available) {
        
        List<ResourceDTO> resources;
        if (type != null && available != null && available) {
            resources = resourceService.getAvailableResourcesByType(type);
        } else if (type != null) {
            resources = resourceService.getResourcesByType(type);
        } else if (available != null && available) {
            resources = resourceService.getAvailableResources();
        } else {
            resources = resourceService.getAllResources();
        }
        
        return ResponseEntity.ok(resources);
    }
    
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('admin')")
    public ResponseEntity<ResourceDTO> updateResource(@PathVariable("id") Long id, 
                                                       @Valid @RequestBody ResourceDTO dto) {
        return resourceService.updateResource(id, dto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('admin')")
    public ResponseEntity<Void> deleteResource(@PathVariable("id") Long id) {
        if (resourceService.deleteResource(id)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
    
    @GetMapping("/{id}/availability")
    public ResponseEntity<Boolean> checkAvailability(@PathVariable("id") Long id) {
        boolean available = resourceService.isResourceAvailable(id);
        return ResponseEntity.ok(available);
    }
}


