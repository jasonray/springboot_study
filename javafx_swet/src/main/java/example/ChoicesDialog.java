package example;



import java.util.HashMap;
import java.util.Map;

import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.Group;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import org.apache.log4j.Category;

import example.ChoiceItem;

@SuppressWarnings("restriction")
// see also: https://github.com/4ntoine/JavaFxDialog/wiki
//
public class ChoicesDialog extends Stage {

	private Stage stage;
	// NOTE: can't do that
	// private static final Map<String, String> userData = new HashMap<>();
	static final Category logger = Category.getInstance(ChoicesDialog.class);
	public static String result = null;
	private Scene scene;
	private ChoiceItem[] choices;

	public ChoiceItem[] getChoices() {
		return choices;
	}

	public void setChoices(ChoiceItem... choices) {
		if (choices.length < 1) {
			throw new IllegalArgumentException(
					"You must provide at least one choice");
		}
		this.choices = choices;
	}

	private Image image;

	public Image getImage() {
		return image;
	}

	private String message;

	public String getMessage() {
		return message;
	}

	public void setMessage(String data) {
		this.message = data;
	}

	// https://stackoverflow.com/questions/34118025/javafx-pass-values-from-child-to-parent
	public String getResult() {
		return result;
	}

	// https://stackoverflow.com/questions/12830402/javafx-2-buttons-size-fill-width-and-are-each-same-width
	@SuppressWarnings("restriction")
	public ChoicesDialog(Stage stage, ChoiceItem... choices) {
		super();
		if (choices.length < 1) {
			throw new IllegalArgumentException(
					"You must provide at least one choice");
		}
		this.stage = stage;
		initOwner(stage);
		setTitle("title");
		initModality(Modality.APPLICATION_MODAL);
		// https://stackoverflow.com/questions/8341305/how-to-remove-javafx-stage-buttons-minimize-maximize-close
		this.initStyle(StageStyle.UNDECORATED);
		Group root = new Group();
		HBox outerHBox = new HBox();
		outerHBox.setAlignment(Pos.CENTER);

		scene = new Scene(root, 300, 200);
		setScene(scene);

		VBox outerVBox = new VBox();
		outerHBox.getChildren().add(outerVBox);
		outerHBox.setSpacing(12);
		scene.setRoot(outerHBox);

		HBox labelHBox = new HBox();
		Label label = new Label();
		label.setText("Do yo relly want to exit?");
		labelHBox.setHgrow(label, Priority.ALWAYS);
		labelHBox.getChildren().add(label);
		outerVBox.setSpacing(12);
		outerVBox.getChildren().addAll(labelHBox);

		int index = 0;
		for (ChoiceItem item : choices) {
			index++;
			HBox buttonHBox = new HBox();
			Button button = new Button();
			button.setText(item.getText());
			button.setMaxWidth(Double.MAX_VALUE);
			Map<String, String> userData = new HashMap<>();
			// userData = new HashMap<>();
			int itemIndex = item.getIndex();
			userData.put("index",
					String.format("%d", (itemIndex != 0 ? itemIndex : index)));
			button.setUserData(userData);
			button.setOnAction(new buttonHandler());
			buttonHBox.setHgrow(button, Priority.ALWAYS);
			buttonHBox.getChildren().add(button);
			// Exception in thread "JavaFX Application Thread"
			// java.lang.IllegalArgumentException: Children: duplicate children
			// added..

			outerVBox.getChildren().add(buttonHBox);
		}

		outerVBox.setSpacing(12);
		/*
		outerVBox.getChildren().addAll(labelHBox, buttonHBox1, buttonHBox2,
				buttonHBox3);
				*/
	}

	private static class buttonHandler implements EventHandler<ActionEvent> {
		public void handle(ActionEvent event) {
			Button button = (Button) event.getSource();

			Map<String, String> data = (Map<String, String>) button.getUserData();
			String eventData = null;
			try {
				eventData = data == null ? null : data.getOrDefault("index", null);
			} catch (Exception e) {
				System.err.println("Exception (ignored) " + e.toString());
				eventData = null;
			}
			logger.info("Received: "
					+ (eventData == null ? "no user data was received" : eventData));
			Stage stage = (Stage) button.getScene().getWindow();
			result = eventData;
			stage.close();
		}

	}
}
