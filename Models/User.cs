using MongoDB.Bson;
using MongoDB.Bson.Serialization.Attributes;
using System;
using System.Collections.Generic;
using System.ComponentModel.DataAnnotations.Schema;

namespace HrWebRecruitment;

public partial class User
{
    [BsonId]
    [BsonElement("_id")] // MongoDB stores the primary key as '_id'
    [BsonRepresentation(BsonType.ObjectId)] // Since Id is a string in JSON
    public ObjectId Id { get; set; }

    [BsonElement("UserName")]
    public string? UserName { get; set; }

    [BsonElement("Password")]
    public string? Password { get; set; }

    [BsonElement("FirstName")]
    public string? FirstName { get; set; }

    [BsonElement("LastName")]
    public string? LastName { get; set; }

    [BsonElement("Email")]
    public string? Email { get; set; }

    [BsonElement("RoleId")]
    public string? RoleId { get; set; } // Link to dictionary item

    [BsonElement("StartDate")]
    public DateTime? StartDate { get; set; }

    [BsonElement("EndDate")]
    public DateTime? EndDate { get; set; }
}
