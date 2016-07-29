package de.hhu.stups.plues.ui.events;

import de.hhu.stups.plues.data.entities.Course;

public class CourseSelectionChanged {
    private final Course course;

    public CourseSelectionChanged(Course course) {
        this.course = course;
    }

    public Course getCourse() {
        return course;
    }
}
