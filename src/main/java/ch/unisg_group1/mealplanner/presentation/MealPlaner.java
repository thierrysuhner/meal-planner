package ch.unisg_group1.mealplanner.presentation;

import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import org.vaadin.stefan.fullcalendar.*;
import org.vaadin.stefan.fullcalendar.CalendarView;
import java.time.LocalDateTime;

@Route(value = "mealplaner", layout = MainView.class)
public class MealPlaner extends VerticalLayout {

    public MealPlaner() {
        // Create a new calendar instance and attach it to our layout
        FullCalendar calendar = FullCalendarBuilder.create().build();
        //calendar.changeView(CalendarView.TIME_GRID_WEEK);

        add(calendar);
    }

}
