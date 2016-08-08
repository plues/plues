package de.hhu.stups.plues.ui.components;

import com.google.inject.Inject;
import de.hhu.stups.plues.data.Store;
import de.hhu.stups.plues.data.entities.Course;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.util.Callback;
import javafx.util.StringConverter;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.stream.Collectors;

public class MajorMinorCourseSelection extends VBox implements Initializable {

    @FXML
    @SuppressWarnings("unused")
    private ComboBox<Course> cbMajor;

    @FXML
    @SuppressWarnings("unused")
    private ComboBox<Course> cbMinor;

    @Inject
    public MajorMinorCourseSelection(final FXMLLoader loader){
        loader.setLocation(getClass().getResource("/fxml/components/MajorMinorCourseSelection.fxml"));

        loader.setRoot(this);
        loader.setController(this);

        try {
            loader.load();
        } catch(final IOException exception) {
            throw new RuntimeException(exception);
        }
    }

    @Override
    public void initialize(final URL location, final ResourceBundle resources) {
        cbMajor.setConverter(new CourseConverter());
        cbMinor.setConverter(new CourseConverter());
    }

    public void initializeItemsFromStore(final Store store) {
        final List<Course> courses = store.getCourses();

        final List<Course> majorCourseList = courses.stream()
                .filter(Course::isMajor)
                .collect(Collectors.toList());

        final List<Course> minorCourseList = courses.stream()
                .filter(Course::isMinor)
                .collect(Collectors.toList());

        setMajorCourseList(FXCollections.observableList(majorCourseList));
        setMinorCourseList(FXCollections.observableList(minorCourseList));
    }

    public void highlightImpossibleCourses(final Set<String> impossibleCourses) {
        cbMajor.setCellFactory(getCallbackForImpossibleCourses(impossibleCourses));
        cbMinor.setCellFactory(getCallbackForImpossibleCourses(impossibleCourses));
    }

    private Callback<ListView<Course>, ListCell<Course>> getCallbackForImpossibleCourses(final Set<String> impossibleCourses) {
        return new Callback<ListView<Course>, ListCell<Course>>() {
            @Override
            public ListCell<Course> call(ListView<Course> p) {
                return new ListCell<Course>() {
                    @Override
                    protected void updateItem(Course item, boolean empty) {
                        super.updateItem(item, empty);

                        if (item != null) {
                            setText(item.getFullName());

                            if (impossibleCourses.contains(item.getName())) {
                                setTextFill(Color.RED);
                            } else {
                                setTextFill(Color.BLACK);
                            }

                        }

                    }
                };
            }
        };
    }

    public Course getSelectedMajorCourse(){
        return cbMajor.getSelectionModel().getSelectedItem();
    }

    public Course getSelectedMinorCourse(){
        return cbMinor.getSelectionModel().getSelectedItem();
    }

    private void setMajorCourseList(ObservableList<Course> majorCourseList) {
        cbMajor.setItems(majorCourseList);
        cbMajor.getSelectionModel().select(0);
    }

    private void setMinorCourseList(ObservableList<Course> minorCourseList) {
        cbMinor.setItems(minorCourseList);
        cbMinor.getSelectionModel().select(0);
    }

    private static class CourseConverter extends StringConverter<Course> {
        @Override
        public String toString(final Course object) {
            return object.getFullName();
        }

        @Override
        public Course fromString(final String string) {
            throw new RuntimeException();
        }
    }
}