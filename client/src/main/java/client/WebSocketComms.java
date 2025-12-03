package client;


import com.google.gson.Gson;
import websocket.commands.UserGameCommand;
import websocket.messages.ServerMessage;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.concurrent.CompletionStage;

public class WebSocketComms implements WebSocket.Listener {

    public interface ServerMessageObserver {
        void notify(ServerMessage message);
    }

    private final Gson gson = new Gson();
    private final ServerMessageObserver observer;
    private WebSocket webSocket;

    public WebSocketComms(String wsUrl, ServerMessageObserver observer) {
        this.observer = observer;
        this.webSocket = HttpClient.newHttpClient()
                .newWebSocketBuilder()
                .buildAsync(URI.create(wsUrl), this)
                .join();
    }

    public void send(UserGameCommand command) {
        String json = gson.toJson(command);
        webSocket.sendText(json, true);
    }

    public void close() {
        if (webSocket != null) {
            webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "bye");
        }
    }

    @Override
    public void onOpen(WebSocket webSocket) {
        this.webSocket = webSocket;
        webSocket.request(1);
    }

    @Override
    public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
        ServerMessage msg = gson.fromJson(data.toString(), ServerMessage.class);
        observer.notify(msg);
        webSocket.request(1);
        return null;
    }

    @Override
    public void onError(WebSocket webSocket, Throwable error) {
        System.out.println("Websocket error: " + error.getMessage());
    }

}
