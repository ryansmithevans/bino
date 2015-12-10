package com.veasmkii.bino;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.MongoClient;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.MongoIterable;
import javafx.application.Application;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.MultipleSelectionModel;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SelectionMode;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import org.bson.Document;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

public class Main extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        final MongoClient client = new MongoClient();

        final WebView webView = new WebView();
        final WebEngine webEngine = webView.getEngine();

        final ScrollPane scrollPane = new ScrollPane();
        scrollPane.setContent(webView);
        webEngine.loadContent("<b>Empty</b>");


        final ListView<String> collectionList = new ListView<String>();
        final List<String> databaseNames = new ArrayList<>();
        for (final String database : client.listDatabaseNames())
            databaseNames.add(database);

        final ListView<String> databaseList = new ListView<String>();
        final ObservableList<String> databaseValues = FXCollections.observableArrayList(databaseNames);
        final MultipleSelectionModel<String> databaseSelectionModel = databaseList.getSelectionModel();
        databaseSelectionModel.setSelectionMode(SelectionMode.SINGLE);

        final MongoBuilder builder = new MongoBuilder();
        databaseSelectionModel.selectedItemProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
                if (isNotBlank(newValue)) {
                    final String database = loadCollections(databaseList, client, collectionList);
                    builder.setDatabase(database);
                }
            }
        });
        databaseList.setItems(databaseValues);

        final MultipleSelectionModel<String> collectionSelectionModel = collectionList.getSelectionModel();
        collectionSelectionModel.setSelectionMode(SelectionMode.SINGLE);
        collectionSelectionModel.selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (isNotBlank(newValue)) {
                builder.setCollection(newValue);


                final MongoDatabase database = client.getDatabase(builder.getDatabase());
                final MongoCollection<Document> collection = database.getCollection(builder.getCollection());

                final FindIterable<Document> documents = collection.find();

                final List<String> results = new ArrayList<>();
                for (final Document document : documents)
                    results.add(document.toJson());

                final ObjectMapper mapper = new ObjectMapper();
                final StringBuilder resultBuilder = new StringBuilder();
                int count = 1;
                for (String result : results) {

                    try {
                        JsonNode tree = mapper .readTree(result);
                        Object obj = mapper.readValue(result, Object.class);

                        String formattedJson = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(obj);
                        resultBuilder.append("<p><b>/* ").append(count).append(" */</b></p><p><code>")
                                .append(formattedJson)
//                            .append(result.replaceAll("\\\"", "\"").replaceAll("\\{", "<br>{<br>").replaceAll("\\[", "<br>[<br>")
//                                    .replaceAll("\\]", "<br>]<br>").replaceAll("\\}", "<br>}<br>"))
                                .append("</code></p>");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }


                    System.out.println(result);


                    count++;
                }
                webEngine.loadContent(resultBuilder.toString());
            }
        });


        final MongoDatabase database = client.getDatabase("tracking");

        final Scene scene = new Scene(new Group());
        stage.setTitle("Table View Sample");
        stage.setWidth(300);
        stage.setHeight(500);


        final Label label = new Label("Binos");
        label.setFont(new Font("san serif", 20));


        final VBox vbox = new VBox();
        vbox.setSpacing(5);
        vbox.setPadding(new Insets(10, 0, 0, 10));


        vbox.getChildren().addAll(label, databaseList, collectionList, scrollPane);

        ((Group) scene.getRoot()).getChildren().addAll(vbox);

        stage.setScene(scene);
        stage.show();
    }

    private class MongoBuilder {
        private String database;
        private String collection;

        public String getDatabase() {
            return database;
        }

        public void setDatabase(String database) {
            this.database = database;
        }

        public String getCollection() {
            return collection;
        }

        public void setCollection(String collection) {
            this.collection = collection;
        }
    }

    private String loadCollections(ListView<String> databaseList, MongoClient client, ListView<String> collectionList) {
        final String selectedDatabase = databaseList.getSelectionModel().getSelectedItem();
        final MongoDatabase database = client.getDatabase(selectedDatabase);

        final MongoIterable<String> strings = database.listCollectionNames();
        final List<String> collections = new ArrayList<>();
        for (String string : strings)
            collections.add(string);
        collectionList.setItems(FXCollections.observableArrayList(collections));
        return selectedDatabase;
    }

}
