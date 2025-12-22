package ch.unisg_group1.mealplanner;

import ch.unisg_group1.mealplanner.service.MealPlannerService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class MealPlannerApplicationTests {
	@Autowired
	MealPlannerService service;

	@Test
	void contextLoads() {
		assertThat(service).isNotNull();
	}

	@Test
	void caloriesForEmptyDayIsZero() {
		int calories = service.calculateCaloriesForDay(LocalDate.now());
		assertThat(calories).isZero();
	}

}
