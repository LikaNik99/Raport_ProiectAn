using HrWebRecruitment.Models.Config;
using HrWebRecruitment.Services;
using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.Options;
using MongoDB.Driver;

var builder = WebApplication.CreateBuilder(args);

builder.Services.AddOptions<MongoDbConfigModel>()
    .Bind(builder.Configuration.GetSection("MongoDb"))
    .ValidateDataAnnotations()
    .ValidateOnStart();
builder.Services.AddOptions<MinioConfigModel>()
    .Bind(builder.Configuration.GetSection("Minio"))
    .ValidateDataAnnotations()
    .ValidateOnStart();


// Add services to the container.
builder.Services.AddControllersWithViews();
builder.Services.AddSingleton<DbService>();
builder.Services.AddSingleton<MinioService>();
builder.Services.AddSingleton<IMongoClient>(sp =>
{
    var logger = sp.GetRequiredService<ILogger<Program>>();
    var mongoDbConfig = sp.GetRequiredService<IOptions<MongoDbConfigModel>>().Value;
    logger.LogInformation("MongoDb connection established");
    return new MongoClient(mongoDbConfig.ConnectionString);
});

var app = builder.Build();
string? _connectionString = app.Configuration.GetConnectionString("DefaultConnection");
// Configure the HTTP request pipeline.

app.UseDefaultFiles();
app.UseStaticFiles();

app.UseStatusCodePages();

app.MapDefaultControllerRoute();
app.MapControllerRoute(
    name: "default",
    pattern: "{controller=Main}/{action=Index}/{id?}");

app.Run();
