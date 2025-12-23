package ch.unisg_group1.mealplanner.presentation;

import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import org.vaadin.stefan.fullcalendar.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Route(value = "mealplaner", layout = MainView.class)
public class MealPlaner extends VerticalLayout {

    public MealPlaner() {
        // Create a new calendar instance and attach it to our layout
        FullCalendar calendar = FullCalendarBuilder.create().build();
        calendar.changeView(CalendarViewImpl.DAY_GRID_WEEK);
        calendar.setSizeFull();

        // Create a initial sample entry
        Entry entry = new Entry();
        entry.setTitle("Some event");
        entry.setColor("#ff3333");

        // the given times will be interpreted as utc based - useful when the times are fetched from your database
        entry.setStart(LocalDate.now().withDayOfMonth(3).atTime(10, 0));
        entry.setEnd(entry.getStart().plusHours(2));

        // FC uses a data provider concept similar to the Vaadin default's one, with some differences
        // By default the FC uses a in-memory data provider, which is sufficient for most basic use cases.
        calendar.getEntryProvider().asInMemory().addEntries(entry);

        add(calendar);
        setSizeFull();
    }

}
