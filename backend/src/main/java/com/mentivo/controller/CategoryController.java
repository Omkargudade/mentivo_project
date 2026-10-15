package com.mentivo.controller;

import com.mentivo.dto.ApiResponse;
import com.mentivo.dto.category.CategoryDto;
import com.mentivo.dto.category.SkillDto;
import com.mentivo.dto.category.TopicDto;
import com.mentivo.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@Tag(name = "Taxonomy", description = "Categories, skills, and learning topics")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping("/categories")
    @Operation(summary = "Get all categories with nested topics and skills")
    public ResponseEntity<ApiResponse<List<CategoryDto>>> getAllCategories() {
        List<CategoryDto> categories = categoryService.getAllCategories();
        return ResponseEntity.ok(ApiResponse.success(categories));
    }

    @GetMapping("/categories/{slug}")
    @Operation(summary = "Get category details by slug")
    public ResponseEntity<ApiResponse<CategoryDto>> getCategoryBySlug(@PathVariable String slug) {
        CategoryDto category = categoryService.getCategoryBySlug(slug);
        return ResponseEntity.ok(ApiResponse.success(category));
    }

    @GetMapping("/skills")
    @Operation(summary = "Get all platform skills")
    public ResponseEntity<ApiResponse<List<SkillDto>>> getAllSkills() {
        List<SkillDto> skills = categoryService.getAllSkills();
        return ResponseEntity.ok(ApiResponse.success(skills));
    }

    @GetMapping("/topics")
    @Operation(summary = "Get all platform topics")
    public ResponseEntity<ApiResponse<List<TopicDto>>> getAllTopics() {
        List<TopicDto> topics = categoryService.getAllTopics();
        return ResponseEntity.ok(ApiResponse.success(topics));
    }
}
