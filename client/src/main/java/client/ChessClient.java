package client;

import chess.*;
import model.*;
import ui.EscapeSequences;
import websocket.*;
import websocket.commands.UserGameCommand;
import websocket.messages.ServerMessage;

import java.io.IOException;
import java.util.*;

public class ChessClient implements WebSocketComms.ServerMessageObserver {

    private final ServerFacade facade;
    private final Scanner scanner = new Scanner(System.in);

    private String authToken = null;
    private String username = null;

    private final List<GameData> cachedGames = new ArrayList<>();

    private WebSocketComms ws;
    private ChessGame currentGame;
    private int currentGameId;
    private ChessGame.TeamColor currentPerspective;
    private boolean inGame = false;

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
            case "create" -> handleCreateGame();
            case "list" -> handleListGames();
            case "play" -> handlePlayGame();
            case "observe" -> handleObserveGame();
            case "quit", "exit", "q" -> {
                System.out.println("adios!");
                System.exit(0);
            }
            default -> System.out.println("not recognized, type 'help' for more options");
        }
    }

    private void printPostloginHelp() {
        System.out.println("""
                === Post-Login Commands ===
                help (h) - show help text
                logout   - Log out and return to main menu
                create   - create a new game
                list     - list games on server
                play     - join game
                observe  - observe a game
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
            System.out.printf("Created game '%s'.%n", name, game.gameID());
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
            System.out.println("run 'list' first.");
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

            //ChessGame cg = new ChessGame();
            //cg.getBoard().resetBoard();
            //showGameBoard(cg, color);

            startGameplay(game.gameID(), color, false);

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
            System.out.println("Run 'list' first");
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

            // facade.joinGame(authToken, ChessGame.TeamColor.WHITE, game.gameID());
            System.out.printf("Observing '%s' .%n", game.gameName());

            //ChessGame cg = new ChessGame();
            //cg.getBoard().resetBoard();

            startGameplay(game.gameID(), ChessGame.TeamColor.WHITE, true);
            //showGameBoard(cg, ChessGame.TeamColor.WHITE);
        } catch (NumberFormatException nfe) {
            System.out.println("Game number must be a number");
        } catch (RuntimeException e) {
            System.out.println(cleanErrorMessage(e.getMessage()));
        }
    }

    //drawing the game board

    private void startGameplay(int gameId, ChessGame.TeamColor perspective, boolean observing) {
        this.currentGameId = gameId;
        this.currentPerspective = perspective;
        this.currentGame = null;
        this.inGame = true;

        this.ws = new WebSocketComms(facade.getWsUrl(), this);

        UserGameCommand connect = new UserGameCommand(
                UserGameCommand.CommandType.CONNECT, authToken, gameId);
        ws.send(connect);
        gameLoop(observing);
    }

    private void gameLoop(boolean observing){
        printGameHelp(observing);

        while(inGame) {
            System.out.print("[GAME] > ");
            String cmd = scanner.nextLine().trim().toLowerCase();

            switch (cmd) {
                case "help", "h" -> printGameHelp(observing);
                case "redraw" -> redrawBoard();
                case "highlight" -> handleHighlight();
                case "move" -> {
                    if (observing) {
                        System.out.println("observers cannot make moves.");
                    } else {
                        handleUserMove();
                    }
                }
                case "resign" -> {
                    if (observing) {
                        System.out.println("observers cannot resign.");
                    } else {
                        handleResign();
                    }
                }
                case "leave" -> handleLeave();
                default -> System.out.println("Unknown command. Type 'help'.");
            }
        }

        if (ws != null) {
            ws.close();
        }
        currentGame = null;
    }

    private void printGameHelp(boolean observing) {
        System.out.println("""
                === gameplay commands ===
                help (h)     - show this help text
                redraw       - redraw the chess board
                highlight    - highlight legal moves for a piece
                move         - make a move (players only)
                resign       - resign the game (players only)
                leave        - leave the game and return to menu
                """);
        if (observing) {
            System.out.println("You are observing: you can 'highlight' and 'redraw', but not 'move' or 'resign'.");
        }
    }


    private void handleUserMove() {
        if (currentGame == null) {
            System.out.println("Game not loaded yet");
            return;
        }

        System.out.print("Enter move, for ex. a1 a2");
        String line = scanner.nextLine().trim();
        String[] parts = line.split("\\s+");
        if (parts.length != 2) {
            System.out.println("Please enter exactly two squares like a1 a2");
            return;
        }

        ChessPosition from = parsePosition(parts[0]);
        ChessPosition to = parsePosition(parts[1]);

        if (from == null || to == null) {
            System.out.println("Bad square format. Use a-h + 1-8, a1 a2");
            return;
        }

        ChessMove move = new ChessMove(from, to , null);
        UserGameCommand cmd = new UserGameCommand(
                UserGameCommand.CommandType.MAKE_MOVE, authToken, currentGameId, move);
        ws.send(cmd);
    }

    private void handleResign() {
        System.out.print("type yes to confirm resign: ");
        String confirm = scanner.nextLine().trim().toLowerCase();
        if (!confirm.equals("yes") && !confirm.equals("y")) {
            System.out.println("Resign canceled.");
            return;
        }

        UserGameCommand cmd = new UserGameCommand(
                UserGameCommand.CommandType.RESIGN, authToken, currentGameId);
        ws.send(cmd);
    }

    private void handleLeave() {
        UserGameCommand cmd = new UserGameCommand(
                UserGameCommand.CommandType.LEAVE, authToken, currentGameId);
        ws.send(cmd);
        inGame = false;
    }

    private void handleHighlight() {
        if (currentGame == null) {
            System.out.println("game not loaded yet");
            return;
        }

        System.out.print("Square to highlight moves from : ");
        String sq = scanner.nextLine().trim();
        ChessPosition pos = parsePosition(sq);
        if (pos == null) {
            System.out.println("bad square format, type something like a1");
            return;
        }

        Collection<ChessMove> moves = currentGame.validMoves(pos);
        if (moves == null || moves.isEmpty()) {
            System.out.println("no legal moves for that piece");
            return;
        }

        Set<ChessPosition> highlights = new HashSet<>();
        highlights.add(pos);
        for (ChessMove m : moves) {
            highlights.add(m.getEndPosition());
        }

        System.out.print(EscapeSequences.ERASE_SCREEN);
        drawBoard(currentGame.getBoard(), currentPerspective, highlights);
        System.out.println();
    }

    private void redrawBoard() {
        if (currentGame == null) {
            System.out.print("game not loaded yet");
            return;
        }

        System.out.print(EscapeSequences.ERASE_SCREEN);
        drawBoard(currentGame.getBoard(), currentPerspective, null);
        System.out.println();
    }

    private ChessPosition parsePosition(String s) {
        if (s == null || s.length() != 2) {
            return null;
        }

        char file = Character.toLowerCase(s.charAt(0));
        char rank = s.charAt(1);
        if (file < 'a' || file > 'h') {
            return null;
        }
        if (rank <'1' || rank > '8' ) {
            return null;
        }

        int col = file - 'a' + 1;
        int row = rank - '0';
        return new ChessPosition(row, col);
    }

//    private void showGameBoard(ChessGame game, ChessGame.TeamColor perspective) {
//        System.out.print(EscapeSequences.ERASE_SCREEN);
//        System.out.printf("Press ENTER to return. %n", perspective);
//        drawBoard(game.getBoard(), perspective);
//        scanner.nextLine();
//        System.out.print(EscapeSequences.ERASE_SCREEN);
//    }

    private void drawBoard(ChessBoard board, ChessGame.TeamColor perspective, Set<ChessPosition> highlights) {
        if (perspective == ChessGame.TeamColor.WHITE) {
            drawWhiteBoard(board, highlights);
        } else {
            drawBlackBoard(board, highlights);
        }
        System.out.print(EscapeSequences.RESET_BG_COLOR + EscapeSequences.RESET_TEXT_COLOR + "\n");
    }

    private void drawWhiteBoard(ChessBoard board, Set<ChessPosition> highlights){
        System.out.print("   ");
        for (char file = 'a'; file <= 'h'; file++) {
            System.out.print(" " + file + "  ");
        }
        System.out.println();

        for (int row = 8; row >= 1; row--){
            System.out.print(" " + row + " ");
            for (int col = 1; col <= 8; col++) {
                boolean light = (row + col) % 2 == 0;
                printSquare(board,row,col,light, highlights);
            }
            System.out.print(" " + row);
            System.out.println();
        }

        System.out.print("   ");
        for (char file = 'a'; file <= 'h'; file++) {
            System.out.print(" " + file + "  ");
        }
        System.out.println();
    }

    private void drawBlackBoard(ChessBoard board, Set<ChessPosition> highlights){
        System.out.print("   ");
        for (char file = 'h'; file >= 'a'; file--) {
            System.out.print(" " + file + "  ");
        }
        System.out.println();

        for (int row = 1; row <=8; row++){
            System.out.print(" " + row + " ");
            for (int col = 8; col >= 1; col--) {
                boolean light = (row + col) % 2 == 0;
                printSquare(board,row,col,light, highlights);
            }
            System.out.print(" " + row);
            System.out.println();
        }

        System.out.print("   ");
        for (char file = 'h'; file >= 'a'; file--) {
            System.out.print(" " + file + "  ");
        }
        System.out.println();
    }

    private void printSquare(ChessBoard board, int row, int col, boolean lightSquare,
                             Set<ChessPosition> highlights){

        ChessPosition pos = new ChessPosition(row, col);
        ChessPiece piece = board.getPiece(pos);

        boolean highlighted = highlights != null && highlights.contains(pos);

        String bg;

        if (highlighted) {
            bg = EscapeSequences.SET_BG_COLOR_GREEN;
        } else {
            bg = lightSquare ? EscapeSequences.SET_BG_COLOR_DARK_GREY
                    : EscapeSequences.SET_BG_COLOR_LIGHT_GREY;
        }

        String fg = EscapeSequences.SET_TEXT_COLOR_WHITE;
        String glyph = EscapeSequences.EMPTY;

        if (piece != null) {
            if (piece.getTeamColor() == ChessGame.TeamColor.WHITE) {
                fg = EscapeSequences.SET_TEXT_COLOR_RED;
                glyph = switch (piece.getPieceType()) {
                    case KING -> EscapeSequences.WHITE_KING;
                    case QUEEN -> EscapeSequences.WHITE_QUEEN;
                    case BISHOP -> EscapeSequences.WHITE_BISHOP;
                    case KNIGHT -> EscapeSequences.WHITE_KNIGHT;
                    case ROOK -> EscapeSequences.WHITE_ROOK;
                    case PAWN -> EscapeSequences.WHITE_PAWN;
                };
            } else {
                fg = EscapeSequences.SET_TEXT_COLOR_BLUE;
                glyph = switch (piece.getPieceType()) {
                    case KING -> EscapeSequences.BLACK_KING;
                    case QUEEN -> EscapeSequences.BLACK_QUEEN;
                    case BISHOP -> EscapeSequences.BLACK_BISHOP;
                    case KNIGHT -> EscapeSequences.BLACK_KNIGHT;
                    case ROOK -> EscapeSequences.BLACK_ROOK;
                    case PAWN -> EscapeSequences.BLACK_PAWN;
                };
            }
        }

        System.out.print(bg + fg + glyph + EscapeSequences.RESET_TEXT_COLOR + EscapeSequences.RESET_BG_COLOR);
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

    @Override
    public synchronized void notify(ServerMessage message) {
        switch (message.getServerMessageType()) {
            case LOAD_GAME ->  {
                this.currentGame = message.getGame();
                redrawBoard();
            }
            case NOTIFICATION -> {
                System.out.println();
                System.out.println("NOTIFICATION : " + message.getMessage());
            }
            case ERROR -> {
                System.out.println();
                System.out.println("Server error: " + message.getErrorMessage());
            }
        }
    }
}
