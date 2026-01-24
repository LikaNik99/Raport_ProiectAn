using MongoDB.Bson;
using MongoDB.Bson.Serialization.Attributes;

// Target Model for MongoDB storage
public class PasswordModel
{
    // MongoDB uses ObjectId for the primary key
    [BsonId]
    [BsonRepresentation(BsonType.ObjectId)]
    public string Id { get; set; }

    // User/Owner Information
    [BsonElement("userId")]
    public string UserId { get; set; }

    [BsonElement("proprietar")]
    public string Proprietar { get; set; }

    // Password Entry Details
    [BsonElement("denumire")] // Corresponds to 'itemName' from frontend
    public string Denumire { get; set; }

    [BsonElement("tip")]
    public string Tip { get; set; } = "Conectare"; // Default value

    [BsonElement("username")]
    public string Username { get; set; }

    [BsonElement("parola")] // WARNING: Ensure this is encrypted before saving!
    public string Parola { get; set; }

    [BsonElement("saitweb")]
    public string SaitWeb { get; set; }

    [BsonElement("detalii")] // Corresponds to 'itemNote' from frontend
    public string Detalii { get; set; }

    // Metadata
    [BsonElement("createdDate")]
    [BsonDateTimeOptions(Kind = DateTimeKind.Utc)]
    public DateTime CreatedDate { get; set; } = DateTime.UtcNow;

    [BsonElement("lastEdited")]
    [BsonDateTimeOptions(Kind = DateTimeKind.Utc)]
    public DateTime LastEdited { get; set; } = DateTime.UtcNow;

    // History/Log (optional for MongoDB, but good practice)
    [BsonElement("istoric")]
    public List<HistoryEntry> Istoric { get; set; } = new List<HistoryEntry>();
}

public class HistoryEntry
{
    [BsonElement("actiune")]
    public string Actiune { get; set; }

    [BsonElement("data")]
    [BsonDateTimeOptions(Kind = DateTimeKind.Utc)]
    public DateTime Data { get; set; } = DateTime.UtcNow;
}