package server.handler;
import message.Request;
import message.Response;
import server.network.ClientHandler ;
public interface IActionHandler{
    Response execute(Request request, ClientHandler client);
}
