package ch.unisg_group1.mealplanner;

import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.theme.Theme;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.component.page.AppShellConfigurator;

@PageTitle("Meal Planner")
@SpringBootApplication
public class MealPlannerApplication implements AppShellConfigurator {

	public static void main(String[] args) {
		SpringApplication.run(MealPlannerApplication.class, args);
	}

}
