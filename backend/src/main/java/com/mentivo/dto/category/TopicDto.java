package com.mentivo.dto.category;

public class TopicDto {
    private Long id;
    private String name;
    private String slug;
    private Long categoryId;
    private String categoryName;

    public TopicDto() {}

    public TopicDto(Long id, String name, String slug) {
        this.id = id;
        this.name = name;
        this.slug = slug;
    }

    public TopicDto(Long id, String name, String slug, Long categoryId, String categoryName) {
        this.id = id;
        this.name = name;
        this.slug = slug;
        this.categoryId = categoryId;
        this.categoryName = categoryName;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }

    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }

    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }
}
