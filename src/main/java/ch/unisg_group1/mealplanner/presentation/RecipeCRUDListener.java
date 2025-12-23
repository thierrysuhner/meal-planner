package ch.unisg_group1.mealplanner.presentation;


import ch.unisg_group1.mealplanner.model.Recipe;
import ch.unisg_group1.mealplanner.service.MealPlannerService;
import org.vaadin.crudui.crud.CrudListener;

public class RecipeCRUDListener implements CrudListener<Recipe> {

    private final MealPlannerService service;

    public RecipeCRUDListener(MealPlannerService service) {
        this.service = service;
    }

    @Override
    public java.util.List<Recipe> findAll() {
        return service.getAllRecipes();
    }

    @Override
    public Recipe add(Recipe recipe) {
        return service.saveRecipe(recipe);
    }

    @Override
    public Recipe update(Recipe recipe) {
        return service.saveRecipe(recipe);
    }

    @Override
    public void delete(Recipe recipe) {
        service.deleteRecipe(recipe.getId());
    }
}
