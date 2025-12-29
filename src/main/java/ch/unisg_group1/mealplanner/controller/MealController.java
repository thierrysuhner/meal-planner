package ch.unisg_group1.mealplanner.controller;

import ch.unisg_group1.mealplanner.model.Meal;
import ch.unisg_group1.mealplanner.service.MealPlannerService;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("api/meals")
public class MealController {
    private final MealPlannerService service;

    public MealController(MealPlannerService service) {
        this.service = service;
    }

    @GetMapping
    public List<Meal> getMealsInRange(@RequestParam String start, @RequestParam String end) {
        return service.findMealsInRange(
                LocalDate.parse(start).atStartOfDay(),
                LocalDate.parse(end).atTime(23, 59)
        );
    }

    @PostMapping
    public Meal saveMeal(@RequestBody Meal meal) {
        return service.saveMeal(meal);
    }

    @DeleteMapping("/{id}")
    public void deleteMeal(@PathVariable Long id) {
        service.deleteMealById(id);
    }

    @GetMapping("/calories")
    public int getCaloriesForDay(@RequestParam String date) {
        LocalDate localDate = LocalDate.parse(date);
        return service.calculateCaloriesForDay(localDate);
    }
}
