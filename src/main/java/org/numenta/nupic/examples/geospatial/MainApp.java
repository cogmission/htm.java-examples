package org.numenta.nupic.examples.geospatial;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javafx.application.Application;
import javafx.beans.value.ObservableValue;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ScrollPane.ScrollBarPolicy;
import javafx.scene.control.ToolBar;
import javafx.scene.effect.InnerShadow;
import javafx.scene.effect.Reflection;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import netscape.javascript.JSObject;

import com.google.maps.DirectionsApi;
import com.google.maps.GeoApiContext;
import com.google.maps.GeocodingApi;
import com.google.maps.model.DirectionsLeg;
import com.google.maps.model.DirectionsRoute;
import com.google.maps.model.DirectionsStep;
import com.google.maps.model.GeocodingResult;
import com.google.maps.model.LatLng;
import com.lynden.gmapsfx.GoogleMapView;
import com.lynden.gmapsfx.MapComponentInitializedListener;
import com.lynden.gmapsfx.javascript.event.UIEventType;
import com.lynden.gmapsfx.javascript.object.GoogleMap;
import com.lynden.gmapsfx.javascript.object.LatLong;
import com.lynden.gmapsfx.javascript.object.MVCArray;
import com.lynden.gmapsfx.javascript.object.MapOptions;
import com.lynden.gmapsfx.javascript.object.MapTypeIdEnum;
import com.lynden.gmapsfx.javascript.object.Marker;
import com.lynden.gmapsfx.javascript.object.MarkerOptions;
import com.lynden.gmapsfx.service.directions.Waypoint;
import com.lynden.gmapsfx.shapes.Polyline;
import com.lynden.gmapsfx.shapes.PolylineOptions;


/**
 * Example Application for creating and loading a GoogleMap into a JavaFX
 * application
 *
 * @author Rob Terpilowski
 */
public class MainApp extends Application implements MapComponentInitializedListener {

    protected GoogleMapView mapComponent;
    protected GoogleMap map;
    
    private ScrollPane directionsPane;
    private VBox directionsBox;

    private Button btnZoomIn;
    private Button btnZoomOut;
    private Label lblZoomSetting;
    private Label lblCenterSettingLat;
    private Label lblCenterSettingLng;
    private Label lblClickSettingLat;
    private Label lblClickSettingLng;
    private ComboBox<MapTypeIdEnum> mapTypeCombo;
	
	private MarkerOptions markerOptions2;
	private Marker myMarker2;
	
	private Stage primeStage;
	private double dragAnchorX;
	private double dragAnchorY;
	
	private ToolBar toolBar;
	
	private List<Waypoint> route = new ArrayList<>();
	
	private EventHandler<MouseEvent> resizer;
    
	
    @Override
    public void start(final Stage primaryStage) throws Exception {
        mapComponent = new GoogleMapView();
        mapComponent.addMapInializedListener(this);
        mapComponent.setStyle("-fx-border-radius: 0 0 5 5; -fx-background-radius: 0 0 5 5; -fx-border-color: rgb(117,117,117); -fx-background-color: rgb(53, 53, 53);");
        
        directionsBox = new VBox();
        directionsBox.setPadding(new Insets(5,5,5,5));
        directionsPane = new ScrollPane(directionsBox);
        directionsPane.setHbarPolicy(ScrollBarPolicy.NEVER);
        directionsPane.setVbarPolicy(ScrollBarPolicy.AS_NEEDED);
        directionsPane.setPrefWidth(200);
        directionsPane.setStyle("-fx-background-fill: rgb(255,255,255);");
        directionsBox.maxWidthProperty().bind(directionsPane.widthProperty().subtract(10));
        addDirectionsBulletin("Walking directions are in beta. Use caution – This route may be missing sidewalks or pedestrian paths.");
        
        Pane window = new Pane();
        window.setStyle("-fx-background-radius: 10; -fx-border-radius: 10; -fx-background-color: rgba(0, 255, 0, 0.4);");
        window.setPrefWidth(1000);
        window.setPrefHeight(780);
        
        LogoPane logo = getLogo();
        logo.setLayoutY(25);
        
        Text t = new Text("GeoSpatial Demo");
        t.setCache(true);
        t.setFill(Color.BLACK);
        t.setFont(Font.font(null, FontWeight.BOLD, 60));
         
        Reflection r = new Reflection();
        r.setFraction(0.7f);
        r.setBottomOpacity(0.0d);
        r.setTopOpacity(0.6d);
         
        t.setEffect(r);
        addStageDragHandling(t, window);
         
        window.getChildren().addAll(logo, t);
        
        Pane background = new Pane();
        background.setStyle("-fx-background-radius: 10 10 10 10; -fx-background-insets: 10 10 10 10; -fx-border-radius: 10 10 10 10; -fx-background-color: rgb(53, 53, 53);");
         
        BorderPane bp = new BorderPane();
        
        toolBar = new ToolBar();
        //toolBar.setStyle("");
        
        logo.layoutXProperty().bind(window.widthProperty().divide(5).subtract(logo.getWidth() / 2 + 20));
        t.layoutXProperty().bind(logo.layoutXProperty().add(logo.getWidth() + 150));
        t.layoutYProperty().bind(logo.layoutYProperty().add(logo.getHeight() + 40));
        
        btnZoomIn = new Button("+");
        btnZoomIn.setOnAction(e -> {
            map.zoomProperty().set(map.getZoom() + 1);
        });
        btnZoomIn.setDisable(true);
        btnZoomIn.setMaxSize(45, 20);
        btnZoomIn.setMinSize(45, 20);
        btnZoomIn.setPadding(new Insets(2, 2, 2, 2));
        
        btnZoomOut = new Button("-");
        btnZoomOut.setOnAction(e -> {
            map.zoomProperty().set(map.getZoom() - 1);
        });
        btnZoomOut.setDisable(true);
        btnZoomOut.setMaxSize(45, 20);
        btnZoomOut.setMinSize(45, 20);
        btnZoomOut.setPadding(new Insets(0, 0, 2, 0));
        
        lblZoomSetting = new Label();
        
        Label lblZoom = new Label("Zoom Level: ");
        lblZoom.setTextFill(Color.WHITE);
        
        HBox zoomBox = new HBox(5);
        zoomBox.getChildren().addAll(lblZoom, lblZoomSetting);
        HBox zoomButtons = new HBox(5);
        zoomButtons.getChildren().addAll(btnZoomIn, btnZoomOut);
        VBox zoomControls = new VBox(5);
        zoomControls.getChildren().addAll(zoomBox, zoomButtons);
        zoomControls.setBackground(
            new Background(
                new BackgroundFill(Color.rgb(70, 70, 70), new CornerRadii(5), null)));
        zoomControls.setPadding(new Insets(5, 5, 5, 5));
        InnerShadow innerShadow = new InnerShadow();
        innerShadow.setRadius(5d);
        innerShadow.setOffsetX(2);
        innerShadow.setOffsetY(2);
        zoomControls.setEffect(innerShadow);
                
        lblCenterSettingLat = new Label();
        lblCenterSettingLng = new Label();
        lblClickSettingLat = new Label();
        lblClickSettingLng = new Label();
        
        VBox mapTypeControls = new VBox(5);
        Label mapTypeLabel = new Label("Map Type");
        mapTypeLabel.setTextFill(Color.WHITE);
        mapTypeCombo = new ComboBox<>();
        mapTypeCombo.setOnAction( e -> {
           map.setMapType(mapTypeCombo.getSelectionModel().getSelectedItem());
        });
        mapTypeCombo.setDisable(true);
        mapTypeControls.getChildren().addAll(mapTypeLabel, mapTypeCombo);
        mapTypeControls.setEffect(innerShadow);
        mapTypeControls.setBackground(
            new Background(
                new BackgroundFill(Color.rgb(70, 70, 70), new CornerRadii(5), null)));
        mapTypeControls.setPadding(new Insets(5, 5, 5, 5));
        
        VBox mapCenterControls = new VBox(3);
        Label lblCenter = new Label("Center");
		lblCenter.setTextFill(Color.WHITE);
        mapCenterControls.getChildren().addAll(lblCenter, lblCenterSettingLat, lblCenterSettingLng);
        mapCenterControls.setEffect(innerShadow);
        mapCenterControls.setBackground(
            new Background(
                new BackgroundFill(Color.rgb(70, 70, 70), new CornerRadii(5), null)));
        mapCenterControls.setPadding(new Insets(3, 5, 0, 5));
        lblCenterSettingLat.setPadding(new Insets(0, 0, 0, 15));
        lblCenterSettingLat.setFont(new Font("Arial", 10));
        lblCenterSettingLng.setPadding(new Insets(0, 0, 0, 15));
        lblCenterSettingLng.setFont(new Font("Arial", 10));
        
        VBox mapClickControls = new VBox(3);
        Label lblClick = new Label("Last Click");
        lblClick.setTextFill(Color.WHITE);
        mapClickControls.getChildren().addAll(lblClick, lblClickSettingLat, lblClickSettingLng);
        mapClickControls.setEffect(innerShadow);
        mapClickControls.setBackground(
            new Background(
                new BackgroundFill(Color.rgb(70, 70, 70), new CornerRadii(5), null)));
        mapClickControls.setPadding(new Insets(3, 5, 0, 5));
        lblClickSettingLat.setPadding(new Insets(0, 0, 0, 15));
        lblClickSettingLat.setFont(new Font("Arial", 10));
        lblClickSettingLng.setPadding(new Insets(0, 0, 0, 15));
        lblClickSettingLng.setFont(new Font("Arial", 10));
        
        HBox spacer = new HBox();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        toolBar.getItems().addAll(zoomControls, mapTypeControls, spacer, mapCenterControls, mapClickControls);

        bp.setTop(toolBar);
        bp.setCenter(mapComponent);
        bp.setRight(directionsPane);
        bp.setLayoutX(10);
        bp.setLayoutY(10);
        
        background.setLayoutX(0);
        background.setLayoutY(120);
        window.boundsInLocalProperty().addListener((v, o, n) -> {
            background.setPrefWidth(n.getWidth() - 20);
            background.setPrefHeight(n.getHeight() - 200);
            mapComponent.setPrefWidth(Math.max(600, Math.max(0, n.getWidth() - 20 - 200)));//n.getWidth() - 20));
            mapComponent.setPrefHeight(n.getHeight() - 210);
            toolBar.setPrefWidth(Math.max(800, n.getWidth() - 20));
            toolBar.setPrefHeight(60);
        });

        background.getChildren().add(bp);
        background.setPadding(new Insets(10, 10, 10, 10));
        window.getChildren().add(background);
        Scene scene = new Scene(window, Color.WHITE);
        
        String css = getClass().getResource("mapstyle.css").toExternalForm();
        scene.getStylesheets().clear();
        scene.getStylesheets().add(css);
        
        final Stage stage = new Stage(StageStyle.TRANSPARENT);
        stage.initOwner(primaryStage);
        stage.setScene(scene);
        
        primeStage = stage;
        addStageDragHandling(logo.getLogoDot(), window);
        
        stage.show();
    }
    
    private void addDirectionsBulletin(String s) {
        Label l = new Label(s);
        l.setBackground(new Background(
            new BackgroundFill(
                Color.color(
                    Color.YELLOW.getRed(), 
                    Color.YELLOW.getGreen(), 
                    Color.YELLOW.getBlue(), 0.4d),
                new CornerRadii(5), 
                null)));
        l.setWrapText(true);
        l.setPrefWidth(200);
        l.setPadding(new Insets(5,5,5,5));
        directionsBox.getChildren().add(l);
    }
    
    private void addDirectionsLocation(Waypoint wp) {
        GeocodingResult[] results = null;
        try {
            results = GeocodingApi.newRequest(context)
                .latlng(new LatLng(wp.getLatLong().getLatitude(), wp.getLatLong().getLongitude())).await();
        }catch(Exception e) {
            e.printStackTrace();
        }
        
        Label l = new Label(results[0].formattedAddress, new ImageView(getMarkerIconPath()));
        l.setBackground(new Background(
            new BackgroundFill(Color.LIGHTGRAY,
                new CornerRadii(5), 
                null)));
        l.setWrapText(true);
        l.setPrefWidth(200);
        l.setPadding(new Insets(5,5,5,5));
        l.setBorder(new Border(new BorderStroke(Color.GRAY, new BorderStrokeStyle(null, null, null, 10, 0, null), null, null)));
        directionsBox.getChildren().add(l);
    }
    
    private void addDirectionsLeg(DirectionsRoute route) {
        try {
            GeocodingResult[] results = GeocodingApi.newRequest(context)
                .latlng(new LatLng(point.getLatitude(), point.getLongitude())).await();
            addDirectionsBulletin(results[0].formattedAddress);
        }catch(Exception e) {
            e.printStackTrace();
        }
    }
    
    public LogoPane getLogo() {
        LogoPane logoPane = new LogoPane();
        logoPane.setStyle("-fx-background-color: transparent;");
        logoPane.setPrefWidth(100);
        
        Button roundButton = new Button();
        String styleString = "-fx-background-radius: 5em; " +
                        "-fx-min-width: 20px; " +
                        "-fx-min-height: 20px; " +
                        "-fx-max-width: 20px; " +
                        "-fx-max-height: 20px;";
        roundButton.setStyle(styleString + " -fx-background-color: rgb(18, 142, 213);");
        logoPane.setLogoDot(roundButton);
        Button roundButton2 = new Button();
        styleString = "-fx-background-radius: 5em; " +
                        "-fx-min-width: 20px; " +
                        "-fx-min-height: 20px; " +
                        "-fx-max-width: 20px; " +
                        "-fx-max-height: 20px;";
        roundButton2.setStyle(styleString + " -fx-background-color: black;");
        Button roundButton3 = new Button();
        styleString = "-fx-background-radius: 5em; " +
                        "-fx-min-width: 20px; " +
                        "-fx-min-height: 20px; " +
                        "-fx-max-width: 20px; " +
                        "-fx-max-height: 20px;";
        roundButton3.setStyle(styleString + " -fx-background-color: black;");
        Button roundButton4 = new Button();
        styleString = "-fx-background-radius: 5em; " +
                        "-fx-min-width: 20px; " +
                        "-fx-min-height: 20px; " +
                        "-fx-max-width: 20px; " +
                        "-fx-max-height: 20px;";
        roundButton4.setStyle(styleString + " -fx-background-color: black;");
        Button roundButton5 = new Button();
        styleString = "-fx-background-radius: 5em; " +
                        "-fx-min-width: 20px; " +
                        "-fx-min-height: 20px; " +
                        "-fx-max-width: 20px; " +
                        "-fx-max-height: 20px;";
        roundButton5.setStyle(styleString + " -fx-background-color: black;");
        Button roundButton6 = new Button();
        styleString = "-fx-background-radius: 5em; " +
                        "-fx-min-width: 20px; " +
                        "-fx-min-height: 20px; " +
                        "-fx-max-width: 20px; " +
                        "-fx-max-height: 20px;";
        roundButton6.setStyle(styleString + " -fx-background-color: black;");
        
        Line l = new Line();
        l.setFill(Color.BLACK);
        l.setStrokeWidth(2);
        
        Line l2 = new Line();
        l2.setFill(Color.BLACK);
        l2.setStrokeWidth(2);
        
        Line l3 = new Line();
        l3.setFill(Color.BLACK);
        l3.setStrokeWidth(2);
        
        Line l4 = new Line();
        l4.setFill(Color.BLACK);
        l4.setStrokeWidth(2);
        
        Line l5 = new Line();
        l5.setFill(Color.BLACK);
        l5.setStrokeWidth(2);
        
        Line l6 = new Line();
        l6.setFill(Color.BLACK);
        l6.setStrokeWidth(2);
        
        
        logoPane.layoutBoundsProperty().addListener((v, o, n) -> {
           double x = n.getWidth() / 2 - 10;
           
           roundButton.setLayoutX(x);
           roundButton.toFront();
           l.setStartX(roundButton.getLayoutX() + 10);
           l.setStartY(roundButton.getLayoutY() + 10);
           l2.setStartX(roundButton.getLayoutX() + 10);
           l2.setStartY(roundButton.getLayoutY() + 10);
           l.toBack();
           l2.toBack();
           roundButton2.setLayoutX(x - 20);
           roundButton2.setLayoutY(35);
           l.setEndX(roundButton2.getLayoutX() + 10);
           l.setEndY(roundButton2.getLayoutY() + 10);
           l3.setStartX(roundButton2.getLayoutX() + 10);
           l3.setStartY(roundButton2.getLayoutY() + 10);
           l4.setStartX(roundButton2.getLayoutX() + 10);
           l4.setStartY(roundButton2.getLayoutY() + 10);
           roundButton3.setLayoutX(x + 20);
           roundButton3.setLayoutY(35);
           l2.setEndX(roundButton3.getLayoutX() + 10);
           l2.setEndY(roundButton3.getLayoutY() + 10);
           l5.setStartX(roundButton3.getLayoutX() + 10);
           l5.setStartY(roundButton3.getLayoutY() + 10);
           l6.setStartX(roundButton3.getLayoutX() + 10);
           l6.setStartY(roundButton3.getLayoutY() + 10);
           roundButton4.setLayoutX(x - 40);
           roundButton4.setLayoutY(70);
           l3.setEndX(roundButton4.getLayoutX() + 10);
           l3.setEndY(roundButton4.getLayoutY() + 10);
           roundButton5.setLayoutX(x);
           roundButton5.setLayoutY(70);
           l4.setEndX(roundButton5.getLayoutX() + 10);
           l4.setEndY(roundButton5.getLayoutY() + 10);
           l5.setEndX(roundButton5.getLayoutX() + 10);
           l5.setEndY(roundButton5.getLayoutY() + 10);
           roundButton6.setLayoutX(x + 40);
           roundButton6.setLayoutY(70);
           l6.setEndX(roundButton6.getLayoutX() + 10);
           l6.setEndY(roundButton6.getLayoutY() + 10);
        });
        
        logoPane.getChildren().add(roundButton);
        logoPane.getChildren().add(roundButton2);
        logoPane.getChildren().add(roundButton3);
        logoPane.getChildren().add(roundButton4);
        logoPane.getChildren().add(roundButton5);
        logoPane.getChildren().add(roundButton6);
        logoPane.getChildren().add(l);
        logoPane.getChildren().add(l2);
        logoPane.getChildren().add(l3);
        logoPane.getChildren().add(l4);
        logoPane.getChildren().add(l5);
        logoPane.getChildren().add(l6);
        
        return logoPane;
    }
    
    @Override
    public void mapInitialized() {
        //Once the map has been loaded by the Webview, initialize the map details.
        LatLong center = new LatLong(41.91073, -87.71332000000001);
        mapComponent.addMapReadyListener(() -> {
            // This call will fail unless the map is completely ready.
            checkCenter(center);
        });
        
        MapOptions options = new MapOptions();
        options.center(center)
                .mapMarker(true)
                .zoom(15)
                .overviewMapControl(false)
                .panControl(false)
                .rotateControl(false)
                .scaleControl(false)
                .streetViewControl(false)
                .zoomControl(false)
                .mapType(MapTypeIdEnum.ROADMAP);

        map = mapComponent.createMap(options);
        
//        InfoWindowOptions infoOptions = new InfoWindowOptions();
//        infoOptions.content("<h2>Here's an info window</h2><h3>with some info</h3>")
//                .position(center);
//
//        InfoWindow window = new InfoWindow(infoOptions);
//        window.open(map, myMarker);
        
        
//        map.fitBounds(new LatLongBounds(new LatLong(30, 120), center));
//        System.out.println("Bounds : " + map.getBounds());

//        map.setCenter(new LatLong(47.606189, -122.335842));
        lblCenterSettingLat.setText(map.getCenter().toString().substring(0, map.getCenter().toString().indexOf("lng:") - 1));
        lblCenterSettingLng.setText(map.getCenter().toString().substring(map.getCenter().toString().indexOf("lng:"), map.getCenter().toString().length()));
        lblCenterSettingLat.setTextFill(Color.WHITE);
        lblCenterSettingLng.setTextFill(Color.WHITE);
        map.centerProperty().addListener((ObservableValue<? extends LatLong> obs, LatLong o, LatLong n) -> {
            lblCenterSettingLat.setText(n.toString().substring(0, n.toString().indexOf("lng:") - 1));
            lblCenterSettingLng.setText(n.toString().substring(n.toString().indexOf("lng:"), n.toString().length()));
        });

        lblZoomSetting.setText(Integer.toString(map.getZoom()));
        lblZoomSetting.setTextFill(Color.WHITE);
        map.zoomProperty().addListener((ObservableValue<? extends Number> obs, Number o, Number n) -> {
            lblZoomSetting.setText(n.toString());
        });
        
        lblClickSettingLat.setTextFill(Color.WHITE);
        lblClickSettingLng.setTextFill(Color.WHITE);

        map.addUIEventHandler(UIEventType.click, (JSObject obj) -> {
            LatLong ll = new LatLong((JSObject) obj.getMember("latLng"));
            lblClickSettingLat.setText(ll.toString().substring(0, ll.toString().indexOf("lng:") - 1));
            lblClickSettingLng.setText(ll.toString().substring(ll.toString().indexOf("lng:"), ll.toString().length()));
            
            addRouteMarker(new LatLong(ll.getLatitude(), ll.getLongitude()));
        });

        btnZoomIn.setDisable(false);
        btnZoomOut.setDisable(false);
        mapTypeCombo.setDisable(false);
        
        mapTypeCombo.getItems().addAll(MapTypeIdEnum.ALL);
        mapTypeCombo.setValue(MapTypeIdEnum.ROADMAP);

//        LatLong centreC = new LatLong(47.545481, -121.87384);
//        CircleOptions cOpts = new CircleOptions()
//                .center(centreC)
//                .radius(5000)
//                .strokeColor("green")
//                .strokeWeight(2)
//                .fillColor("orange")
//                .fillOpacity(0.3);
//
//        Circle c = new Circle(cOpts);
//        map.addMapShape(c);
//        map.addUIEventHandler(c, UIEventType.click, (JSObject obj) -> {
//            c.setEditable(!c.getEditable());
//        });
//
//        LatLongBounds llb = new LatLongBounds(new LatLong(47.533893, -122.89856), new LatLong(47.580694, -122.80312));
//        RectangleOptions rOpts = new RectangleOptions()
//                .bounds(llb)
//                .strokeColor("black")
//                .strokeWeight(2)
//                .fillColor("null");
//
//        Rectangle rt = new Rectangle(rOpts);
//        map.addMapShape(rt);
//
//        LatLong arcC = new LatLong(47.227029, -121.81641);
//        double startBearing = 0;
//        double endBearing = 30;
//        double radius = 30000;
//
//        MVCArray path = ArcBuilder.buildArcPoints(arcC, startBearing, endBearing, radius);
//        path.push(arcC);
//
//        Polygon arc = new Polygon(new PolygonOptions()
//                .paths(path)
//                .strokeColor("blue")
//                .fillColor("lightBlue")
//                .fillOpacity(0.3)
//                .strokeWeight(2)
//                .editable(false));
//
//        map.addMapShape(arc);
//        map.addUIEventHandler(arc, UIEventType.click, (JSObject obj) -> {
//            arc.setEditable(!arc.getEditable());
//        });
        
//        MaxZoomService mzs = new MaxZoomService();
//        mzs.getMaxZoomAtLatLng(lle, new MaxZoomServiceCallback() {
//            @Override
//            public void maxZoomReceived(MaxZoomResult result) {
//                System.out.println("Max Zoom Status: " + result.getStatus());
//                System.out.println("Max Zoom: " + result.getMaxZoom());
//            }
//        });
        
        
    }
    
    private static final String MKBL = "markers/blue_Marker";
    private static final String MKGR = "markers/blue_Marker";
    private static final String MKRD = "markers/blue_Marker";
    private static final String MKYW = "markers/blue_Marker";
    
    private char chr = 'A';
    private GeoApiContext context = new GeoApiContext().setApiKey("AIzaSyCAr_oliMz9_4SLg2fx1OWSKT0WOgZtiQU");
    private String currentMarkerPath;
    
    private enum MarkerIconPath { BLUE, GREEN, RED, YELLOW };
    private String getMarkerIconPath() {
        return currentMarkerPath;
    }
    
    private String getNextMarkerIconPath(MarkerIconPath mip) {
        chr = chr == 'Z' ? 'A' - 1 : chr;
        String pathPart = mip == MarkerIconPath.BLUE ? MKBL : mip == MarkerIconPath.GREEN ? MKGR : mip == MarkerIconPath.RED ? MKRD : MKYW;
        return currentMarkerPath = getClass().getResource(pathPart.concat(Character.toString(chr++)).concat(".png")).toString();
    }
    
    private void addRouteMarker(LatLong point) {
        String iconPath = getNextMarkerIconPath(MarkerIconPath.GREEN);
        MarkerOptions opts = new MarkerOptions()
            .position(point)
            .title("Waypoint " + route.size() + 1)
            .icon(iconPath)
            .visible(true);

        Waypoint waypoint = new Waypoint(point, new Marker(opts));
//        System.out.println("icon = " + waypoint.getMarker().getJSObject().getMember("icon");
        map.addMarker(waypoint.getMarker());
        route.add(waypoint);
        List<Waypoint> workingRoute = new ArrayList<>(route);
        
        if(route.size() > 1) {
            LatLong[] ary = new LatLong[route.size()];
            int i = 0;
            for(Waypoint wp : route) {
                ary[i++] = wp.getLatLong(); 
            }
            MVCArray mvc = new MVCArray(ary);
  
            PolylineOptions polyOpts = new PolylineOptions()
                  .path(mvc)
                  .strokeColor("red")
                  .strokeWeight(2);

            Polyline poly = new Polyline(polyOpts);
            map.addMapShape(poly);
            map.addUIEventHandler(poly, UIEventType.click, (JSObject obj) -> {
                LatLong ll = new LatLong((JSObject) obj.getMember("latLng"));
                System.out.println("You clicked the line at LatLong: lat: " + ll.getLatitude() + " lng: " + ll.getLongitude());
            });
            
            LatLong origin = workingRoute.remove(0).getLatLong();
            LatLong dest = workingRoute.remove(workingRoute.size() - 1).getLatLong();
            String[] waypoints = new String[workingRoute.size()];
            List<LatLng> response = null;
            i = 0;
            for(Waypoint w : workingRoute) {
                waypoints[i++] = new LatLng(w.getLatLong().getLatitude(), w.getLatLong().getLongitude()).toString();
            }
            
            try {
                DirectionsRoute[] routes = DirectionsApi.newRequest(context)
                    .origin(new LatLng(origin.getLatitude(), origin.getLongitude()))
                    .destination(new LatLng(dest.getLatitude(), dest.getLongitude()))
                    .waypoints(waypoints)
                    .await();
                
                Arrays.stream(routes).flatMap(r -> Arrays.stream(r.legs)).flatMap(l -> Arrays.stream(l.steps)).forEach(step ->  {
                    
                });
                for(DirectionsRoute dr : routes) {
                    response = dr.overviewPolyline.decodePath();
                    System.out.println("got latlng = " + response);
                    DirectionsLeg[] legs = dr.legs;
                    int i = 0;
                    for(DirectionsLeg leg : legs) {
                        DirectionsStep[] steps = leg.steps;
                        
                    }
                }
            } catch(Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            
            ary = new LatLong[response.size()];
            i = 0;
            for(LatLng wp : response) {
                ary[i++] = new LatLong(wp.lat, wp.lng); 
            }
            mvc = new MVCArray(ary);
  
            polyOpts = new PolylineOptions()
                  .path(mvc)
                  .strokeColor("lightBlue")
                  .strokeWeight(2)
                  .strokeOpacity(0.5);

            poly = new Polyline(polyOpts);
            map.addMapShape(poly);
            map.addUIEventHandler(poly, UIEventType.click, (JSObject obj) -> {
                LatLong ll = new LatLong((JSObject) obj.getMember("latLng"));
                System.out.println("You clicked the line at LatLong: lat: " + ll.getLatitude() + " lng: " + ll.getLongitude());
            });
            
            
//            LatLong first = null;
//            DirectionsRequest rr = new DirectionsRequest(
//                first = route.remove(0).getLatLong(), 
//                route.remove(route.size() - 1).getLatLong(), 
//                route, 
//                TravelModeEnum.WALKING);
//            System.out.println("route size = " + route.size());
//            DirectionsService service = new DirectionsService();
//            DirectionsServiceCallback callback = new DirectionsServiceCallback() {
//                @Override
//                public void routeReceived(List<LatLong> route, RouteStatus status) {
//                    
//                }
//            };
//             
//            map.setHeading(first.getBearing(point));
//            service.getRoute(rr, callback);
        }
        
        addDirectionsLocation(waypoint);
        
    }
	
    private void hideMarker() {
//		System.out.println("deleteMarker");
		
		boolean visible = myMarker2.getVisible();
		
		//System.out.println("Marker was visible? " + visible);
		
		myMarker2.setVisible(! visible);

//				markerOptions2.visible(Boolean.FALSE);
//				myMarker2.setOptions(markerOptions2);
//		System.out.println("deleteMarker - made invisible?");
	}
	
	private void deleteMarker() {
		//System.out.println("Marker was removed?");
		map.removeMarker(myMarker2);
	}
	
    private void checkCenter(LatLong center) {
        System.out.println("Testing fromLatLngToPoint using: " + center);
        Point2D p = map.fromLatLngToPoint(center);
        System.out.println("Testing fromLatLngToPoint result: " + p);
        System.out.println("Testing fromLatLngToPoint expected: " + mapComponent.getWidth()/2 + ", " + mapComponent.getHeight()/2);
    }
    
    private void addStageDragHandling(Node node, Node window) {
        node.setOnMousePressed((MouseEvent me) -> { 
            dragAnchorX = me.getScreenX() - primeStage.getX(); 
            dragAnchorY = me.getScreenY() - primeStage.getY(); 
        });
        
        node.setOnMouseDragged((MouseEvent me) -> {
            primeStage.setX(me.getScreenX() - dragAnchorX);
            primeStage.setY(me.getScreenY() - dragAnchorY);
        });
        
        final double factor = node instanceof Text ? .15 : .75;
        
        node.setOnMouseEntered((MouseEvent me) -> {
            node.setScaleX(node.getScaleX() + factor);
            node.setScaleY(node.getScaleY() + factor);
            window.getScene().setCursor(Cursor.CLOSED_HAND);
        });
        
        node.setOnMouseExited((MouseEvent me) -> {
            node.setScaleX(node.getScaleX() - factor);
            node.setScaleY(node.getScaleY() - factor);
            window.getScene().setCursor(Cursor.DEFAULT);
        });
        
        if(resizer == null) {
            window.setOnMousePressed((MouseEvent me) -> {
                Point2D topLeft = toolBar.localToScreen(toolBar.getLayoutX(), toolBar.getLayoutY());
                if(me.getScreenY() < topLeft.getY() - 10) return;
                
                dragAnchorX = me.getScreenX(); 
                dragAnchorY = me.getScreenY(); 
            });
            
            window.setOnMouseMoved((MouseEvent me) -> {
                Point2D topRight = toolBar.localToScreen(toolBar.getLayoutBounds().getMinX() + toolBar.getLayoutBounds().getWidth(), toolBar.getLayoutY());
                Point2D botRight = new Point2D(topRight.getX(), primeStage.getY() + primeStage.getHeight());
                if(me.getX() > 40 && me.getX() < toolBar.getLayoutBounds().getMaxX() - 40 && me.getScreenY() > botRight.getY() - 20) {
                    window.getScene().setCursor(Cursor.S_RESIZE);
                }else if(me.getX() < 20) {
                    if(me.getScreenY() < topRight.getY() + 20 && me.getScreenY() > topRight.getY() - 20) {
                        window.getScene().setCursor(Cursor.NW_RESIZE);
                    }else if(me.getScreenY() > botRight.getY() - 20) {
                        window.getScene().setCursor(Cursor.SW_RESIZE);
                    }else if(me.getScreenY() < botRight.getY() - 20 && me.getScreenY() > topRight.getY() + 20) {
                        window.getScene().setCursor(Cursor.W_RESIZE);
                    }
                }else if(me.getScreenX() > botRight.getX() - 20) {
                    if(me.getScreenY() < topRight.getY() + 20 && me.getScreenY() > topRight.getY() - 20) {
                        window.getScene().setCursor(Cursor.NE_RESIZE);
                    }else if(me.getScreenY() > botRight.getY() - 20) {
                        window.getScene().setCursor(Cursor.SE_RESIZE);
                    }else if(me.getScreenY() < botRight.getY() - 20 && me.getScreenY() > topRight.getY() + 20) {
                        window.getScene().setCursor(Cursor.E_RESIZE);
                    }
                }else{
                    window.getScene().setCursor(Cursor.DEFAULT);
                }
            });
            
            window.setOnMouseExited((MouseEvent me) -> {
               window.getScene().setCursor(Cursor.DEFAULT); 
            });
            
            window.setOnMouseDragged(resizer = (MouseEvent me) -> {
                Point2D topLeft = toolBar.localToScreen(toolBar.getLayoutX(), toolBar.getLayoutY());
                Point2D topRight = toolBar.localToScreen(toolBar.getLayoutBounds().getMinX() + toolBar.getLayoutBounds().getWidth(), toolBar.getLayoutY());
                Point2D botRight = new Point2D(topRight.getX(), primeStage.getY() + primeStage.getHeight());
                
                if(me.getScreenY() < topLeft.getY() - 10) return;
                
                if(me.getX() > 40 && me.getX() < toolBar.getLayoutBounds().getMaxX() - 40 && me.getScreenY() > botRight.getY() - 20) {
                    if(dragAnchorY < me.getScreenY()) {
                        growBottom(me.getScreenY() - dragAnchorY);
                    }else{
                        shrinkBottom(dragAnchorY - me.getScreenY());
                    }
                }else if(me.getScreenX() < dragAnchorX) {
                    if(me.getX() < 20) {
                        growLeft(dragAnchorX - me.getScreenX());
                        if(me.getScreenY() < topLeft.getY() + 20 && me.getScreenY() > topLeft.getY() - 20) {
                            growTop(dragAnchorY - me.getScreenY());
                        }else if(me.getScreenY() > botRight.getY() - 20) {
                            growBottom(me.getScreenY() - dragAnchorY);
                        }
                    }else if(me.getScreenX() > botRight.getX() - 20) {
                        shrinkRight(dragAnchorX - me.getScreenX());
                        if(me.getScreenY() < topRight.getY() + 20 && me.getScreenY() > topRight.getY() - 20) {
                            shrinkTop(me.getScreenY() - dragAnchorY);
                        }else if(me.getScreenY() > botRight.getY() - 20) {
                            shrinkBottom(dragAnchorY - me.getScreenY());
                        }
                    }
                }else{
                    if(me.getX() < 20) {
                        shrinkLeft(me.getScreenX() - dragAnchorX);
                        if(me.getScreenY() < topLeft.getY() + 20 && me.getScreenY() > topLeft.getY() - 20) {
                            shrinkTop(me.getScreenY() - dragAnchorY);
                        }else if(me.getScreenY() > botRight.getY() - 20) {
                            shrinkBottom(dragAnchorY - me.getScreenY());
                        }
                    }else if(me.getScreenX() > botRight.getX() - 20) {
                        growRight(me.getScreenX() - dragAnchorX);
                        if(me.getScreenY() < topRight.getY() + 20 && me.getScreenY() > topRight.getY() - 20) {
                            growTop(dragAnchorY - me.getScreenY());
                        }else if(me.getScreenY() > botRight.getY() - 20) {
                            growBottom(me.getScreenY() - dragAnchorY);
                        }
                    }
                }
                dragAnchorX = me.getScreenX();
                dragAnchorY = me.getScreenY();
            });
        }
    }
    
    private void growTop(double amount) {
        primeStage.setY(primeStage.getY() - amount);
        primeStage.setHeight(primeStage.getHeight() + amount);
    }
    
    private void shrinkTop(double amount) {
        primeStage.setY(primeStage.getY() + amount);
        primeStage.setHeight(primeStage.getHeight() - amount);
    }
    
    private void growLeft(double amount) {
        primeStage.setX(primeStage.getX() - amount);
        primeStage.setWidth(primeStage.getWidth() + amount);
    }
    
    private void shrinkLeft(double amount) {
        primeStage.setX(primeStage.getX() + amount);
        primeStage.setWidth(primeStage.getWidth() - amount);
    }
    
    private void growRight(double amount) {
        System.out.println("growRight " + amount);
        primeStage.setWidth(primeStage.getWidth() + amount);
    }
    
    private void shrinkRight(double amount) {
        primeStage.setWidth(primeStage.getWidth() - amount);
    }
    
    private void growBottom(double amount) {
        primeStage.setHeight(primeStage.getHeight() + amount);
    }
    
    private void shrinkBottom(double amount) {
        primeStage.setHeight(primeStage.getHeight() - amount);
    }
    
    class LogoPane extends Pane {
        Button logoDot;
        public void setLogoDot(Button b) { this.logoDot = b; }
        public Button getLogoDot() { return logoDot; }
    }
    
    /**
     * The main() method is ignored in correctly deployed JavaFX application.
     * main() serves only as fallback in case the application can not be
     * launched through deployment artifacts, e.g., in IDEs with limited FX
     * support. NetBeans ignores main().
     *
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        System.setProperty("java.net.useSystemProxies", "true");
        launch(args);
    }

}
