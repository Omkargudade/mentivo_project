package com.mentivo.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "topics", indexes = {
        @Index(name = "idx_topic_slug", columnList = "slug"),
        @Index(name = "idx_topic_name", columnList = "name")
})
public class Topic {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 100)
    private String slug;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    public Topic() {}

    public Topic(String name, String slug, Category category) {
        this.name = name;
        this.slug = slug;
        this.category = category;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }

    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }
}
