/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package client;

import entity.TicTacToeConstants;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Ellipse;
import javafx.scene.shape.Line;
import javafx.stage.Stage;

/**
 *
 * @author admin
 */
public class Client extends Application implements TicTacToeConstants{
    private boolean myTurn = false;
    private char myToken = ' ';
    private char otherToken = ' ';
    private Cell[][] cell = new Cell[ROW][COLUMN];
    private Label lblTitle = new Label();
    private Label lblStatus = new Label();
    private int rowSelected, columnSelected;
    private DataOutputStream toServer;
    private DataInputStream fromServer;
    private boolean continueToPlay = true;
    // Wait for the player to mark a cell
    private boolean waiting = true;
    private String host = "localhost";
    @Override
    public void start(Stage primaryStage) {
        GridPane pane = new GridPane();
        for(int i=0;i<ROW;i++){
            for(int j=0;j<COLUMN;j++){
                pane.add(cell[i][j] = new Cell(i, j), j, i);
            }
        }
        
        BorderPane borderPane = new BorderPane();
        borderPane.setTop(lblTitle);
        borderPane.setCenter(pane);
        borderPane.setBottom(lblStatus);
        
        Scene scene = new Scene(borderPane, 320, 350);
        primaryStage.setTitle("TicTacToeClient");
        primaryStage.setScene(scene);
        primaryStage.show();
        
        connectToServer();
    }
    
    private void connectToServer(){
        try {
            Socket socket = new Socket(host, 8000);
            fromServer = new DataInputStream(socket.getInputStream());
            toServer = new DataOutputStream(socket.getOutputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        // Control the game on a separate thread
        new Thread(() -> {
            try {
                // Get notification from the server
                int player = fromServer.readInt();
                
                if(player == PLAYER1){
                    myToken = 'X';
                    otherToken = 'O';
                    Platform.runLater(() -> {
                        lblTitle.setText("You are player 1 with token X");
                        lblStatus.setText("Waiting for player 2 to join");
                    });
                    
                    // Receive startup notification from the server
                    fromServer.readInt();   // Whatever read is ignore

                    // The other player has joined
                    Platform.runLater(() -> {
                        lblStatus.setText("Player 2 has joined. You go first");
                    });

                    myTurn = true;
                }
                
                else if(player == PLAYER2){
                    myToken = 'O';
                    otherToken = 'X';
//                    fromServer.readInt();
                    Platform.runLater(() -> {
                        lblTitle.setText("You are player 2 with token O");
                        lblStatus.setText("Waiting for player 1 to move");
                    });
                }
                
                // Continue to play
                while(continueToPlay){
                    if(player == PLAYER1){
                        waitForPlayerAction();  // Waiting for player 1 to move
                        sendMove();             // Send the move to the server
                        receiveInfoFromServer();// Receive info from the server
                    }
                    else if(player == PLAYER2){
                        receiveInfoFromServer();
                        waitForPlayerAction();
                        sendMove();
                    }
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
        }).start();
    }
    
    // Waiting for the player to mark a cell
    private void waitForPlayerAction() throws InterruptedException{
        while(waiting){
            Thread.sleep(1000);
        }
        waiting = true;
    }
    
    // Send this player's move to the server
    private void sendMove() throws IOException{
        toServer.writeInt(rowSelected);
        toServer.writeInt(columnSelected);
    }
    
    // Receive info from the server
    private void receiveInfoFromServer() throws IOException{
        // Receive game status
        int status = fromServer.readInt();
        
        if(status == PLAYER1_WON){
            continueToPlay = false;
            if(myToken == 'X'){
                Platform.runLater(() -> {
                    lblStatus.setText("You won!");
                });
            }
            else if(myToken == 'O'){
                Platform.runLater(() -> {
                    lblStatus.setText("You lose!"); 
                });
                receiveMove();
            }
        }
        
        else if(status == PLAYER2_WON){
            continueToPlay = false;
            if(myToken == 'O'){
                Platform.runLater(() -> {
                    lblStatus.setText("You won!");
                });
            }
            else if(myToken == 'X'){
                Platform.runLater(() -> {
                    lblStatus.setText("You lose!"); 
                });
                receiveMove();
            }
        }
        
        else if(status == DRAW){
            continueToPlay = false;
            Platform.runLater(() -> {
                lblStatus.setText("DRAW !!!");
            });
            if(myToken == 'O'){
                receiveMove();
            }
        }
        
        else{
            receiveMove();
            Platform.runLater(() -> {
                lblStatus.setText("Your turn");
                myTurn = true;
            });
        }
    }
    
    private void receiveMove() throws IOException{
        int row = fromServer.readInt();
        int column = fromServer.readInt();
        Platform.runLater(() -> {
            cell[row][column].setToken(otherToken);
        });
    }
    
    // An inner class for a cell 
    public class Cell extends Pane{
        private int row, column;
        private char token = ' ';
        public Cell(int row, int column){
            this.row = row;
            this.column = column;
            this.setPrefSize(2000, 2000);
            setStyle("-fx-border-color: black");
            this.setOnMouseClicked((event) -> {
                handleMouseClick();
            });
        }
        
        public char getToken(){
            return this.token;
        }
        
        public void setToken(char c){
            this.token = c;
            repaint();
        }
        
        private void repaint(){
            if(token == 'X'){
                Line line1 = new Line(10, 10, this.getWidth() - 10, this.getHeight() - 10);
                line1.endXProperty().bind(this.widthProperty().subtract(10));
                line1.endYProperty().bind(this.heightProperty().subtract(10));
                Line line2 = new Line(10, this.getHeight() - 10, this.getWidth() - 10, 10);
                line2.startYProperty().bind(this.heightProperty().subtract(10));
                line2.endXProperty().bind(this.widthProperty().subtract(10));
                
                // Add the lines to the pane 
                this.getChildren().addAll(line1, line2);
            }
            else if(token == 'O'){
                Ellipse ellipse = new Ellipse(this.getWidth() / 2, this.getHeight() / 2, this.getWidth() / 2 - 10,
                                                this.getHeight() / 2 - 10);
                ellipse.centerXProperty().bind(this.widthProperty().divide(2));
                ellipse.centerYProperty().bind(this.heightProperty().divide(2));
                ellipse.radiusXProperty().bind(this.widthProperty().divide(2).subtract(10));
                ellipse.radiusYProperty().bind(this.heightProperty().divide(2).subtract(10));
                ellipse.setStroke(Color.BLACK);
                ellipse.setFill(Color.WHITE);
                getChildren().add(ellipse); // Add the ellipse to the pane
            }
        }
        
        private void handleMouseClick(){
            if(token == ' ' && myTurn){
                setToken(myToken);
                myTurn = false;
                rowSelected = row;
                columnSelected = column;
                lblStatus.setText("Waiting for the other player to move");
                waiting = false;    // Complete a successful move
            }
        }
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        launch(args);
    }
    
}
