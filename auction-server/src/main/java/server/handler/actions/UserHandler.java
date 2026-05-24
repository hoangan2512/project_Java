package server.handler.actions;

import java.util.logging.Level;
import java.util.logging.Logger;

import message.Request;
import message.Response;
import model.User;
import server.controller.UserController;
import server.handler.IActionHandler;
import server.network.ClientHandler;

public class UserHandler implements IActionHandler {
    private final UserController userController = new UserController();
    private static final Logger LOGGER = Logger.getLogger(UserHandler.class.getName());

    @Override
    public Response execute(Request request, ClientHandler client) {
        switch (request.getAction()) {
            case LOGIN_SELLER:
            case LOGIN_BIDDER:
            case LOGIN_ADMIN:
                return handleLogin(request, client);
            case REGISTER:
                return handleRegister(request, client);
            case LOGOUT:
                return handleLogout(client);
            case GOOGLE_LOGIN:
                return handleGoogleLogin(request, client);
            default:
                LOGGER.warning("Unknown or invalid action attempted: " + request.getAction());
                return new Response("ERROR", null, "Invalid user action.");
        }
    }

    private Response handleLogin(Request request, ClientHandler client) {
        Response response = userController.handleLogin(request);
        if ("SUCCESS".equals(response.getStatus()) && response.getData() instanceof User) {
            client.setLoggedInUser((User) response.getData());
            LOGGER.info("Session successfully recorded for user: " + client.getLoggedInUser().getName());
        }
        return response;
    }

    private Response handleRegister(Request request, ClientHandler client) {
        Response response = userController.handleRegister(request);
        if ("SUCCESS".equals(response.getStatus()) && response.getData() instanceof User) {
            client.setLoggedInUser((User) response.getData());
            LOGGER.info("Session automatically recorded after registration for user: " + client.getLoggedInUser().getName());
        }
        return response;
    }

    private Response handleLogout(ClientHandler client) {
        if (client.getLoggedInUser() != null) {
            LOGGER.info("Session terminated (Logout) for user: " + client.getLoggedInUser().getName());
        }
        client.clearSession();
        return new Response("SUCCESS", null, "Logged out successfully.");
    }

    private Response handleGoogleLogin(Request request, ClientHandler client) {
        String codeReceived = (String) request.getPayload();
        String clientId = "887547914295-i912u5c51mm9ur6s7kd1pr5kipmpcgka.apps.googleusercontent.com";
        String clientSecret = "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";
        String redirectUri = "http://localhost:8080";

        try {
            com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse tokenResponse =
                    new com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeTokenRequest(
                            new com.google.api.client.http.javanet.NetHttpTransport(),
                            com.google.api.client.json.gson.GsonFactory.getDefaultInstance(),
                            "https://oauth2.googleapis.com/token",
                            clientId,
                            clientSecret,
                            codeReceived,
                            redirectUri
                    ).execute();

            com.google.api.client.googleapis.auth.oauth2.GoogleIdToken idToken = tokenResponse.parseIdToken();
            com.google.api.client.googleapis.auth.oauth2.GoogleIdToken.Payload payload = idToken.getPayload();

            String email = payload.getEmail();
            String name = (String) payload.get("name");

            LOGGER.info("[SERVER] User authenticated via Google OAuth: " + name + " (" + email + ")");
            Response authResponse = userController.handleGoogleLoginAuth(email, name);

            if ("SUCCESS".equals(authResponse.getStatus()) && authResponse.getData() instanceof User) {
                client.setLoggedInUser((User) authResponse.getData());
                LOGGER.info("=> Session recorded via Google for user: " + client.getLoggedInUser().getName());
            }

            return authResponse;

        } catch (com.google.api.client.auth.oauth2.TokenResponseException e) {
            StringBuilder errorLog = new StringBuilder("Google OAuth API returned configuration error:\n");
            if (e.getDetails() != null) {
                errorLog.append("  - Error: ").append(e.getDetails().getError()).append("\n")
                        .append("  - Description: ").append(e.getDetails().getErrorDescription());
            } else {
                errorLog.append("  - Raw Content: ").append(e.getContent());
            }
            LOGGER.log(Level.SEVERE, errorLog.toString(), e);

            String errorMsg = (e.getDetails() != null) ? e.getDetails().getErrorDescription() : e.getMessage();
            return new Response("FAILED", null, "Google refused to grant Token: " + errorMsg);

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Google OAuth verification error at Server", e);
            return new Response("FAILED", null, "Server connection error: " + e.getMessage());
        }
    }
}
