package com.mcon152.recipeshare.web;

import com.mcon152.recipeshare.Recipe;
import com.mcon152.recipeshare.service.RecipeFactory;
import com.mcon152.recipeshare.service.RecipeService;

// imports for logging
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/recipes")
public class RecipeController {
    // Logging
    private static final Logger logger = LoggerFactory.getLogger(RecipeController.class);
    private final RecipeService recipeService;

    public RecipeController(RecipeService recipeService) {
        this.recipeService = recipeService;
    }

    /**
     * Create a new recipe.
     * Returns 201 Created with Location header pointing to the new resource.
     */
    @PostMapping
    public ResponseEntity<Recipe> addRecipe(@RequestBody RecipeRequest recipeRequest) {
        logger.info("POST /api/recipes - Creating new recipe");
        logger.debug("Recipe request details - title: {}, type: {}",
                recipeRequest.getTitle(), recipeRequest.getType());
        try {
            Recipe toSave = RecipeFactory.createFromRequest(recipeRequest);
            Recipe saved = recipeService.addRecipe(toSave);

            logger.info("Successfully created recipe with id={}", saved.getId());

            URI location = ServletUriComponentsBuilder
                    .fromCurrentRequest()           // /api/recipes
                    .path("/{id}")                  // /{id}
                    .buildAndExpand(saved.getId())
                    .toUri();

            return ResponseEntity.created(location).body(saved);
        } catch (Exception e) {
            logger.error("Error occurred while adding recipe: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Retrieve all recipes. 200 OK.
     */
    @GetMapping
    public ResponseEntity<List<Recipe>> getAllRecipes() {
        logger.info("GET /api/recipes - Retrieving all recipes");
        List<Recipe> recipes = recipeService.getAllRecipes();
        logger.info("Successfully retrieved {} recipes", recipes.size());
        return ResponseEntity.ok(recipes);
    }

    /**
     * Retrieve a recipe by id. 200 OK or 404 Not Found.
     */
    @GetMapping("/{id}")
    public ResponseEntity<Recipe> getRecipeById(@PathVariable long id) {
        logger.info("GET /api/recipes/{} - Retrieving recipe by id", id);

        return recipeService.getRecipeById(id)
                .map(recipe -> {
                    logger.info("Successfully retrieved recipe with id={}", id);
                    return ResponseEntity.ok(recipe);
                })
                .orElseGet(() -> {
                    logger.warn("Recipe with id={} not found", id);
                    return ResponseEntity.notFound().build();
                });
    }

    /**
     * Delete a recipe. 204 No Content if deleted, 404 Not Found otherwise.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRecipe(@PathVariable long id) {
        logger.info("DELETE /api/recipes/{} - Deleting recipe", id);
        try {
            boolean deleted = recipeService.deleteRecipe(id);

            if (deleted) {
                logger.info("Successfully deleted recipe with id={}", id);
                return ResponseEntity.noContent().build();
            } else {
                logger.warn("Recipe with id={} not found for deletion", id);
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            logger.error("Error occurred while deleting recipe with id={}: {}", id, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Replace a recipe (full update). 200 OK with updated entity or 404 Not Found.
     */
    @PutMapping("/{id}")
    public ResponseEntity<Recipe> updateRecipe(@PathVariable long id, @RequestBody RecipeRequest updatedRequest) {
        logger.info("PUT /api/recipes/{} - Updating recipe", id);
        logger.debug("Recipe update request - title: {}, type: {}",
                updatedRequest.getTitle(), updatedRequest.getType());

        Recipe updatedRecipe = RecipeFactory.createFromRequest(updatedRequest);
        return recipeService.updateRecipe(id, updatedRecipe)
                .map(recipe -> {
                    logger.info("Successfully updated recipe with id={}", id);
                    return ResponseEntity.ok(recipe);
                })
                .orElseGet(() -> {
                    logger.warn("Recipe with id={} not found for update", id);
                    return ResponseEntity.notFound().build();
                });
    }

    /**
     * Partial update. 200 OK with updated entity or 404 Not Found.
     */
    @PatchMapping("/{id}")
    public ResponseEntity<Recipe> patchRecipe(@PathVariable long id, @RequestBody RecipeRequest partialRequest) {
        logger.info("PATCH /api/recipes/{} - Partially updating recipe", id);
        logger.debug("Recipe patch request - title: {}, type: {}",
                partialRequest.getTitle(), partialRequest.getType());
        Recipe partialRecipe = RecipeFactory.createFromRequest(partialRequest);
        return recipeService.patchRecipe(id, partialRecipe)
                .map(recipe -> {
                    logger.info("Successfully patched recipe with id={}", id);
                    return ResponseEntity.ok(recipe);
                })
                .orElseGet(() -> {
                    logger.warn("Recipe with id={} not found for patch", id);
                    return ResponseEntity.notFound().build();
                });
    }
}