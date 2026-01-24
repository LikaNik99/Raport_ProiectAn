
using Microsoft.AspNetCore.Mvc;
using PasswordManager.Repositories;
using PasswordManager.Services;
using System.Net.WebSockets;
using System.Security.Claims;
using System.Text;
using System.Text.RegularExpressions;

namespace PasswordManager.Controllers
{
    public class MainController(IWebSocketManager webSocketManager) : Controller
    {
        [HttpGet]
        public IActionResult Index()
        {
            return View("Views/request_client.cshtml");
        }

        public async Task SyncDataWebSocket()
        {
            try
            {
                if (!HttpContext.WebSockets.IsWebSocketRequest)
                    return;

                using var webSocket = await HttpContext.WebSockets.AcceptWebSocketAsync();
                webSocketManager.AddSocket(Guid.NewGuid().ToString(), webSocket);
                await ReceiveLoop(webSocket);

            }
            catch (AggregateException ex)
            {
                webSocketManager.GetWebSockets();
                Console.WriteLine($"Operation canceled: {ex.Message}");
            }
            catch (Exception ex)
            {
                Console.WriteLine(ex.Message + "\n" + ex.StackTrace);
            }
        }
        private async Task<bool> HandleCloseIfNeeded(WebSocket webSocket, string? message)
        {
            if (webSocket.State == WebSocketState.CloseReceived || message == null)
            {
                await webSocket.CloseOutputAsync(WebSocketCloseStatus.NormalClosure, "Closed", CancellationToken.None);
                webSocketManager.GetWebSockets();
                return true;
            }
            return false;
        }

        private async Task SendToWebSockets(string? message)
        {
            if (string.IsNullOrEmpty(message))
                return;
            foreach (var socket in webSocketManager.GetWebSockets())
                await socket.SendAsync(Encoding.UTF8.GetBytes(message), WebSocketMessageType.Text, true, CancellationToken.None);
        }

        private async Task ReceiveLoop(WebSocket webSocket)
        {
            var buffer = new byte[1024 * 4];

            while (webSocket.State is WebSocketState.Open or WebSocketState.CloseSent)
            {
                var message = await ReceiveMessage(webSocket, buffer);
                if (message == null)
                    continue;

                await SendToWebSockets(message);

                if (await HandleCloseIfNeeded(webSocket, message))
                    break;
            }
        }

        private static async Task<string?> ReceiveMessage(WebSocket webSocket, byte[] buffer)
        {
            using var stream = new MemoryStream();
            WebSocketReceiveResult result;

            do
            {
                result = await webSocket.ReceiveAsync(new ArraySegment<byte>(buffer), CancellationToken.None);
                await stream.WriteAsync(buffer, 0, result.Count);
            } while (!result.EndOfMessage);

            if (result.MessageType == WebSocketMessageType.Close)
                return null;

            return Encoding.UTF8.GetString(stream.ToArray());
        }


        [HttpGet("Admin")]
        public IActionResult Admin()
        {
            return View("Views/admin_request.cshtml");
        }

        [HttpGet("Login")]
        public IActionResult Login()
        {
            return View("Views/login_admin.cshtml");
        }

        [HttpGet("Password")]
        public IActionResult PasswordDatabase()
        {
            return View("Views/password_database.cshtml");
        }

        [HttpGet("Request")]
        public IActionResult RequestClient()
        {
            return View("Views/request_client.cshtml");
        }
    }
}
