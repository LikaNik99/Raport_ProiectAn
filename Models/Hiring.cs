using MongoDB.Bson;
using MongoDB.Bson.Serialization.Attributes;
using Newtonsoft.Json;
using System;
using System.Collections.Generic;
using System.ComponentModel.DataAnnotations.Schema;
using static HrWebRecruitment.Services.UtilsService;

namespace HrWebRecruitment;

public partial class Hiring
{
    [BsonId]
    [BsonRepresentation(BsonType.String)] // Since Id is a string in JSON
    public ObjectId Id { get; set; }

    [BsonElement("Candidat")]
    public string Candidat { get; set; }
    public string CandidatId { get; set; }

    [BsonElement("Employee")]
    public string? Employee { get; set; }

    [BsonElement("Status")]
    public string Status { get; set; }

    [BsonElement("Vacancy")]
    public string? Vacancy { get; set; }

    [BsonElement("StatusDate")]
    [JsonConverter(typeof(BsonDateTimeConverter))] // Use a custom converter
    public DateTime? StatusDate { get; set; }

    [BsonElement("Comm")]
    public string? Comm { get; set; }
}
