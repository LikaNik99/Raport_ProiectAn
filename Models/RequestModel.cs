using MongoDB.Bson.Serialization.Attributes;

namespace PasswordManager.Models
{
    public class RequestModel
    {
        [BsonElement("id")]
        public string? Id { get; set; }
        [BsonElement("nume")]
        public string Nume { get; set; } = string.Empty;
        [BsonElement("scop")]
        public string Scop { get; set; } = string.Empty;
        [BsonElement("detalii")]
        public string Detalii { get; set; } = string.Empty;
        [BsonElement("prioritate")]
        public string Prioritate { get; set; } = string.Empty;
        [BsonElement("data")]
        public DateTime Data { get; set; }
        [BsonElement("clientId")]
        public string ClientId { get; set; } = string.Empty;
        [BsonElement("status")]
        public string Status { get; set; } = string.Empty;
    }
}
