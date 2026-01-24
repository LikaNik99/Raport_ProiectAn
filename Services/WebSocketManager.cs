using Newtonsoft.Json.Linq;
using PasswordManager.Models;
using System.Collections.Concurrent;
using System.Net.WebSockets;
using System.Text;

namespace PasswordManager.Services
{
    public interface IWebSocketManager
    {
        void AddSocket(string id, WebSocket socket);
        List<WebSocket> GetWebSockets();
        WebSocket GetSocketById(string id);
        List<string> GetWebSocketIds();
        void RemoveSocket(string id);
    }
    public class WebSocketManagerService : IWebSocketManager
    {
        private ConcurrentDictionary<string, WebSocket> _sockets = new();

        public void AddSocket(string id, WebSocket socket)
        {
            _sockets.AddOrUpdate(id, socket, (id, oldSocket) => oldSocket = socket);
        }

        public void RemoveSocket(string id)
        {
            _sockets.TryRemove(id, out _);
        }

        public WebSocket GetSocketById(string id)
        {
            _sockets.TryGetValue(id, out var socket);
            return socket;
        }

        public List<WebSocket> GetWebSockets()
        {
            foreach (var socket in _sockets)
            {
                if (socket.Value.State != WebSocketState.Open)
                {
                    socket.Value.CloseAsync(WebSocketCloseStatus.NormalClosure, "Disconnected", CancellationToken.None);
                    RemoveSocket(socket.Key);
                }
            }
            return _sockets.Values.ToList();
        }

        public List<string> GetWebSocketIds()
        {
            foreach (var socket in _sockets)
            {
                if (socket.Value.State != WebSocketState.Open)
                {
                    socket.Value.CloseAsync(WebSocketCloseStatus.NormalClosure, "Disconnected", CancellationToken.None);
                    RemoveSocket(socket.Key);
                }
            }
            return _sockets.Keys.ToList();
        }

        private async Task<string?> WaitForActiveSession(int loopTime)
        {
            string? activeSessionGuid = GetWebSocketIds().FirstOrDefault();

            while (string.IsNullOrEmpty(activeSessionGuid))
            {
                await Task.Delay(loopTime);
                activeSessionGuid = GetWebSocketIds().FirstOrDefault();
            }

            return activeSessionGuid;
        }

        private async Task SendToWebSockets(IEnumerable<WebSocket> webSockets, JObject broadcastResponse)
        {
            byte[] messageBytes = Encoding.UTF8.GetBytes(broadcastResponse.ToString());
            foreach (var socket in webSockets)
            {
                try
                {
                    await socket.SendAsync(messageBytes, WebSocketMessageType.Text, true, CancellationToken.None);
                }
                catch (Exception sendEx)
                {

                }
            }
        }
    }
}
