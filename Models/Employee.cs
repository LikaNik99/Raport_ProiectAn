using MongoDB.Bson;
using MongoDB.Bson.Serialization.Attributes;
using System;
using System.Collections.Generic;
using System.ComponentModel.DataAnnotations.Schema;

namespace HrWebRecruitment;

public partial class Employee
{
    [BsonId]
    [BsonRepresentation(BsonType.String)] // Since Id is a string in JSON
    public ObjectId Id { get; set; }

    [BsonElement("FirstName")]
    public string FirstName { get; set; } = null!;

    [BsonElement("LastName")]
    public string LastName { get; set; } = null!;

    [BsonElement("PhoneNumber")]
    public string? PhoneNumber { get; set; }

    [BsonElement("Email")]
    public string? Email { get; set; }

    [BsonElement("Department")]
    public string Department { get; set; }

    [BsonElement("Position")]
    public string Position { get; set; }

    [BsonElement("Hiring")]
    public string? Hiring { get; set; }

    [BsonElement("StartDate")]
    public DateTime? StartDate { get; set; }

    [BsonElement("EndDate")]
    public DateTime? EndDate { get; set; }

}
public partial class EmployeeDto
{
    [BsonId]
    public string Id { get; set; }

    [BsonElement("FirstName")]
    public string FirstName { get; set; } = null!;

    [BsonElement("LastName")]
    public string LastName { get; set; } = null!;

    [BsonElement("PhoneNumber")]
    public string? PhoneNumber { get; set; }

    [BsonElement("Email")]
    public string? Email { get; set; }

    [BsonElement("Department")]
    public string Department { get; set; }

    [BsonElement("Position")]
    public string Position { get; set; }

    [BsonElement("Hiring")]
    public string Hiring { get; set; }

    [BsonElement("StartDate")]
    public DateTime? StartDate { get; set; }

    [BsonElement("EndDate")]
    public DateTime? EndDate { get; set; }

}
