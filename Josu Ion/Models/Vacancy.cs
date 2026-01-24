using MongoDB.Bson;
using MongoDB.Bson.Serialization.Attributes;

namespace HrWebRecruitment;

public class Vacancy
{
    [BsonId]
    [BsonRepresentation(BsonType.String)] // Since Id is a string in JSON
    public ObjectId Id { get; set; } = new ObjectId();

    [BsonElement("LinkId")]
    public int LinkId { get; set; }

    [BsonElement("Title")]
    public string? Title { get; set; }

    [BsonElement("Description")]
    public string? Description { get; set; }

    [BsonElement("StartDate")]
    public DateTime StartDate { get; set; }

    [BsonElement("EndDate")]
    public DateTime EndDate { get; set; }
}
