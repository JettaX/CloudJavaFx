package com.cloud.cloudclient.fxcontrollers;


import com.cloud.cloudclient.Main;
import com.cloud.cloudclient.network.Connection;
import com.cloud.cloudclient.utils.FileUtil;
import com.cloud.cloudclient.utils.FilesChecker;
import com.cloud.cloudclient.view.Indicators;
import com.cloud.cloudclient.view.PopupControlRename;
import com.cloud.cloudclient.view.utils.RoundPicture;
import com.cloud.cloudclient.view.utils.WindowUtil;
import com.cloud.common.entity.CloudFile;
import com.cloud.common.entity.CloudFolder;
import javafx.application.HostServices;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.*;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Objects;

@Slf4j
public class HomeController {
    @FXML
    public Button searchButton;
    @FXML
    public TextField search;
    @FXML
    public HBox searchWrapper;
    @FXML
    public HBox titleWrapper;
    @FXML
    public HBox settingsWrapper;
    @FXML
    public Button settings;
    @FXML
    public Button account;
    @FXML
    public Pane foldersWrapper;
    @FXML
    public HBox toolBox;
    @FXML
    public ToggleButton serverSource;
    @FXML
    public ToggleButton localSource;
    @FXML
    public Button reload;
    @FXML
    public Button downloadsButton;
    @FXML
    public ToggleButton tableView;
    @FXML
    public ToggleButton listView;
    @FXML
    public ScrollPane scrollPaneHome;
    @FXML
    public Button addFiles;
    private CloudFolder currentCloudFolder;
    private boolean isSearch;
    private Stage stage;
    private HostServices hostServices;

    public void initializer(Stage stage) {
        this.stage = stage;
        isSearch = false;
        initAccountButton();
        initGraphics();
        toggleView();
        toggleSource();
        foldersWrapperInit();
        Platform.runLater(() -> {
            if (Main.root != null) {
                generateHierarchy(Main.root);
            }
        });
    }

    private void foldersWrapperInit() {
        if (listView.isSelected()) {
            foldersWrapper = new VBox();
        } else {
            foldersWrapper = new FlowPane();
        }
        foldersWrapper.getStyleClass().add("folders-wrapper");
        scrollPaneHome.contentProperty().set(foldersWrapper);
    }

    private void toggleView() {
        ToggleGroup toggleGroup = new ToggleGroup();
        listView.setToggleGroup(toggleGroup);
        tableView.setToggleGroup(toggleGroup);

        toggleGroup.selectedToggleProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == null) {
                oldValue.setSelected(true);
            } else {
                foldersWrapperInit();
                generateHierarchy(currentCloudFolder);
            }
        });
    }

    private void initAccountButton() {
        account.setOnMousePressed(event -> {
            if (event.isSecondaryButtonDown()) {
                createContextMenuForAccount().show(account, event.getScreenX(), event.getScreenY());
            }
        });
    }

    private ContextMenu createContextMenuForAccount() {
        ContextMenu contextMenu = new ContextMenu();
        MenuItem exit = new MenuItem("exit");
        exit.setOnAction(event -> Main.disconnect());
        contextMenu.getItems().add(exit);
        return contextMenu;
    }

    private void initGraphics() {
        graphicsApply(addFiles, "/images/icon/plus.png", 20, 20);
        graphicsApply(downloadsButton, "/images/icon/downloads.png", 30, 30);
        graphicsApply(searchButton, "/images/icon/search.png", 30, 30);
        graphicsApply(reload, "/images/icon/spinner.png", 20, 20);
        graphicsApply(settings, "/images/icon/cog.png", 30, 30);
        graphicsApplyForToggles(serverSource, "Cloud", "/images/icon/cloud.png", 30, 30);
        graphicsApplyForToggles(localSource, "My Computer", "/images/icon/display.png", 30, 30);
        graphicsApply(listView, "/images/icon/list.png", 20, 20);
        graphicsApply(tableView, "/images/icon/table2.png", 20, 20);
        account.setGraphic(RoundPicture.getRoundPicture(30, Main.user.getImageUrl()));
    }

    private void graphicsApply(ButtonBase button, String iconPath, int width, int height) {
        button.setGraphic(new ImageView(new Image(Objects.requireNonNull(getClass().getResourceAsStream(iconPath)), width, height, false, true)));
    }

    private void graphicsApply(MenuItem menuItem, String iconPath, int width, int height) {
        menuItem.setGraphic(new ImageView(new Image(Objects.requireNonNull(getClass().getResourceAsStream(iconPath)), width, height, false, true)));
    }

    private void graphicsApplyForToggles(ToggleButton toggle, String text, String iconPath, int width, int height) {
        HBox hBox = new HBox();
        hBox.setSpacing(10);
        hBox.setAlignment(Pos.CENTER_LEFT);
        Label label0 = new Label();
        label0.setGraphic(new ImageView(new Image(Objects.requireNonNull(getClass().getResourceAsStream(iconPath)), width, height, false, true)));
        Label label = new Label(text);
        hBox.getChildren().addAll(label0, label);
        toggle.setGraphic(hBox);
    }

    private void toggleSource() {
        ToggleGroup toggleGroup = new ToggleGroup();
        serverSource.setToggleGroup(toggleGroup);
        localSource.setToggleGroup(toggleGroup);

        toggleGroup.selectedToggleProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == null) {
                oldValue.setSelected(true);
            } else if (newValue.equals(serverSource)) {
                isSearch = false;
                Platform.runLater(() -> generateHierarchy(Main.root));
            } else {
                isSearch = false;
                Platform.runLater(() -> {
                    var folder = FileUtil.getRootFolder();
                    generateHierarchy(folder);
                    FilesChecker.refillLocalFiles(folder);
                });
            }
        });
    }

    public void generateHierarchy(CloudFolder cloudFolder) {
        foldersWrapper.getChildren().clear();
        currentCloudFolder = cloudFolder;
        if (!cloudFolder.getCloudFolders().isEmpty() || !cloudFolder.getCloudFiles().isEmpty()) {
            generateFolders(cloudFolder);
            generateFiles(cloudFolder);
        }
        foldersWrapper.setOnDragDropped(event -> setDragDroppedListener(event, currentCloudFolder));
        foldersWrapper.setOnDragOver(this::setDragOver);
    }

    private void generateFolders(CloudFolder cloudFolder) {
        cloudFolder.getCloudFolders().forEach(folder -> {
            //TODO Check folder
            Button button = createButton(folder.getName(), "/images/icon/folder.png", false, false);
            ContextMenu contextMenu = createContextMenuForFolder(folder);
            setFolderListener(button, folder, contextMenu);
            foldersWrapper.getChildren().add(button);
        });
    }

    private void setFolderListener(Button button, CloudFolder cloudFolder, ContextMenu contextMenu) {
        button.setOnMouseClicked(event -> {
            if (event.getButton().equals(MouseButton.PRIMARY)) {
                Platform.runLater(() -> generateHierarchy(cloudFolder));
            } else if (event.getButton().equals(MouseButton.SECONDARY)) {
                contextMenu.show(button, event.getScreenX(), event.getScreenY());
            }
        });
        button.setOnDragOver(this::setDragOver);
        button.setOnDragDropped(event -> setDragDroppedListener(event, cloudFolder));
    }

    private void setDragDroppedListener(DragEvent event, CloudFolder cloudFolder) {
        List<File> files = event.getDragboard().getFiles();
        files.forEach(file -> {
            try {
                FileUtil.moveFile(file.getName(), file.getAbsolutePath(), cloudFolder.getPath());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        Platform.runLater(() -> generateHierarchy(cloudFolder));
    }

    private void setDragOver(DragEvent event) {
        if (!serverSource.isSelected() && event.getDragboard().hasFiles()) {
            event.acceptTransferModes(TransferMode.COPY_OR_MOVE);
            event.consume();
        }
    }

    private ContextMenu createContextMenuForFolder(CloudFolder cloudFolder) {
        ContextMenu contextMenu = new ContextMenu();
        MenuItem save = new MenuItem(serverSource.isSelected() ? "Save to local" : "Save to Server");
        save.setOnAction(event1 -> {
            if (serverSource.isSelected()) {
                new Thread(() -> FileUtil.saveFolder(cloudFolder)).start();
            } else {
                new Thread(() -> Connection.get().sendFolder(cloudFolder)).start();
            }
        });

        MenuItem delete = new MenuItem("Delete");
        deleteFolderListener(delete, cloudFolder);

        contextMenu.getItems().add(save);
        contextMenu.getItems().add(delete);
        return contextMenu;
    }

    private void deleteFolderListener(MenuItem delete, CloudFolder cloudFolder) {
        delete.setOnAction(event -> {
            if (serverSource.isSelected()) {
                new Thread(() -> Connection.get().deleteFile(cloudFolder.getPath())).start();
            } else {
                new Thread(() -> FileUtil.deleteFolder(cloudFolder.getPath())).start();
            }
        });
    }

    private void generateFiles(CloudFolder cloudFolder) {
        if (!cloudFolder.getCloudFiles().isEmpty()) {
            cloudFolder.getCloudFiles().forEach(file -> {
                var isCloud = FilesChecker.isServerSave(file);
                var isLocal = FilesChecker.isLocalSave(file);
                Button fileButton = createButton(file.getName(), "/images/icon/file-text2.png", isCloud, isLocal);
                ContextMenu contextMenu = createContextMenuForFile(file);
                setFilesListener(fileButton, contextMenu, file);
                foldersWrapper.getChildren().add(fileButton);
                /*fileButton.setOnMouseClicked(event -> {
                    if (event.getButton() == MouseButton.PRIMARY) {
                        hostServices.showDocument(file.getPath());
                    }
                });*/
            });
        }
    }

    private void setFilesListener(Button fileButton, ContextMenu contextMenu, CloudFile file) {
        fileButton.setOnMouseClicked(event -> {
            if (event.getButton().equals(MouseButton.SECONDARY)) {
                contextMenu.show(fileButton, event.getScreenX(), event.getScreenY());
            }
        });
        fileButton.setOnDragDetected(event -> {
            if (!serverSource.isSelected()) {
                Dragboard db = fileButton.startDragAndDrop(TransferMode.COPY_OR_MOVE);
                ClipboardContent content = new ClipboardContent();
                content.putFiles(List.of(new File(file.getPath())));
                db.setContent(content);
                event.consume();
            }
        });
    }

    private ContextMenu createContextMenuForFile(CloudFile cloudFile) {
        ContextMenu contextMenu = new ContextMenu();
        MenuItem save = new MenuItem(serverSource.isSelected() ? "Save to local" : "Save to Server");
        saveFileListener(save, contextMenu, cloudFile);
        MenuItem delete = new MenuItem("Delete");
        deleteFileListener(delete, cloudFile);
        MenuItem rename = new MenuItem("Rename");
        renameFileListener(rename, cloudFile);
        contextMenu.getItems().add(save);
        contextMenu.getItems().add(delete);
        contextMenu.getItems().add(rename);
        return contextMenu;
    }

    private void renameFileListener(MenuItem rename, CloudFile cloudFile) {
        rename.setOnAction(event -> {
            PopupControlRename popup = new PopupControlRename("rename file", cloudFile.getName());
            if (serverSource.isSelected()) {
                popup.getInputName().setOnAction(popEvent -> new Thread(() -> Connection.get()
                        .renameFile(cloudFile, popup.getInputName().getText())).start());
            } else {
                popup.getInputName().setOnAction(popEvent -> {
                    FileUtil.renameFile(cloudFile, popup.getInputName().getText());
                    onReload();
                });
            }
            showDialog(popup);
        });
    }

    private void deleteFileListener(MenuItem delete, CloudFile cloudFile) {
        delete.setOnAction(event -> {
            if (serverSource.isSelected()) {
                new Thread(() -> Connection.get().deleteFile(cloudFile.getPath())).start();
            } else {
                FileUtil.deleteFile(cloudFile.getPath());
                onReload();
            }
        });
    }

    private void saveFileListener(MenuItem save, ContextMenu contextMenu, CloudFile cloudFile) {
        save.setOnAction(event1 -> {
            if (serverSource.isSelected()) {
                new Thread(() -> Connection.get().requestFile(cloudFile.getPath())).start();
            } else {
                new Thread(() -> {
                    try {
                        startDownloading(contextMenu);
                        File file = FileUtil.getFile(cloudFile.getPath());
                        Connection.get().sendFile(file);
                        endDownloading(file.getName(), contextMenu);
                    } catch (FileNotFoundException e) {
                        throw new RuntimeException(e);
                    }
                }).start();
            }
        });
    }

    private void startDownloading(ContextMenu contextMenu) {
        Platform.runLater(() -> {
            contextMenu.getItems().forEach(x -> x.setDisable(true));
            graphicsApply(downloadsButton, "/images/icon/downloading.png", 30, 30);
            downloadsButton.getStyleClass().add("downloading");
        });
    }

    private void endDownloading(String name, ContextMenu contextMenu) {
        Platform.runLater(() -> {
            contextMenu.getItems().forEach(x -> x.setDisable(false));
            downloadsButton.getStyleClass().remove("downloading");
            graphicsApply(downloadsButton, "/images/icon/downloads.png", 30, 30);
            Indicators.removeIndicator(name);
        });
    }

    private Button createButton(String name, String iconPath, boolean isCloud, boolean isLocal) {
        if (tableView.isSelected()) {
            return tableView(name, iconPath, isCloud, isLocal);
        } else {
            return listView(name, iconPath, isCloud, isLocal);
        }
    }

    private Button tableView(String name, String iconPath, boolean isCloud, boolean isLocal) {
        var mainButton = new Button();

        var infoWrapper = new VBox();
        infoWrapper.setAlignment(Pos.CENTER);
        var imageView = createImageView(iconPath, 60, 60);
        var cloudWrapper = new VBox();
        ImageView imageCloud = createImageViewCloud(isCloud, isLocal);
        cloudWrapper.setAlignment(Pos.BOTTOM_LEFT);
        cloudWrapper.getChildren().add(imageCloud);
        infoWrapper.getChildren().addAll(cloudWrapper, imageView, new Label(name));

        mainButton.setGraphic(infoWrapper);
        mainButton.setAlignment(Pos.CENTER);
        mainButton.getStyleClass().add("doc-table-view");
        mainButton.setTooltip(new Tooltip(name));
        return mainButton;
    }

    private Button listView(String name, String iconPath, boolean isCloud, boolean isLocal) {
        var mainButton = new Button();

        var infoWrapper = new HBox();
        infoWrapper.setSpacing(10);

        Label cloudState = new Label("");
        cloudState.setGraphic(createImageViewCloud(isCloud, isLocal));

        var imageView = createImageView(iconPath, 20, 20);

        Label nameLabel = new Label(name);

        infoWrapper.getChildren().addAll(cloudState, imageView, nameLabel);

        mainButton.setGraphic(infoWrapper);
        mainButton.getStyleClass().add("doc-list-view");
        return mainButton;
    }

    private ImageView createImageViewCloud(boolean isCloud, boolean isLocal) {
        if (serverSource.isSelected() && isLocal) {
            return createImageView("/images/icon/cloud-checked.png", 20, 20);
        } else if (serverSource.isSelected() && !isLocal) {
            return createImageView("/images/icon/cloud-check.png", 20, 20);
        } else if (!serverSource.isSelected() && isCloud) {
            return createImageView("/images/icon/cloud-saved.png", 20, 20);
        } else {
            return createImageView("/images/icon/cloud.png", 20, 20);
        }
    }

    private ImageView createImageView(String iconPath, int width, int height) {
        return new ImageView(new Image(Objects.requireNonNull(getClass().getResourceAsStream(iconPath)), width, height, false, true));
    }

    @FXML
    public void backListener(MouseEvent mouseEvent) {
        if (mouseEvent.getButton().equals(MouseButton.BACK)) {
            if (currentCloudFolder.getParentCloudFolder() != null) {
                generateHierarchy(currentCloudFolder.getParentCloudFolder());
            }
        }
    }

    @FXML
    public void searchButtonListener() {
        if (!search.getText().isBlank()) {
            searchInit();
        }
    }

    public void onReload() {
        if (!isSearch) {
            if (serverSource.isSelected()) {
                Connection.get().requestStructure();
            } else {
                var folder = FileUtil.getRootFolder();
                generateHierarchy(folder);
                FilesChecker.refillLocalFiles(folder);
            }
        }
    }

    public void downloads() {
        Main.showDownloads();
    }

    public void searchInputListener(KeyEvent keyEvent) {
        if (!search.getText().isBlank() && keyEvent.getCode().getCode() == 10) {
            searchInit();
        } else if (search.getText().isBlank() && isSearch) {
            isSearch = false;
            if (serverSource.isSelected()) {
                generateHierarchy(Main.root);
            } else {
                generateHierarchy(FileUtil.getRootFolder());
            }
        }
    }

    private void searchInit() {
        foldersWrapper.getChildren().clear();
        if (serverSource.isSelected()) {
            generateFoundFiles(FilesChecker.getListServerFiles());
        } else {
            generateFoundFiles(FilesChecker.getListLocalFiles());
        }
    }

    private void generateFoundFiles(List<CloudFile> files) {
        CloudFolder cloudFolder = new CloudFolder();
        for (CloudFile file : files) {
            if (file.getName().toLowerCase().startsWith(search.getText().toLowerCase())) {
                cloudFolder.addFile(file);
            }
        }
        isSearch = true;
        generateHierarchy(cloudFolder);
    }

    public void addFilesListener(MouseEvent mouseEvent) {
        createContextMenuForAddFiles().show(addFiles, mouseEvent.getScreenX(), mouseEvent.getScreenY());
    }

    public ContextMenu createContextMenuForAddFiles() {
        ContextMenu contextMenu = new ContextMenu();
        MenuItem addFolder = new MenuItem("add folder");
        graphicsApply(addFolder, "/images/icon/folder-open.png", 20, 20);
        addFolder.setOnAction(event -> showDialog(createAddDialog("enter file name")));
        contextMenu.getItems().add(addFolder);
        return contextMenu;
    }

    private PopupControl createAddDialog(String description) {
        PopupControlRename popup = new PopupControlRename(description, "");

        popup.getInputName().setOnAction(event -> {
            addFolderAction(popup.getInputName().getText());
            popup.hide();
        });
        return popup;
    }

    private void addFolderAction(String text) {
        if (!serverSource.isSelected()) {
            FileUtil.createFolder(currentCloudFolder.getPath(), text);
            onReload();
        } else {
            File file = new File(currentCloudFolder.getPath(), text);
            new Thread(() -> Connection.get().createFolder(file.getPath())).start();
        }
    }

    public void showDialog(PopupControl popup) {
        popup.show(
                stage,
                WindowUtil.getWidthCenter(popup.getScene(), stage),
                WindowUtil.getHeightCenter(popup.getScene(), stage));
    }
}
