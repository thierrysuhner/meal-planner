package ch.unisg_group1.mealplanner.controller;

import ch.unisg_group1.mealplanner.model.Ingredient;
import ch.unisg_group1.mealplanner.model.Recipe;
import ch.unisg_group1.mealplanner.service.MealPlannerService;
import ch.unisg_group1.mealplanner.persistence.RecipeRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

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
        return service.getAllRecipes();
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
        recipeRepo.findById(id).orElseThrow(()
                -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Recipe not found"));
        service.deleteRecipe(id);
    }

    @PostMapping("/{id}/ingredients")
    public Recipe addIngredientToRecipe(@PathVariable Long id, @RequestBody Ingredient ingredient) {
        Recipe recipe = recipeRepo.findById(id).orElseThrow(()
                -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Recipe not found"));
        return service.addIngredientToRecipe(recipe,ingredient);
    }

    @GetMapping("/{id}/ingredients")
    public List<Ingredient> getIngredients(@PathVariable Long id) {
        Recipe recipe = recipeRepo.findById(id).orElseThrow(()
                -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Recipe not found"));
        return recipe.getIngredients();
    }

    @DeleteMapping("/{id}/ingredients/{ingredientId}")
    public Recipe removeIngredientFromRecipe(@PathVariable Long id, @PathVariable Long ingredientId) {
        Recipe recipe = recipeRepo.findById(id).orElseThrow(()
                -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Recipe not found"));
        return service.removeIngredientFromRecipe(recipe, ingredientId);
    }

    @PutMapping("/{id}") // semantically, PUT always replaces old object
    public Recipe updateRecipe(@PathVariable Long id, @RequestBody Recipe updatedRecipe) {
        // Ensure the recipe exists (otherwise return 404)
        recipeRepo.findById(id).orElseThrow(()
                -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Recipe not found"));

        return service.replaceRecipe(id, updatedRecipe);
    }
}
