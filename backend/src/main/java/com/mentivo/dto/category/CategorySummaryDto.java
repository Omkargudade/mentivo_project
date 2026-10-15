package com.mentivo.dto.category;

public class CategorySummaryDto {
    private Long id;
    private String name;
    private String slug;
    private String iconName;

    public CategorySummaryDto() {}

    public CategorySummaryDto(Long id, String name, String slug, String iconName) {
        this.id = id;
        this.name = name;
        this.slug = slug;
        this.iconName = iconName;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }

    public String getIconName() { return iconName; }
    public void setIconName(String iconName) { this.iconName = iconName; }
}
