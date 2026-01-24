using MongoDB.Bson;
using MongoDB.Bson.Serialization.Attributes;
using System;
using System.Collections.Generic;
using System.ComponentModel.DataAnnotations.Schema;

namespace HrWebRecruitment;

public partial class Log
{
    [BsonId]
    [BsonRepresentation(BsonType.String)] // Since Id is a string in JSON
    public ObjectId LogId { get; set; }

    [BsonElement("Text")]
    public string? Text { get; set; }

    [BsonElement("LogDate")]
    public DateTime? LogDate { get; set; }

    [BsonElement("ErrorMessage")]
    public string? ErrorMessage { get; set; }
}
