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


    // TODO: Fix create to account for calories and save ingredients correctly
    @PostMapping
    public Recipe create(@RequestBody Recipe recipe) {
        Recipe newRecipe = service.saveRecipe(recipe);
        return service.updateRecipeDetails(newRecipe.getId(), newRecipe.getPortions(), newRecipe.getIngredients());
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

    // TODO: Fix calorie calculation
    @PostMapping("/{id}/ingredients")
    public Recipe addIngredientToRecipe(@PathVariable Long id, @RequestBody Ingredient ingredient) {
        Recipe recipe = recipeRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Recipe not found"));

        if (recipe.getIngredients() == null) {
            recipe.setIngredients(new ArrayList<>());
        }
        recipe.getIngredients().add(ingredient);
        return recipeRepo.save(recipe);
    }

    @GetMapping("/{id}/ingredients")
    public List<Ingredient> getIngredients(@PathVariable Long id) {
        Recipe recipe = recipeRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Recipe not found"));

        return recipe.getIngredients();
    }

    // TODO: Fix calorie calculation
    @DeleteMapping("/{id}/ingredients/{ingredientId}")
    public Recipe removeIngredientFromRecipe(@PathVariable Long id, @PathVariable Long ingredientId) {
        Recipe recipe = recipeRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Recipe not found"));

        if (recipe.getIngredients() != null) {
            recipe.getIngredients().removeIf(i -> i.getId() != null && i.getId().equals(ingredientId));
        }

        return recipeRepo.save(recipe);
    }

    //TODO: Fix calorie calculation
    @PutMapping("/{id}")
    public Recipe updateRecipe(@PathVariable Long id, @RequestBody Recipe updatedRecipe) {
        // Ensure the recipe exists (otherwise return 404)
        Recipe existing = recipeRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Recipe not found"));

        // Keep the correct ID and keep the current ingredients if none are provided
        updatedRecipe.setId(id);
        if (updatedRecipe.getIngredients() == null) {
            updatedRecipe.setIngredients(existing.getIngredients());
        }

        return recipeRepo.save(updatedRecipe);
    }
}
