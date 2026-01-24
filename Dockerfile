FROM mcr.microsoft.com/dotnet/sdk:9.0 AS build
WORKDIR /src
COPY ["PasswordManager.csproj", "."]
RUN dotnet restore "./PasswordManager.csproj"
COPY . .
WORKDIR "/src/."
RUN dotnet build "PasswordManager.csproj" -c Release -o /app/build

FROM build AS publish
RUN dotnet publish "PasswordManager.csproj" -c Release -o /app/publish /p:UseAppHost=false

FROM mcr.microsoft.com/dotnet/aspnet:9.0
WORKDIR /app
COPY --from=publish /app/publish .

EXPOSE 8080

ENTRYPOINT ["dotnet", "PasswordManager.dll"]
