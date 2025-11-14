package client;

import chess.ChessBoard;
import chess.ChessGame;
import model.*;
import ui.EscapeSequences;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class ChessClient {
    private final ServerFacade facade;
    private final Scanner scanner = new Scanner(System.in);

    private String authToken = null;
    private String username = null;

    private final List<GameData> cachedGames = new ArrayList<>();

    public ChessClient(ServerFacade facade) {
        this.facade = facade;
    }

    public void run() {
        System.out.println("Welcome to CS240 Chess!");
        System.out.println("Type 'help' to see available commands.\n");

        while (true) {
            if (authToken == null) {
                preloginLoop();
            } else {
                postloginLoop();
            }
        }
    }

    private void preloginLoop() {
        System.out.println("[NOT LOGGED IN] > ");
        String line = scanner.nextLine().trim();

        switch (line.toLowerCase()) {
            case "help", "h" -> printPreloginHelp();
            case "register", "r" -> handleRegister();
            case "login", "l" -> handleLogin();
            case "quit", "exit", "q" -> {
                System.out.println("adios!");
                System.exit(0);
            }
            case "create" -> System.out.println("under construction");
            case "list" -> System.out.println("under construction");
            case "play" -> System.out.println("under construction");
            case "observe" -> System.out.println("under construction");
            default -> System.out.println("not recognized, type 'help' for more options");
        }
    }

    private void printPreloginHelp() {
        System.out.println("""
                === Pre-Login Commands ===
                help (h)      - Show this help text
                register (r)  - Create a new account and log in
                login (l)     - Log in with an existing account
                quit (q)      - Exit chess
                """);
    }

    private void handleRegister() {
        System.out.println("==Register==");
        System.out.println("Username: ");
        String username = scanner.nextLine().trim();

        System.out.println("Password: ");
        String password = scanner.nextLine().trim();

        System.out.println("Email: ");
        String email = scanner.nextLine().trim();

        try {
            AuthData auth = facade.register(username, password, email);
            this.authToken = auth.authToken();
            this.username = auth.username();
            System.out.printf("Registered and logged in as '%s'. %n", username);
        } catch (IOException | InterruptedException e) {
            System.out.println("Error with server: " + e.getMessage());
        } catch (RuntimeException e) {
            System.out.println(cleanErrorMessage(e.getMessage()));
        }
    }

    private void handleLogin() {
        System.out.println("==Login==");
        System.out.println("Username: ");
        String username = scanner.nextLine().trim();

        System.out.println("Password: ");
        String password = scanner.nextLine().trim();

        try {
            AuthData auth = facade.login(username, password);
            this.authToken = auth.authToken();
            this.username = auth.username();
            System.out.printf("Logged in as '%s'. %n", username);
        } catch (IOException | InterruptedException e) {
            System.out.println("Error with server: " + e.getMessage());
        } catch (RuntimeException e) {
            System.out.println(cleanErrorMessage(e.getMessage()));
        }
    }

    // post login

    private void postloginLoop() {
        System.out.printf("[LOGGED IN as %s > ", username);
        String line = scanner.nextLine().trim();

        switch (line.toLowerCase()) {
            case "help", "h" -> printPostloginHelp();
            case "logout" -> handleLogout();
            case "quit", "exit", "q" -> {
                System.out.println("adios!");
                System.exit(0);
            }
            case "create" -> System.out.println("under construction");
            case "list" -> System.out.println("under construction");
            case "play" -> System.out.println("under construction");
            case "observe" -> System.out.println("under construction");
            default -> System.out.println("not recognized, type 'help' for more options");
        }
    }

    private void printPostloginHelp() {
        System.out.println("""
                === Post-Login Commands ===
                help (h) - show help text
                logout   - Log out and return to main menu
                create   - under construction
                list     - under construction
                play     - under construction
                observe  - under construction
                quit (q) - exit chess
                """);
    }

    private void handleLogout() {
        try {
            facade.logout(authToken);
            System.out.printf("logged out  '%s'. %n ", username);
            this.authToken = null;
            this.username = null;
        } catch (IOException | InterruptedException e) {
            System.out.println("Error with server: " + e.getMessage());
        } catch (RuntimeException e) {
            System.out.println(cleanErrorMessage(e.getMessage()));
        }
    }

    private void handleCreateGame() {
        System.out.println("Game name: ");
        String name = scanner.nextLine().trim();
        if (name.isBlank()) {
            System.out.println("you gotta put something in the game name");
            return;
        }

        try {
            GameData game = facade.createGame(authToken, name);
            System.out.printf("Created game '%s' (id = %d).%n", game.gameName(), game.gameID());
        }  catch (IOException | InterruptedException e) {
            System.out.println("Error with creating game: " + e.getMessage());
        } catch (RuntimeException e) {
            System.out.println(cleanErrorMessage(e.getMessage()));
        }
    }

    private void handleListGames() {
        try {
            GameData[] games = facade.listGames(authToken);
            cachedGames.clear();

            if (games == null || games.length == 0) {
                System.out.println("No games found cuh");
                return;
            }

            System.out.println("+++Games+++");
            int i = 1;
            for (GameData g: games) {
                cachedGames.add(g);
                String white = g.whiteUsername() == null ? "-" : g.whiteUsername();
                String black = g.blackUsername() == null ? "-" : g.blackUsername();
                System.out.printf("%d. %s (white: %s, black : %s)%n", i++,
                        g.gameName(),white, black);
            }
        } catch (IOException | InterruptedException e) {
            System.out.println("Error with creating game: " + e.getMessage());
        } catch (RuntimeException e) {
            System.out.println(cleanErrorMessage(e.getMessage()));
        }
    }

    private void handlePlayGame() {
        if (cachedGames.isEmpty()) {
            System.out.println("No games, run 'list' first.");
            return;
        }

        try {
            System.out.println("Game number (from 'list'): ");
            int num = Integer.parseInt(scanner.nextLine().trim());
            if (num < 1 || num > cachedGames.size()) {
                System.out.println("Invalid game number");
                return;
            }
            GameData game = cachedGames.get(num - 1);

            System.out.println("Color (WHITE/BLACK): ");
            String colorStr = scanner.nextLine().trim().toUpperCase();
            ChessGame.TeamColor color;
            if (colorStr.equals("WHITE")) {
                color = ChessGame.TeamColor.WHITE;
            } else if (colorStr.equals("BLACK")) {
                color = ChessGame.TeamColor.BLACK;
            } else {
                System.out.println("Color must be WHITE or BLACK.");
                return;
            }

            facade.joinGame(authToken, color, game.gameID());
            System.out.printf("Joined '%s' as %s. %n", game.gameName(), color);

            ChessGame cg = new ChessGame();
            cg.getBoard().resetBoard();
            showGameBoard(cg, color);
        } catch (NumberFormatException nfe) {
            System.out.println("Game number must be a number");
        } catch (IOException | InterruptedException e ){
            System.out.println("Error joining da game: " + e.getMessage());
        } catch (RuntimeException e) {
            System.out.println(cleanErrorMessage(e.getMessage()));
        }
    }

    private void handleObserveGame() {
        if (cachedGames.isEmpty()) {
            System.out.println("No cached games. Run 'list' first");
            return;
        }

        try {
            System.out.println("Game number (from 'list'): ");
            int num = Integer.parseInt(scanner.nextLine().trim());
            if (num < 1 || num > cachedGames.size()) {
                System.out.println("Invalid game number");
                return;
            }
            GameData game = cachedGames.get(num -1);

            facade.joinGame(authToken, null, game.gameID());
            System.out.printf("Observing '%s' .%n", game.gameName());

            ChessGame cg = new ChessGame();
            cg.getBoard().resetBoard();

            showGameBoard(cg, ChessGame.TeamColor.WHITE);
        } catch (NumberFormatException nfe) {
            System.out.println("Game number must be a number");
        } catch (IOException | InterruptedException e ){
            System.out.println("Error observing da game: " + e.getMessage());
        } catch (RuntimeException e) {
            System.out.println(cleanErrorMessage(e.getMessage()));
        }
    }

    //drawing the game board

    private void showGameBoard(ChessGame game, ChessGame.TeamColor perspective) {
        System.out.print(EscapeSequences.ERASE_SCREEN);
        System.out.printf("Viewing board as %s. Press ENTER to return. %n", perspective);
        drawBoard(game.getBoard(), perspective);
        scanner.nextLine();
        System.out.print(EscapeSequences.ERASE_SCREEN);
    }

    private void drawBoard(ChessBoard board, ChessGame.TeamColor perspective) {
        if (perspective == ChessGame.TeamColor.WHITE) {
            draw
        }
    }

    private void drawWhiteBoard(ChessBoard board){

    }

    private void drawBlackBoard(ChessBoard board){

    }

    private void printSquare(ChessBoard board, int row, int col, boolean lightSquare){

    }

    // helpers
    // keep clean error messages
    private String cleanErrorMessage(String raw) {
        if (raw == null || raw.isBlank()) {
            return "idk what happened just now.";
        }

        raw = raw.trim();
        if (raw.startsWith("{") && raw.contains("\"message\"")) {
            int idx = raw.indexOf("\"message\"");
            int colon = raw.indexOf(':', idx);
            int quoteStart = raw.indexOf('"', colon + 1);
            int quoteEnd = raw.indexOf('"', quoteStart + 1);
            if (quoteStart != -1 && quoteEnd != -1) {
                return raw.substring(quoteStart + 1, quoteEnd);
            }
        }

        return raw;
    }
}
