package client;

import chess.*;

public class Main {
    public static void main(String[] args) {

        int port = 8080;
        var facade = new ServerFacade(port);
        var ui = new ChessClient(facade);

        ui.run();
        var piece = new ChessPiece(ChessGame.TeamColor.WHITE, ChessPiece.PieceType.PAWN);
        System.out.println("♕ 240 Chess Client: " + piece);

    }
}