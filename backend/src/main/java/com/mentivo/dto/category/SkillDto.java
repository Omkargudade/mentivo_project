package com.mentivo.dto.category;

public class SkillDto {
    private Long id;
    private String name;
    private Long categoryId;
    private String categoryName;

    public SkillDto() {}

    public SkillDto(Long id, String name) {
        this.id = id;
        this.name = name;
    }

    public SkillDto(Long id, String name, Long categoryId, String categoryName) {
        this.id = id;
        this.name = name;
        this.categoryId = categoryId;
        this.categoryName = categoryName;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }

    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }
}
