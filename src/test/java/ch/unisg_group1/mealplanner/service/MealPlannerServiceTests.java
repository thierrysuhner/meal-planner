package ch.unisg_group1.mealplanner.service;

import ch.unisg_group1.mealplanner.model.*;
import ch.unisg_group1.mealplanner.persistence.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MealPlannerServiceTests {

    @Mock
    private RecipeRepository recipeRepo;
    @Mock
    private MealRepository mealRepo;
    @Mock
    private ShoppingListItemRepository shoppingListRepo;
    @Mock
    private IngredientRepository ingredientRepo;
    @Mock
    private MealShoppingListRepository mealShoppingListRepo;

    @InjectMocks
    private MealPlannerService service;

    // RECIPE TESTS
    @Test
    void fetchRecipeWithIngredients_ShouldLoadRecipeAndTriggerLazyList() {
        // Arrange
        Long id = 1L;
        Recipe mockRecipe = new Recipe();
        mockRecipe.setId(id);
        mockRecipe.setIngredients(new ArrayList<>());

        when(recipeRepo.findById(id)).thenReturn(Optional.of(mockRecipe));

        // Act
        Recipe result = service.fetchRecipeWithIngredients(id);

        // Assert
        assertThat(result.getId()).isEqualTo(id);
        verify(recipeRepo).findById(id);
    }

    @Test
    void updateRecipeCalories_ShouldCalculateCaloriesFor2PortionsCorrectly() {
        // Arrange
        int newPortions = 2;

        Recipe recipe = new Recipe();
        Ingredient ing1 = new Ingredient(); ing1.setCalories(200);
        Ingredient ing2 = new Ingredient(); ing2.setCalories(100);
        recipe.setIngredients(List.of(ing1, ing2)); // Total 300 kcal

        // Act
        service.updateRecipeCalories(recipe, newPortions);

        // Assert
        // 300 kcal Total / 2 Portions = 150 kcal per portion
        assertThat(recipe.getCalories()).isEqualTo(150);
    }

    @Test
    void updateRecipeCalories_ShouldCalculateCaloriesForMaxPortionsCorrectly() {
        // Arrange
        int newPortions = Integer.MAX_VALUE;

        Recipe recipe = new Recipe();
        Ingredient ing1 = new Ingredient(); ing1.setCalories(Integer.MAX_VALUE-1);
        Ingredient ing2 = new Ingredient(); ing2.setCalories(1);
        recipe.setIngredients(List.of(ing1, ing2)); // Total 300 kcal

        // Act
        service.updateRecipeCalories(recipe, newPortions);

        // Assert
        // 2147483647 kcal Total / 2147483647 Portions = 1 kcal per portion
        assertThat(recipe.getCalories()).isEqualTo(1);
    }

    @Test
    void updateRecipeCalories_ShouldThrowErrorCalculateCaloriesFor0Portions() {
        // Arrange
        Recipe recipe = new Recipe();
        Ingredient ing1 = new Ingredient(); ing1.setCalories(300);
        Ingredient ing2 = new Ingredient(); ing2.setCalories(200);
        recipe.setIngredients(List.of(ing1, ing2)); // Total 300 kcal

        // Act + Assert
        assertThrows(IllegalArgumentException.class, ()
                ->service.updateRecipeCalories(recipe, 0));
    }

    @Test
    void updateRecipeCalories_ShouldThrowErrorCalculateCaloriesForNegativePortions() {
        // Arrange
        Recipe recipe = new Recipe();
        Ingredient ing1 = new Ingredient(); ing1.setCalories(300);
        Ingredient ing2 = new Ingredient(); ing2.setCalories(200);
        recipe.setIngredients(List.of(ing1, ing2)); // Total 300 kcal

        // Act + Assert
        assertThrows(IllegalArgumentException.class, ()
                ->service.updateRecipeCalories(recipe, -1));
    }


    @Test
    void updateRecipeCalories_ShouldCalculateMaxValueCalories() {
        // Arrange
        int newPortions = 1;

        Recipe recipe = new Recipe();
        Ingredient ing1 = new Ingredient(); ing1.setCalories(Integer.MAX_VALUE-1);
        Ingredient ing2 = new Ingredient(); ing2.setCalories(1);
        recipe.setIngredients(List.of(ing1, ing2));

        // Act
        service.updateRecipeCalories(recipe, newPortions);

        // Assert
        assertThat(recipe.getCalories()).isEqualTo(Integer.MAX_VALUE);
    }

    @Test
    void updateRecipeCalories_ShouldThrowErrorCalculateOverflowCalories() {
        // Arrange
        Recipe recipe = new Recipe();
        Ingredient ing1 = new Ingredient(); ing1.setCalories(Integer.MAX_VALUE);
        Ingredient ing2 = new Ingredient(); ing2.setCalories(1);
        recipe.setIngredients(List.of(ing1, ing2)); // Total 300 kcal

        // Act + Assert
        assertThrows(IllegalArgumentException.class, ()
                ->service.updateRecipeCalories(recipe, 1));
    }


    @Test
    void updateRecipeDetails_ShouldUpdateIngredientRelation() {
        // Arrange
        Recipe existing = new Recipe();
        existing.setIngredients(new ArrayList<>());

        Ingredient newIng = new Ingredient();

        when(recipeRepo.findById(any())).thenReturn(Optional.of(existing));
        when(recipeRepo.save(any())).thenAnswer(i -> i.getArgument(0));

        // Act
        Recipe result = service.updateRecipeDetails(1L, 1, List.of(newIng));

        // Assert
        assertThat(result.getIngredients()).contains(newIng);
    }

    // MEAL TESTS

    @Test
    void calculateCaloriesForDay_ShouldSumUpAllMeals() {
        // Arrange
        Recipe r1 = new Recipe(); r1.setCalories(500);
        Recipe r2 = new Recipe(); r2.setCalories(300);

        Meal meal1 = new Meal(); meal1.setRecipes(List.of(r1));
        Meal meal2 = new Meal(); meal2.setRecipes(List.of(r2));

        when(mealRepo.findByStartTimeBetween(any(), any())).thenReturn(List.of(meal1, meal2));

        // Act
        int totalCals = service.calculateCaloriesForDay(LocalDate.now());

        // Assert
        assertThat(totalCals).isEqualTo(800);
    }

    @Test
    void calculateCaloriesForDay_ShouldReturnZeroIfNoMeals() {
        // Arrange
        when(mealRepo.findByStartTimeBetween(any(), any())).thenReturn(Collections.emptyList());

        // Act
        int totalCals = service.calculateCaloriesForDay(LocalDate.now());

        // Assert
        assertThat(totalCals).isZero();
    }

    @Test
    void calculateCaloriesForDay_ShouldSumUpAllMealsMaxValueCalories() {
        // Arrange
        Recipe r1 = new Recipe(); r1.setCalories(Integer.MAX_VALUE-1);
        Recipe r2 = new Recipe(); r2.setCalories(1);

        Meal meal1 = new Meal(); meal1.setRecipes(List.of(r1));
        Meal meal2 = new Meal(); meal2.setRecipes(List.of(r2));

        when(mealRepo.findByStartTimeBetween(any(), any())).thenReturn(List.of(meal1, meal2));

        // Act
        int totalCals = service.calculateCaloriesForDay(LocalDate.now());

        // Assert
        assertThat(totalCals).isEqualTo(Integer.MAX_VALUE);
    }

    @Test
    void calculateCaloriesForDay_ShouldThrowErrorOverflowingCalories() {
        // Arrange
        Recipe r1 = new Recipe(); r1.setCalories(Integer.MAX_VALUE);
        Recipe r2 = new Recipe(); r2.setCalories(1);

        Meal meal1 = new Meal(); meal1.setRecipes(List.of(r1));
        Meal meal2 = new Meal(); meal2.setRecipes(List.of(r2));

        when(mealRepo.findByStartTimeBetween(any(), any())).thenReturn(List.of(meal1, meal2));

        // Act + Assert
        assertThrows(IllegalArgumentException.class, ()
                -> service.calculateCaloriesForDay(LocalDate.now()));
    }

    // SHOPPING LIST TESTS

    @Test
    void getAllShoppingListItems_ShouldAggregateDuplicateItems() {
        // Arrange
        ShoppingListItem i1 = new ShoppingListItem(); i1.setName("Milk"); i1.setUnit("L"); i1.setTotalAmount(1.0);
        ShoppingListItem i2 = new ShoppingListItem(); i2.setName("Milk"); i2.setUnit("L"); i2.setTotalAmount(2.0);

        when(shoppingListRepo.findAll()).thenReturn(List.of(i1, i2));

        // Act
        List<ShoppingListItem> result = service.getAllShoppingListItems();

        // Assert
        assertThat(result.getFirst().getTotalAmount()).isEqualTo(3.0);
    }

    @Test
    void getAllShoppingListItems_ShouldReturnAllDistinctItems() {
        // Arrange
        ShoppingListItem i1 = new ShoppingListItem();
        i1.setName("Milk");
        i1.setUnit("L");
        i1.setTotalAmount(1.0);

        ShoppingListItem i2 = new ShoppingListItem();
        i2.setName("Milk");
        i2.setUnit("g");
        i2.setTotalAmount(2000);

        when(shoppingListRepo.findAll()).thenReturn(List.of(i1, i2));

        // Act
        List<ShoppingListItem> result = service.getAllShoppingListItems();

        // Assert
        assertThat(result)
                .usingRecursiveFieldByFieldElementComparator()
                .containsExactlyInAnyOrder(i1, i2);
    }

    @Test
    void getAllShoppingListItems_ShouldSumUpToMaxValue() {
        // Arrange
        ShoppingListItem i1 = new ShoppingListItem(); i1.setName("Milk"); i1.setUnit("L"); i1.setTotalAmount(Double.MAX_VALUE-1);
        ShoppingListItem i2 = new ShoppingListItem(); i2.setName("Milk"); i2.setUnit("L"); i2.setTotalAmount(1);

        when(shoppingListRepo.findAll()).thenReturn(List.of(i1, i2));

        // Act
        List<ShoppingListItem> result = service.getAllShoppingListItems();

        // Assert
        assertThat(result.getFirst().getTotalAmount()).isEqualTo(Double.MAX_VALUE);
    }

    @Test
    void getAllShoppingListItems_ShouldThrowExceptionOnOverflow() {
        // Arrange
        ShoppingListItem i1 = new ShoppingListItem(); i1.setName("Milk"); i1.setUnit("L"); i1.setTotalAmount(Double.MAX_VALUE);
        ShoppingListItem i2 = new ShoppingListItem(); i2.setName("Milk"); i2.setUnit("L"); i2.setTotalAmount(Double.MAX_VALUE);

        when(shoppingListRepo.findAll()).thenReturn(List.of(i1, i2));

        // Act + Assert
        assertThrows(IllegalArgumentException.class, () -> service.getAllShoppingListItems());
    }


    @Test
    void generateShoppingList_ShouldDoNothingIfNoMeals() {
        // Arrange
        when(mealRepo.findByStartTimeBetween(any(), any())).thenReturn(Collections.emptyList());

        // Act
        service.generateShoppingListFromMeals(LocalDate.now(), LocalDate.now());

        // Assert
        verify(mealShoppingListRepo, never()).save(any());
    }

    @Test
    void generateShoppingList_ShouldCalculateAmountBasedOnPersons() {
        // Arrange
        Ingredient flour = new Ingredient(); flour.setName("Flour"); flour.setUnit("g"); flour.setAmount(400);
        Recipe recipe = new Recipe(); recipe.setPortions(4); recipe.setIngredients(List.of(flour));

        Meal meal = new Meal(); meal.setPersons(2); meal.setRecipes(List.of(recipe));

        when(mealRepo.findByStartTimeBetween(any(), any())).thenReturn(List.of(meal));
        ArgumentCaptor<MealShoppingList> captor = ArgumentCaptor.forClass(MealShoppingList.class);

        // Act
        service.generateShoppingListFromMeals(LocalDate.now(), LocalDate.now());

        // Assert
        verify(mealShoppingListRepo).save(captor.capture());
        ShoppingListItem savedItem = captor.getValue().getItems().getFirst();

        // (400g / 4) * 2 = 200g
        assertThat(savedItem.getTotalAmount()).isEqualTo(200.0);
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 0})
    void generateShoppingList_ShouldThrowExceptionForInvalidPersons(int persons) {
        // Arrange
        Ingredient ing = new Ingredient(); ing.setName("Flour"); ing.setUnit("g"); ing.setAmount(100);
        Recipe recipe = new Recipe(); recipe.setPortions(1); recipe.setIngredients(List.of(ing));
        Meal meal = new Meal(); meal.setPersons(persons); meal.setRecipes(List.of(recipe));

        when(mealRepo.findByStartTimeBetween(any(), any())).thenReturn(List.of(meal));

        // Act + Assert
        assertThrows(IllegalArgumentException.class,
                () -> service.generateShoppingListFromMeals(LocalDate.now(), LocalDate.now()));
    }

    @ParameterizedTest
    @ValueSource(ints = {1, Integer.MAX_VALUE})
    void generateShoppingList_ShouldHandleValidPersons(int persons) {
        // Arrange
        Ingredient ing = new Ingredient(); ing.setName("Flour"); ing.setUnit("g"); ing.setAmount(10);
        Recipe recipe = new Recipe(); recipe.setPortions(2); recipe.setIngredients(List.of(ing));
        Meal meal = new Meal(); meal.setPersons(persons); meal.setRecipes(List.of(recipe));

        when(mealRepo.findByStartTimeBetween(any(), any())).thenReturn(List.of(meal));
        ArgumentCaptor<MealShoppingList> captor = ArgumentCaptor.forClass(MealShoppingList.class);

        // Act
        service.generateShoppingListFromMeals(LocalDate.now(), LocalDate.now());

        // Assert
        verify(mealShoppingListRepo).save(captor.capture());
        ShoppingListItem item = captor.getValue().getItems().getFirst();

        if (persons == 1) {
            assertThat(item.getTotalAmount()).isEqualTo(5.0);
        } else if (persons == Integer.MAX_VALUE) {
            assertThat(item.getTotalAmount()).isEqualTo((double) 5 * Integer.MAX_VALUE);
        }
    }

    @Test
    void generateShoppingList_ShouldRoundUpPieceUnits() {
        // Arrange
        Ingredient egg = new Ingredient(); egg.setName("Egg"); egg.setUnit("pcs"); egg.setAmount(1);
        Recipe recipe = new Recipe(); recipe.setPortions(2); recipe.setIngredients(List.of(egg));

        // Meal: 3 people -> 1.5 eggs
        Meal meal = new Meal(); meal.setPersons(3); meal.setRecipes(List.of(recipe));

        when(mealRepo.findByStartTimeBetween(any(), any())).thenReturn(List.of(meal));
        ArgumentCaptor<MealShoppingList> captor = ArgumentCaptor.forClass(MealShoppingList.class);

        // Act
        service.generateShoppingListFromMeals(LocalDate.now(), LocalDate.now());

        // Assert
        verify(mealShoppingListRepo).save(captor.capture());

        ShoppingListItem item = captor.getValue().getItems().getFirst();
        // Math.ceil(1.5) -> 2.0
        assertThat(item.getTotalAmount()).isEqualTo(2.0);
    }

    @Test
    void generateShoppingList_ShouldAggregateDuplicateIngredients() {
        // Arrange
        Ingredient milk1 = new Ingredient(); milk1.setName("Milk"); milk1.setUnit("L"); milk1.setAmount(1);
        Ingredient milk2 = new Ingredient(); milk2.setName("Milk"); milk2.setUnit("L"); milk2.setAmount(2);

        Recipe recipe1 = new Recipe(); recipe1.setPortions(1); recipe1.setIngredients(List.of(milk1));
        Recipe recipe2 = new Recipe(); recipe2.setPortions(1); recipe2.setIngredients(List.of(milk2));

        Meal meal = new Meal(); meal.setPersons(1); meal.setRecipes(List.of(recipe1, recipe2));

        when(mealRepo.findByStartTimeBetween(any(), any())).thenReturn(List.of(meal));
        ArgumentCaptor<MealShoppingList> captor = ArgumentCaptor.forClass(MealShoppingList.class);

        // Act
        service.generateShoppingListFromMeals(LocalDate.now(), LocalDate.now());

        // Assert
        verify(mealShoppingListRepo).save(captor.capture());
        ShoppingListItem item = captor.getValue().getItems().getFirst();

        assertThat(item.getTotalAmount()).isEqualTo(3.0);
    }

    @Test
    void generateShoppingList_ShouldHandleDifferentUnitsSeparately() {
        Ingredient milkG = new Ingredient(); milkG.setName("Milk"); milkG.setUnit("g"); milkG.setAmount(100);
        Ingredient milkMl = new Ingredient(); milkMl.setName("Milk"); milkMl.setUnit("ml"); milkMl.setAmount(1);

        Recipe recipe1 = new Recipe(); recipe1.setPortions(1); recipe1.setIngredients(List.of(milkG));
        Recipe recipe2 = new Recipe(); recipe2.setPortions(1); recipe2.setIngredients(List.of(milkMl));

        Meal meal = new Meal(); meal.setPersons(2); meal.setRecipes(List.of(recipe1,recipe2));

        when(mealRepo.findByStartTimeBetween(any(), any())).thenReturn(List.of(meal));
        ArgumentCaptor<MealShoppingList> captor = ArgumentCaptor.forClass(MealShoppingList.class);

        service.generateShoppingListFromMeals(LocalDate.now(), LocalDate.now());

        verify(mealShoppingListRepo).save(captor.capture());
        List<ShoppingListItem> items = captor.getValue().getItems();
        assertThat(items).hasSize(2);
    }

    @Test
    void generateShoppingList_ShouldCreateOneMealShoppingListPerMeal() {
        // Arrange
        Ingredient flour = new Ingredient(); flour.setName("Flour"); flour.setUnit("g"); flour.setAmount(100);
        Recipe recipe = new Recipe(); recipe.setPortions(1); recipe.setIngredients(List.of(flour));

        Meal meal1 = new Meal(); meal1.setPersons(1); meal1.setRecipes(List.of(recipe));
        Meal meal2 = new Meal(); meal2.setPersons(2); meal2.setRecipes(List.of(recipe));

        when(mealRepo.findByStartTimeBetween(any(), any())).thenReturn(List.of(meal1, meal2));
        ArgumentCaptor<MealShoppingList> captor = ArgumentCaptor.forClass(MealShoppingList.class);

        // Act
        service.generateShoppingListFromMeals(LocalDate.now(), LocalDate.now());

        // Assert
        verify(mealShoppingListRepo, times(2)).save(captor.capture());
        List<MealShoppingList> savedLists = captor.getAllValues();

        assertThat(savedLists).hasSize(2);
    }

    // INGREDIENT TESTS

    @Test
    void getAllAvailableIngredientNames_ShouldReturnDistinctSortedNames() {
        // Arrange
        Ingredient i1 = new Ingredient(); i1.setName("Cheese");
        Ingredient i2 = new Ingredient(); i2.setName("Tomato");
        Ingredient i3 = new Ingredient(); i3.setName("Cheese"); // Duplikat

        when(ingredientRepo.findAll()).thenReturn(List.of(i1, i2, i3));

        // Act
        List<String> result = service.getAllAvailableIngredientNames();

        // Assert
        assertThat(result).containsExactly("Cheese", "Tomato");
    }

    @Test
    void findRecipesMatchingIngredients_ShouldFindMatchingRecipe() {
        // Arrange
        Ingredient cheese = new Ingredient(); cheese.setName("Cheese");
        Recipe pizza = new Recipe(); pizza.setIngredients(List.of(cheese));

        when(recipeRepo.findAll()).thenReturn(List.of(pizza));

        // Act
        List<Recipe> result = service.findRecipesMatchingIngredients(Set.of("cheese"));

        // Assert
        assertThat(result).contains(pizza);
    }

    @Test
    void findRecipesMatchingIngredients_ShouldReturnEmptyIfNoMatch() {
        // Arrange
        Ingredient tomato = new Ingredient(); tomato.setName("Tomato");
        Recipe salad = new Recipe(); salad.setIngredients(List.of(tomato));

        when(recipeRepo.findAll()).thenReturn(List.of(salad));

        // Act
        List<Recipe> result = service.findRecipesMatchingIngredients(Set.of("Meat"));

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    void findRecipesMatchingIngredients_ShouldFindMultipleMatches() {
        // Arrange
        Ingredient cheese = new Ingredient(); cheese.setName("Cheese");
        Ingredient tomato = new Ingredient(); tomato.setName("Tomato");
        Recipe pizza = new Recipe(); pizza.setIngredients(List.of(cheese));
        Recipe salad = new Recipe(); salad.setIngredients(List.of(tomato));

        when(recipeRepo.findAll()).thenReturn(List.of(pizza, salad));

        // Act
        List<Recipe> result = service.findRecipesMatchingIngredients(Set.of("cheese", "tomato"));

        // Assert
        assertThat(result).containsExactlyInAnyOrder(pizza, salad);
    }
}