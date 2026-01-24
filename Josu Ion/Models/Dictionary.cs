using MongoDB.Bson;
using MongoDB.Bson.Serialization.Attributes;
using System;
using System.Collections.Generic;
using System.ComponentModel.DataAnnotations.Schema;

namespace HrWebRecruitment;

public partial class Dictionary
{
    [BsonId]
    [BsonRepresentation(BsonType.String)] // Since Id is a string in JSON
    public ObjectId Id { get; set; }

    [BsonElement("Name")]
    public string Name { get; set; } = null!;

    [BsonElement("Type")]
    public string Type { get; set; } = null!;

    [BsonElement("Description")]
    public string Description { get; set; } = null!;
}
