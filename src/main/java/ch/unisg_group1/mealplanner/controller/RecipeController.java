package ch.unisg_group1.mealplanner.controller;

import ch.unisg_group1.mealplanner.model.Ingredient;
import ch.unisg_group1.mealplanner.model.Recipe;
import ch.unisg_group1.mealplanner.service.MealPlannerService;
import ch.unisg_group1.mealplanner.persistence.RecipeRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("api/recipes")
public class RecipeController {
    private final RecipeRepository recipeRepo;
    private final MealPlannerService service;

    public RecipeController(RecipeRepository recipeRepo, MealPlannerService service) {
        this.recipeRepo = recipeRepo;
        this.service = service;
    }

    @GetMapping
    public List<Recipe> getAll() {
        return recipeRepo.findAll();
    }


    @PostMapping
    public Recipe create(@RequestBody Recipe recipe) {
        service.updateRecipeCalories(recipe, recipe.getPortions());
        return service.saveRecipe(recipe);
    }

    @PostMapping("/suggest")
    public List<Recipe> suggest(@RequestBody Set<String> ingredients) {
        return service.findRecipesMatchingIngredients(ingredients);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        recipeRepo.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Recipe not found"));
        recipeRepo.deleteById(id);
    }

    @PostMapping("/{id}/ingredients")
    public Recipe addIngredientToRecipe(@PathVariable Long id, @RequestBody Ingredient ingredient) {
        Recipe recipe = recipeRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Recipe not found"));

        if (recipe.getIngredients() == null) {
            recipe.setIngredients(new ArrayList<>());
        }
        recipe.getIngredients().add(ingredient);
        service.updateRecipeCalories(recipe,recipe.getPortions());
        return recipeRepo.save(recipe);
    }

    @GetMapping("/{id}/ingredients")
    public List<Ingredient> getIngredients(@PathVariable Long id) {
        Recipe recipe = recipeRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Recipe not found"));

        return recipe.getIngredients();
    }

    @DeleteMapping("/{id}/ingredients/{ingredientId}")
    public Recipe removeIngredientFromRecipe(@PathVariable Long id, @PathVariable Long ingredientId) {
        Recipe recipe = recipeRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Recipe not found"));

        if (recipe.getIngredients() != null) {
            recipe.getIngredients().removeIf(i -> i.getId() != null && i.getId().equals(ingredientId));
        }
        service.updateRecipeCalories(recipe,recipe.getPortions());
        return recipeRepo.save(recipe);
    }

    @PutMapping("/{id}")
    public Recipe updateRecipe(@PathVariable Long id, @RequestBody Recipe updatedRecipe) {
        // Ensure the recipe exists (otherwise return 404)
        Recipe existing = recipeRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Recipe not found"));

        // Keep the correct ID
        updatedRecipe.setId(id);
        // Keep current ingredients if none are provided
        if (updatedRecipe.getIngredients() == null) updatedRecipe.setIngredients(existing.getIngredients());
        // Keep current portions if none are specified
        if (updatedRecipe.getPortions() == 0) updatedRecipe.setPortions(existing.getPortions());
        // Update calories
        service.updateRecipeCalories(updatedRecipe, updatedRecipe.getPortions());

        return recipeRepo.save(updatedRecipe);
    }
}
