package client;

import model.*;
import java.io.IOException;
import java.util.Scanner;

public class ChessClient {
    private final ServerFacade facade;
    private final Scanner scanner = new Scanner(System.in);

    private String authToken = null;
    private String username = null;

    public ChessClient(ServerFacade facade) {
        this.facade = facade;
    }

    public void run() {
        System.out.println("Welcome to CS240 Chess!");
        System.out.println("Type 'help' to see available commmands.\n");

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
