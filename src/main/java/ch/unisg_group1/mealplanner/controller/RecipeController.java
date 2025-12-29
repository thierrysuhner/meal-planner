package ch.unisg_group1.mealplanner.controller;

import ch.unisg_group1.mealplanner.model.Recipe;
import ch.unisg_group1.mealplanner.service.MealPlannerService;
import ch.unisg_group1.mealplanner.persistence.RecipeRepository;
import org.springframework.web.bind.annotation.*;

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
        return recipeRepo.save(recipe);
    }

    @PostMapping("/suggest")
    public List<Recipe> suggest(@RequestBody Set<String> ingredients) {
        return service.findRecipesMatchingIngredients(ingredients);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        recipeRepo.deleteById(id);
    }
}
