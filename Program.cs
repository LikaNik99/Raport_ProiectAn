using Microsoft.AspNetCore.WebSockets;
using Microsoft.Extensions.Options;
using MongoDB.Driver;
using PasswordManager.Models.Config;
using PasswordManager.Repositories;
using PasswordManager.Services;

var builder = WebApplication.CreateBuilder(args);

builder.Services.AddOptions<MongoDbConfigModel>()
    .Bind(builder.Configuration.GetSection("MongoDb"))
    .ValidateDataAnnotations()
    .ValidateOnStart();

builder.Services.AddWebSockets(options =>
{
    // Configure WebSocket options here if needed
    options.KeepAliveInterval = TimeSpan.FromMinutes(2);
});
// Add services to the container.

builder.Services.AddControllersWithViews();
builder.Services.AddSingleton<IWebSocketManager, WebSocketManagerService>();
builder.Services.AddSingleton<DbService>();

builder.Services.AddSingleton<IMongoClient>(sp =>
{
    var logger = sp.GetRequiredService<ILogger<Program>>();
    var mongoDbConfig = sp.GetRequiredService<IOptions<MongoDbConfigModel>>().Value;
    logger.LogInformation("MongoDb connection established");
    return new MongoClient(mongoDbConfig.ConnectionString);
});

var app = builder.Build();

// Configure the HTTP request pipeline.

app.UseWebSockets();

app.UseFileServer();

app.UseAuthorization();

app.MapControllers();

app.MapControllerRoute(
            name: "default",
            pattern: "{controller=Main}/{action=Index}");

app.Run();
