package com.example.platform.fileservice.dto;

import com.example.platform.fileservice.model.ResourceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class ResourceDTO {
    
    private Long id;
    
    @NotBlank(message = "Resource name is required")
    private String name;
    
    private String description;
    
    @NotNull(message = "Resource type is required")
    private ResourceType type;
    
    @NotNull(message = "Price per hour is required")
    @Positive(message = "Price must be positive")
    private BigDecimal pricePerHour;
    
    @NotNull(message = "Capacity is required")
    @Positive(message = "Capacity must be positive")
    private Integer capacity;
    
    private Boolean available;
    
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
    
    // Constructors
    public ResourceDTO() {}
    
    public ResourceDTO(Long id, String name, String description, ResourceType type, 
                      BigDecimal pricePerHour, Integer capacity, Boolean available,
                      LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.type = type;
        this.pricePerHour = pricePerHour;
        this.capacity = capacity;
        this.available = available;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public ResourceType getType() {
        return type;
    }
    
    public void setType(ResourceType type) {
        this.type = type;
    }
    
    public BigDecimal getPricePerHour() {
        return pricePerHour;
    }
    
    public void setPricePerHour(BigDecimal pricePerHour) {
        this.pricePerHour = pricePerHour;
    }
    
    public Integer getCapacity() {
        return capacity;
    }
    
    public void setCapacity(Integer capacity) {
        this.capacity = capacity;
    }
    
    public Boolean getAvailable() {
        return available;
    }
    
    public void setAvailable(Boolean available) {
        this.available = available;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}


