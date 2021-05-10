/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package server;

import entity.TicTacToeConstants;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Date;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;

/**
 *
 * @author admin
 */
public class Server extends Application implements TicTacToeConstants{
    private int sessionNo = 1;
    @Override
    public void start(Stage primaryStage) {
        TextArea ta = new TextArea();
        Scene scene = new Scene(new ScrollPane(ta), 450, 200);
        primaryStage.setTitle("TicTacToeServer");
        primaryStage.setScene(scene);
        primaryStage.show();
        
        new Thread(() -> {
            try {
                ServerSocket serverSocket = new ServerSocket(8000);
                Platform.runLater(() -> {
                    ta.appendText(new Date() + ": Server started at socket 8000\n");
                });
                
                // Ready to create a session for every 2 players
                while (true) {                    
                    Platform.runLater(() -> {
                        ta.appendText(new Date() + ": Wait for players to join session " + sessionNo + "\n");
                    });
                    
                    // Connect to player 1
                    Socket player1 = serverSocket.accept();
                    Platform.runLater(() -> {
                        ta.appendText(new Date() + ": Player 1 joined session " + sessionNo + "\n");
                        ta.appendText("Player 1's IP address: " + player1.getInetAddress().getHostAddress() + "\n");
                    });
                    
                    // Notify that the player is Player 1
                    new DataOutputStream(player1.getOutputStream()).writeInt(PLAYER1);
                    
                    // Connect to player 2
                    Socket player2 = serverSocket.accept();
                    Platform.runLater(() -> {
                        ta.appendText(new Date() + ": Player 2 joined session " + sessionNo + "\n");
                        ta.appendText("Player 2's IP address: " + player1.getInetAddress().getHostAddress() + "\n");
                    });
                    
                    // Notify that the player is Player 2
                    new DataOutputStream(player2.getOutputStream()).writeInt(PLAYER2);
                    
                    // Display this session and increment session number
                    Platform.runLater(() -> {
                        ta.appendText(new Date() + ": Start a thread for session " + sessionNo + "\n");
                    });
                    
                    // Launch a new thread for this session of 2 players
                    new Thread(new HandleASession(player1, player2)).start();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }
    
    class HandleASession implements Runnable, TicTacToeConstants{
        private Socket player1, player2;
        private char[][] cell = new char[ROW][COLUMN];
        private DataInputStream fromPlayer1, fromPlayer2;
        private DataOutputStream toPlayer1, toPlayer2;
        private boolean continueToPlay = true;
        
        public HandleASession(Socket player1, Socket player2){
            this.player1 = player1;
            this.player2 = player2;
            for(int i=0;i<ROW;i++){
                for(int j=0;j<COLUMN;j++){
                    cell[i][j] = ' ';
                }
            }
        }

        @Override
        public void run() {
            try {
                fromPlayer1 = new DataInputStream(player1.getInputStream());
                toPlayer1 = new DataOutputStream(player1.getOutputStream());
                fromPlayer2 = new DataInputStream(player2.getInputStream());
                toPlayer2 = new DataOutputStream(player2.getOutputStream());
                
                // Write anything to notify player 1 to start 
                // This is just to let player 1 know to start
                toPlayer1.writeInt(1);
                
                // Continuously serve the players and determine and report 
                // the game status to players
                while (true) {                    
                    // Reveive a move from player 1
                    int row = fromPlayer1.readInt();
                    int column = fromPlayer1.readInt();
                    cell[row][column] = 'X';
                    
                    // Check if Player 1 wins
                    if(isWon('X')){
                        toPlayer1.writeInt(PLAYER1_WON);
                        toPlayer2.writeInt(PLAYER1_WON);
                        // Send player1's selected row and column to player 2
                        sendMove(toPlayer2, row, column);
                        break;
                    }
                    // Check if all cells are filled
                    else if(isFull()){
                        toPlayer1.writeInt(DRAW);
                        toPlayer2.writeInt(DRAW);
                        sendMove(toPlayer2, row, column);
                        break;
                    }
                    else{
                        // Notify player 2 to take the turn
                        toPlayer2.writeInt(CONTINUE);
                        sendMove(toPlayer2, row, column);
                    }
                    
                    // Receive a move from Player 2
                    row = fromPlayer2.readInt();
                    column = fromPlayer2.readInt();
                    cell[row][column] = 'O';
                    
                    // Check if Player 2 wins
                    if(isWon('O')){
                        toPlayer1.writeInt(PLAYER2_WON);
                        toPlayer2.writeInt(PLAYER2_WON);
                        sendMove(toPlayer1, row, column);
                    }
                    else{
                        // Notify player 1 to take the turn
                        toPlayer1.writeInt(CONTINUE);
                        // Send player2's selected row and column to player 1
                        sendMove(toPlayer1, row, column);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        
        // Send the move to other player 
        private void sendMove(DataOutputStream out, int row, int column) throws IOException{
            out.writeInt(row);
            out.writeInt(column);
        }
        
        // Determine if the cells are full
        private boolean isFull(){
            for(int i=0;i<ROW;i++){
                for(int j=0;j<COLUMN;j++){
                    if(cell[i][j] == ' ') return false;
                }
            }
            return true;
        }
        
        // Determine if the player with the specified token wins
        private boolean isWon(char token){
            int count = 0;
            // Check all rows
            for(int i=0;i<ROW;i++){
                for(int j=0;j<COLUMN;j++){
                    if(cell[i][j] == token){
                        count++;
                    }
                    else    count = 0;
                    if(count == 5)  return true;
                }
            }
            
            // Check all columns
            count = 0;
            for(int j=0;j<COLUMN;j++){
                for(int i=0;i<ROW;i++){
                    if(cell[i][j] == token){
                        count ++;
                    }
                    else count = 0;
                    if(count == 5)  return true;
                }
            }
            
            // Check major diagonal
            count = 0;
            for(int i=0;i<ROW;i++){
                for(int j=0;j<COLUMN;j++){
                    if(cell[i][j] == token){
                        for(int k=0;k<5;k++){
                            if(cell[i+k][j+k] == token){
                                count++;
                            }
                            else{
                                count = 0;
                                break;
                            }
                            if(count == 5) return true;
                        }
                    }
                    else count = 0;
                }
            }
            
            // Check subdiagonal
            count = 0;
            for(int i=0;i<ROW;i++){
                for(int j=0;j<COLUMN;j++){
                    if(cell[i][j] == token){
                        for(int k=0;k<5;k++){
                            if(cell[i-k][j+k] == token){
                                count++;
                            }
                            else{
                                count = 0;
                                break;
                            }
                            if(count == 5) return true;
                        }
                    }
                    else count = 0;
                }
            }       
            return false;
        }
    }  
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        launch(args);
    }
    
}
