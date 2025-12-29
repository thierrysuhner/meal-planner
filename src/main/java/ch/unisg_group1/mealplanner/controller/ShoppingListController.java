package ch.unisg_group1.mealplanner.controller;

import ch.unisg_group1.mealplanner.model.ShoppingListItem;
import ch.unisg_group1.mealplanner.service.MealPlannerService;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("api/shoppinglists")
public class ShoppingListController {
    private final MealPlannerService service;

    public ShoppingListController(MealPlannerService service) {
        this.service = service;
    }

    // Trigger the generation for a period
    @PostMapping("/generate")
    public void generate(@RequestParam String start, @RequestParam String end) {
        service.generateShoppingListFromMeals(
                LocalDate.parse(start),
                LocalDate.parse(end)
        );
    }

    @GetMapping("/items")
    public List<ShoppingListItem> getAllItems() {
        return service.getAllShoppingListItems();
    }

    @DeleteMapping("/clear")
    public void clearList() {
        service.clearShoppingList();
    }
}
